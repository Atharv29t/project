package com.quizly.repository;

import com.quizly.TestDatabase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRepositoryTest {

    @Test
    void awardsXpBadgesAndDoesNotCountTheSameAttemptTwice() {
        var database = TestDatabase.create("progress-" + UUID.randomUUID());
        ProgressionRepository repository = new ProgressionRepository(database);

        var first = repository.recordQuizCompletion("user-1", "attempt-1", 6, false);
        var duplicate = repository.recordQuizCompletion("user-1", "attempt-1", 6, false);
        var second = repository.recordQuizCompletion("user-1", "attempt-2", 6, true);

        assertEquals(1, first.getLevel());
        assertEquals(first.getXp(), duplicate.getXp());
        assertEquals(1, first.getCompletedAttempts());
        assertEquals(87, second.getXp() - first.getXp());
        assertTrue(first.getBadges().contains("first-quiz"));
        assertTrue(second.getBadges().contains("century-xp"));
    }

    @Test
    void sameDayCompletionsDoNotIncreaseTheDailyStreak() {
        var database = TestDatabase.create("progress-" + UUID.randomUUID());
        ProgressionRepository repository = new ProgressionRepository(database);

        var first = repository.recordQuizCompletion("user-1", "attempt-1", 0, false);
        var second = repository.recordQuizCompletion("user-1", "attempt-2", 0, false);

        assertEquals(1, first.getCurrentStreak());
        assertEquals(1, second.getCurrentStreak());
        assertEquals(2, second.getCompletedAttempts());
    }

    @Test
    void expiredStreakIsNotDisplayedAsCurrent() {
        var database = TestDatabase.create("progress-" + UUID.randomUUID());
        Map<String, Object> expiredProgress = new HashMap<>();
        expiredProgress.put("xp", 450);
        expiredProgress.put("currentStreak", 5);
        expiredProgress.put("lastPlayedDate", LocalDate.now(ZoneOffset.UTC).minusDays(2).toString());
        expiredProgress.put("completedAttempts", 8);
        expiredProgress.put("lastAward", 20);
        expiredProgress.put("badges", List.of());
        database.store().set("playerProgress", "user-1", expiredProgress);

        var progression = new ProgressionRepository(database).findByUserId("user-1");

        assertEquals(0, progression.getCurrentStreak());
    }
}
