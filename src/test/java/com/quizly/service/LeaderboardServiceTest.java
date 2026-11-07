package com.quizly.service;

import com.quizly.TestDatabase;
import com.quizly.model.AnswerReview;
import com.quizly.model.Result;
import com.quizly.repository.ResultRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardServiceTest {

    @Test
    void returnsTopThreeSortedByPercentage() {
        ResultRepository repository = repository();
        repository.save(sample("r1", "Ada", "java", 100, Instant.parse("2026-03-01T10:00:00Z")));
        repository.save(sample("r2", "Grace", "java", 80, Instant.parse("2026-03-01T08:00:00Z")));
        repository.save(sample("r3", "Alan", "dsa", 90, Instant.parse("2026-03-02T12:00:00Z")));

        var leaderboard = new LeaderboardService(repository);
        List<Integer> percentages = leaderboard.topEntries(null, 3).stream()
                .map(entry -> entry.getResult().getPercentage())
                .toList();

        assertEquals(100, percentages.get(0));
        assertEquals(90, percentages.get(1));
        assertEquals(80, percentages.get(2));
    }

    @Test
    void filtersTopScoresBySelectedQuiz() {
        ResultRepository repository = repository();
        repository.save(sample("java-high", "Ada", "java", 100, Instant.now()));
        repository.save(sample("dsa-high", "Grace", "dsa", 90, Instant.now()));
        repository.save(sample("dsa-low", "Alan", "dsa", 60, Instant.now()));

        var leaderboard = new LeaderboardService(repository);
        List<String> players = leaderboard.topEntries("dsa", 2).stream()
                .map(entry -> entry.getResult().getPlayerName())
                .toList();

        assertEquals(List.of("Grace", "Alan"), players);
    }

    @Test
    void resolvedTiesFavourTheEarlierCompletion() {
        ResultRepository repository = repository();
        repository.save(sample("older", "Ada", "java", 80, Instant.parse("2026-03-01T10:00:00Z")));
        repository.save(sample("newer", "Grace", "java", 80, Instant.parse("2026-03-02T10:00:00Z")));

        var leaderboard = new LeaderboardService(repository);
        String firstPlayer = leaderboard.topEntries(null, 2).get(0).getResult().getPlayerName();

        assertEquals("Ada", firstPlayer);
    }

    @Test
    void rankOfKnownResultIsProvided() {
        ResultRepository repository = repository();
        Result target = sample("target", "Ada", "java", 100, Instant.now());
        repository.save(target);
        repository.save(sample("lower", "Grace", "java", 60, Instant.now()));

        var leaderboard = new LeaderboardService(repository);
        assertEquals(1, leaderboard.rankOf("target", "java"));
    }

    @Test
    void statsAreAggregatedAcrossQualifyingResults() {
        ResultRepository repository = repository();
        repository.save(sample("r1", "Ada", "java", 100, Instant.now()));
        repository.save(sample("r2", "Grace", "java", 50, Instant.now()));
        repository.save(sample("r3", "Alan", "dsa", 80, Instant.now()));

        var leaderboard = new LeaderboardService(repository);
        var allStats = leaderboard.stats(null);
        assertEquals(3, allStats.getAttempts());
        assertEquals(77, allStats.getAveragePercentage());
        assertEquals(100, allStats.getBestPercentage());

        var dsaStats = leaderboard.stats("dsa");
        assertEquals(1, dsaStats.getAttempts());
        assertEquals(80, dsaStats.getAveragePercentage());

        var emptyStats = leaderboard.stats("web");
        assertEquals(0, emptyStats.getAttempts());
        assertTrue(emptyStats.getBestPercentage() == 0);
    }

    private ResultRepository repository() {
        var database = TestDatabase.create("board-" + UUID.randomUUID());
        database.initialize();
        return new ResultRepository(database);
    }

    private Result sample(String id, String playerName, String quizId, int percentage, Instant completedAt) {
        return new Result(
                id,
                playerName,
                quizId,
                quizId.equals("dsa") ? "DSA Essentials" : "Java Fundamentals",
                quizId.equals("dsa") ? "🧩" : "☕",
                "#6C5CE7",
                percentage / 10,
                10,
                percentage,
                completedAt,
                List.of(new AnswerReview(1, "Sample prompt", "Sample answer", "Correct answer", "Explanation"))
        );
    }
}
