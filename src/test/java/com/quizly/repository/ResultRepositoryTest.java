package com.quizly.repository;

import com.quizly.TestDatabase;
import com.quizly.model.AnswerReview;
import com.quizly.model.Result;
import com.quizly.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultRepositoryTest {

    @Test
    void savesLoadsAndSurvivesARepositoryRestart() {
        String name = "result-" + UUID.randomUUID();
        var database = TestDatabase.create(name);
        database.initialize();
        Instant completedAt = Instant.parse("2026-03-01T10:15:30Z");
        Result result = result("r-1", "Ada, Lovelace", "java", 6, 6, 100, completedAt);

        ResultRepository repository = new ResultRepository(database);
        repository.save(result);
        assertEquals(1, repository.count());

        var reopened = TestDatabase.create(name);
        ResultRepository reloaded = new ResultRepository(reopened);
        assertEquals(1, reloaded.count());
        Result loaded = reloaded.findById("r-1").orElseThrow();
        assertEquals("Ada, Lovelace", loaded.getPlayerName());
        assertEquals("java", loaded.getQuizId());
        assertEquals(100, loaded.getPercentage());
        assertEquals(completedAt, loaded.getCompletedAt());
        assertEquals(1, loaded.getReviews().size());
        assertEquals("Correct", loaded.getReviews().get(0).getCorrectAnswer());
    }

    @Test
    void findsResultsByUserAndQuiz() {
        var database = TestDatabase.create("result-" + UUID.randomUUID());
        database.initialize();
        UserRepository users = new UserRepository(database);
        User user = new User("u-1", "Ada", "ada@example.com", "hash", Instant.now(), Instant.now());
        users.create(user);

        ResultRepository repository = new ResultRepository(database);
        repository.save(result("r-1", user.getId(), "Ada", "java", 4, 6, 67, Instant.now()));
        repository.save(result("r-2", user.getId(), "Ada", "dsa", 3, 6, 50, Instant.now()));

        assertEquals(2, repository.findByUser("u-1").size());
        assertEquals(1, repository.findByQuiz("dsa").size());
        assertEquals("dsa", repository.findById("r-2").orElseThrow().getQuizId());
    }

    @Test
    void persistsTheBonusPointAwardedByDoublePoints() {
        var database = TestDatabase.create("result-" + UUID.randomUUID());
        database.initialize();
        Result result = new Result("bonus", null, "Ada", "java", "Java Fundamentals",
                "☕", "#6C5CE7", 1, 1, 2, 50, Instant.now(),
                List.of(new AnswerReview(1, "Prompt", "Correct", "Correct", "Explanation")));

        ResultRepository repository = new ResultRepository(database);
        repository.save(result);

        Result loaded = repository.findById("bonus").orElseThrow();
        assertEquals(1, loaded.getBonusPoints());
        assertEquals(2, loaded.getPoints());
        assertEquals(50, loaded.getPercentage());
    }

    @Test
    void rejectsSavingAReviewlessResult() {
        var database = TestDatabase.create("result-" + UUID.randomUUID());
        database.initialize();
        Result result = new Result(
                "r-empty",
                "Ada",
                "java",
                "Java Fundamentals",
                "☕",
                "#6C5CE7",
                1,
                6,
                17,
                Instant.now(),
                List.of()
        );

        var repository = new ResultRepository(database);
        assertThrows(IllegalArgumentException.class, () -> repository.save(result));
        assertTrue(repository.findAll().isEmpty());
    }

    private Result result(String id, String playerName, String quizId,
                          int score, int total, int percentage, Instant completedAt) {
        return result(id, null, playerName, quizId, score, total, percentage, completedAt);
    }

    private Result result(String id, String userId, String playerName, String quizId,
                          int score, int total, int percentage, Instant completedAt) {
        return new Result(
                id,
                userId,
                playerName,
                quizId,
                quizId.equals("dsa") ? "DSA Essentials" : "Java Fundamentals",
                quizId.equals("dsa") ? "🧩" : "☕",
                "#6C5CE7",
                score,
                total,
                percentage,
                completedAt,
                List.of(new AnswerReview(1, "Prompt", playerName, "Correct", "Explanation"))
        );
    }
}
