package com.quizly.repository;

import com.quizly.db.Database;
import com.quizly.db.DocumentStore;
import com.quizly.model.AnswerReview;
import com.quizly.model.Result;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Firestore-backed completed attempts and their answer reviews.
 */
public final class ResultRepository {

    private static final String RESULTS = "results";
    private final DocumentStore store;

    public ResultRepository(Database database) {
        this.store = database.store();
    }

    public void save(Result result) {
        if (result.getReviews().isEmpty()) {
            throw new IllegalArgumentException("A stored result must contain reviews for validation");
        }
        List<Map<String, Object>> reviews = result.getReviews().stream().map(review -> {
            Map<String, Object> document = new HashMap<>();
            document.put("number", review.getNumber());
            document.put("prompt", review.getPrompt());
            document.put("selectedAnswer", review.getSelectedAnswer());
            document.put("correctAnswer", review.getCorrectAnswer());
            document.put("explanation", review.getExplanation());
            return document;
        }).toList();
        Map<String, Object> document = new HashMap<>();
        document.put("userId", result.getUserId());
        document.put("playerName", result.getPlayerName());
        document.put("quizId", result.getQuizId());
        document.put("quizTitle", result.getQuizTitle());
        document.put("quizIcon", result.getQuizIcon());
        document.put("accentColor", result.getAccentColor());
        document.put("score", result.getScore());
        document.put("bonusPoints", result.getBonusPoints());
        document.put("total", result.getTotal());
        document.put("percentage", result.getPercentage());
        document.put("completedAt", Date.from(result.getCompletedAt()));
        document.put("reviews", reviews);
        store.create(RESULTS, result.getId(), document);
    }

    public List<Result> findAll() {
        return queryResults(null, null);
    }

    public List<Result> findByQuiz(String quizId) {
        return queryResults("quizId", quizId);
    }

    public List<Result> findByUser(String userId) {
        return queryResults("userId", userId);
    }

    public Optional<Result> findById(String id) {
        Map<String, Object> document = store.find(RESULTS, id);
        return document == null ? Optional.empty() : Optional.of(mapResult(document));
    }

    public long count() {
        return store.findAll(RESULTS).size();
    }

    private List<Result> queryResults(String field, String value) {
        return store.findAll(RESULTS).stream()
                .filter(document -> field == null || value.equals(document.get(field)))
                .map(this::mapResult)
                .sorted((left, right) -> right.getCompletedAt().compareTo(left.getCompletedAt()))
                .toList();
    }

    private Result mapResult(Map<String, Object> document) {
        List<?> storedReviews = (List<?>) document.get("reviews");
        List<AnswerReview> reviews = new ArrayList<>();
        for (Object value : storedReviews) {
            Map<?, ?> review = (Map<?, ?>) value;
            reviews.add(new AnswerReview(
                    number(review.get("number"), 0),
                    (String) review.get("prompt"),
                    (String) review.get("selectedAnswer"),
                    (String) review.get("correctAnswer"),
                    (String) review.get("explanation")
            ));
        }
        return new Result(
                (String) document.get("_id"),
                (String) document.get("userId"),
                (String) document.get("playerName"),
                (String) document.get("quizId"),
                (String) document.get("quizTitle"),
                (String) document.get("quizIcon"),
                (String) document.get("accentColor"),
                number(document.get("score"), 0),
                number(document.get("bonusPoints"), 0),
                number(document.get("total"), 0),
                number(document.get("percentage"), 0),
                instant(document.get("completedAt")),
                reviews
        );
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private Instant instant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof com.google.cloud.Timestamp timestamp) {
            return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("Unexpected Firestore timestamp value");
    }
}
