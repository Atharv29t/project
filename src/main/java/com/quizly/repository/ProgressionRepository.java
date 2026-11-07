package com.quizly.repository;

import com.quizly.db.Database;
import com.quizly.db.DocumentStore;
import com.quizly.model.Progression;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores account XP, daily streaks and earned badges in Firestore.
 */
public final class ProgressionRepository {

    private static final String PROGRESS = "playerProgress";
    private static final String AWARDS = "progressAwards";
    private final DocumentStore store;

    public ProgressionRepository(Database database) {
        this.store = database.store();
    }

    public Progression recordQuizCompletion(String userId, String attemptId,
                                             int correctAnswers, boolean doublePointsEarned) {
        return store.transact(transaction -> {
            Map<String, Object> priorAward = transaction.find(AWARDS, attemptId);
            Map<String, Object> stored = transaction.find(PROGRESS, userId);
            if (priorAward != null) {
                return stored == null
                        ? progression(0, 0, 0, 0, List.of())
                        : mapProgression(stored);
            }

            int previousXp = stored == null ? 0 : number(stored.get("xp"), 0);
            int previousStreak = stored == null ? 0 : number(stored.get("currentStreak"), 0);
            int previousAttempts = stored == null ? 0 : number(stored.get("completedAttempts"), 0);
            String lastPlayed = stored == null ? null : (String) stored.get("lastPlayedDate");
            LocalDate today = LocalDate.now(ZoneOffset.UTC);

            int streak = today.toString().equals(lastPlayed)
                    ? previousStreak
                    : today.minusDays(1).toString().equals(lastPlayed) ? previousStreak + 1 : 1;
            int award = 15 + Math.max(0, correctAnswers) * 10 + (doublePointsEarned ? 10 : 0)
                    + Math.min(streak, 7) * 2;
            int updatedXp = previousXp + award;
            int attempts = previousAttempts + 1;

            List<String> badges = stored == null
                    ? new ArrayList<>()
                    : new ArrayList<>(strings(stored.get("badges")));
            addBadge(badges, attempts >= 1, "first-quiz");
            addBadge(badges, streak >= 3, "three-day-streak");
            addBadge(badges, streak >= 7, "week-streak");
            addBadge(badges, updatedXp >= 100, "century-xp");
            addBadge(badges, updatedXp >= 500, "five-hundred-xp");

            Map<String, Object> updated = new HashMap<>();
            updated.put("xp", updatedXp);
            updated.put("currentStreak", streak);
            updated.put("lastPlayedDate", today.toString());
            updated.put("completedAttempts", attempts);
            updated.put("lastAward", award);
            updated.put("badges", badges);
            updated.put("updatedAt", new Date());
            transaction.set(PROGRESS, userId, updated);
            transaction.create(AWARDS, attemptId, Map.of("userId", userId, "xp", award));
            return progression(updatedXp, streak, attempts, award, badges);
        });
    }

    public Progression findByUserId(String userId) {
        Map<String, Object> stored = store.find(PROGRESS, userId);
        return stored == null ? progression(0, 0, 0, 0, List.of()) : mapProgression(stored);
    }

    private Progression mapProgression(Map<String, Object> stored) {
        String lastPlayed = (String) stored.get("lastPlayedDate");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int currentStreak = lastPlayed != null
                && (lastPlayed.equals(today.toString()) || lastPlayed.equals(today.minusDays(1).toString()))
                ? number(stored.get("currentStreak"), 0) : 0;
        return progression(
                number(stored.get("xp"), 0),
                currentStreak,
                number(stored.get("completedAttempts"), 0),
                number(stored.get("lastAward"), 0),
                strings(stored.get("badges"))
        );
    }

    private Progression progression(int xp, int streak, int attempts, int lastAward, List<String> badges) {
        int level = xp / 100 + 1;
        int xpIntoLevel = xp % 100;
        return new Progression(xp, level, xpIntoLevel, 100 - xpIntoLevel,
                streak, attempts, lastAward, badges);
    }

    private void addBadge(List<String> badges, boolean earned, String badge) {
        if (earned && !badges.contains(badge)) {
            badges.add(badge);
        }
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String.class::cast).toList();
    }
}
