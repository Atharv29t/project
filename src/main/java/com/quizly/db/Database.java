package com.quizly.db;

import com.quizly.data.QuizCatalog;
import com.quizly.model.Question;
import com.quizly.model.Quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Database implements AutoCloseable {

    private final DocumentStore store;

    public Database(DocumentStore store) {
        this.store = store;
    }

    public static Database appDatabase() {
        String projectId = setting("quizly.firebase.projectId", "QUIZLY_FIREBASE_PROJECT_ID");
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "Set QUIZLY_FIREBASE_PROJECT_ID to your Firebase project ID before starting Quizly.");
        }
        return new Database(FirestoreDocumentStore.connect(projectId));
    }

    public DocumentStore store() {
        return store;
    }

    public void initialize() {
        try {
            seedQuizzes();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not initialize the Firebase Firestore database", exception);
        }
    }

    private void seedQuizzes() {
        List<Quiz> catalogue = QuizCatalog.all();
        for (int quizIndex = 0; quizIndex < catalogue.size(); quizIndex++) {
            Quiz quiz = catalogue.get(quizIndex);
            List<Map<String, Object>> questions = new ArrayList<>();
            for (Question question : quiz.getQuestions()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", question.getId());
                item.put("prompt", question.getPrompt());
                item.put("options", question.getOptions());
                item.put("correctOptionIndex", question.getCorrectOptionIndex());
                item.put("difficulty", question.getDifficulty());
                item.put("explanation", question.getExplanation());
                item.put("status", "published");
                questions.add(item);
            }
            Map<String, Object> document = new HashMap<>();
            document.put("title", quiz.getTitle());
            document.put("tagline", quiz.getTagline());
            document.put("description", quiz.getDescription());
            document.put("icon", quiz.getIcon());
            document.put("difficulty", quiz.getLevel());
            document.put("estimatedMinutes", quiz.getEstimatedMinutes());
            document.put("accentColor", quiz.getAccentColor());
            document.put("sortOrder", quizIndex);
            document.put("status", "published");
            document.put("questions", questions);
            document.put("contentManaged", true);
            store.transact(transaction -> {
                Map<String, Object> existing = transaction.find("quizzes", quiz.getId());
                if (existing == null) {
                    document.put("createdAt", java.util.Date.from(java.time.Instant.now()));
                    transaction.create("quizzes", quiz.getId(), document);
                } else if (!Boolean.TRUE.equals(existing.get("contentManaged"))) {
                    List<Map<String, Object>> mergedQuestions = mergeLegacyQuestions(existing.get("questions"), questions);
                    Map<String, Object> migrated = new HashMap<>(existing);
                    migrated.remove("_id");
                    migrated.putAll(document);
                    migrated.put("questions", mergedQuestions);
                    migrated.put("contentManaged", true);
                    transaction.set("quizzes", quiz.getId(), migrated);
                }
                return null;
            });
        }
    }

    /** One-time compatibility merge for quiz documents written before admin editing existed. */
    private List<Map<String, Object>> mergeLegacyQuestions(Object storedValue,
                                                            List<Map<String, Object>> catalogueQuestions) {
        List<Map<String, Object>> merged = new ArrayList<>();
        if (storedValue instanceof List<?> storedQuestions) {
            for (Object value : storedQuestions) {
                if (value instanceof Map<?, ?> storedQuestion) {
                    Map<String, Object> question = new HashMap<>();
                    storedQuestion.forEach((key, item) -> question.put((String) key, item));
                    merged.add(question);
                }
            }
        }
        java.util.Set<Object> ids = merged.stream().map(question -> question.get("id"))
                .collect(java.util.stream.Collectors.toSet());
        for (Map<String, Object> question : catalogueQuestions) {
            if (ids.add(question.get("id"))) {
                merged.add(question);
            }
        }
        return merged;
    }

    private static String setting(String propertyName, String environmentName) {
        String value = System.getProperty(propertyName);
        return value == null || value.isBlank() ? System.getenv(environmentName) : value;
    }

    @Override
    public void close() {
        store.close();
    }
}
