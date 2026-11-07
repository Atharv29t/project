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
            document.put("questions", questions);
            store.transact(transaction -> {
                Map<String, Object> existing = transaction.find("quizzes", quiz.getId());
                if (existing == null) {
                    transaction.create("quizzes", quiz.getId(), document);
                } else {
                    List<Map<String, Object>> mergedQuestions = mergeQuestions(existing.get("questions"), questions);
                    Map<String, Object> mergedQuiz = new HashMap<>(existing);
                    mergedQuiz.remove("_id");
                    mergedQuiz.putAll(document);
                    mergedQuiz.put("questions", mergedQuestions);
                    transaction.set("quizzes", quiz.getId(), mergedQuiz);
                }
                return null;
            });
        }
    }

    private List<Map<String, Object>> mergeQuestions(Object storedValue, List<Map<String, Object>> currentQuestions) {
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
        java.util.Set<Object> existingIds = merged.stream().map(question -> question.get("id"))
                .collect(java.util.stream.Collectors.toSet());
        for (Map<String, Object> question : currentQuestions) {
            if (existingIds.add(question.get("id"))) {
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
