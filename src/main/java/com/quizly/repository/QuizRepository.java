package com.quizly.repository;

import com.quizly.data.QuizCatalog;
import com.quizly.db.Database;
import com.quizly.db.DocumentStore;
import com.quizly.model.Question;
import com.quizly.model.Quiz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads and caches the Firestore quiz catalogue for this application instance.
 */
public final class QuizRepository {

    private final DocumentStore store;
    private volatile List<Quiz> cachedQuizzes;

    public QuizRepository(Database database) {
        this.store = database.store();
    }

    public List<Quiz> findAll() {
        List<Quiz> quizzes = cachedQuizzes;
        if (quizzes != null) {
            return quizzes;
        }
        synchronized (this) {
            if (cachedQuizzes == null) {
                cachedQuizzes = store.findAll("quizzes").stream()
                        .sorted(Comparator.comparingInt((Map<String, Object> document) -> number(document.get("sortOrder"), 0))
                                .thenComparing(document -> (String) document.getOrDefault("title", "")))
                        .map(this::mapQuiz)
                        .toList();
            }
            return cachedQuizzes;
        }
    }

    public Optional<Quiz> findById(String quizId) {
        if (quizId == null || quizId.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(quiz -> quiz.getId().equals(quizId))
                .findFirst();
    }

    public int totalQuestions() {
        return findAll().stream().mapToInt(quiz -> quiz.getQuestions().size()).sum();
    }

    private Quiz mapQuiz(Map<String, Object> document) {
        String quizId = (String) document.get("_id");
        List<?> questionDocuments = (List<?>) document.get("questions");
        List<Question> questions = new ArrayList<>();
        for (int index = 0; index < questionDocuments.size(); index++) {
            Map<?, ?> question = (Map<?, ?>) questionDocuments.get(index);
            questions.add(new Question(
                    (String) question.get("id"),
                    (String) question.get("prompt"),
                    strings(question.get("options")),
                    number(question.get("correctOptionIndex"), 0),
                    (String) question.get("explanation"),
                    number(question.get("difficulty"),
                            QuizCatalog.difficultyForQuestion(quizId, (String) question.get("id"), index))
            ));
        }
        return new Quiz(
                quizId,
                (String) document.get("title"),
                (String) document.get("tagline"),
                (String) document.get("description"),
                (String) document.get("icon"),
                (String) document.get("difficulty"),
                number(document.get("estimatedMinutes"), 0),
                (String) document.get("accentColor"),
                questions
        );
    }

    private List<String> strings(Object value) {
        return ((List<?>) value).stream().map(String.class::cast).toList();
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
