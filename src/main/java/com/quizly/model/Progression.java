package com.quizly.model;

import java.util.List;

public final class Progression {

    private final int xp;
    private final int level;
    private final int xpIntoLevel;
    private final int xpToNextLevel;
    private final int currentStreak;
    private final int completedAttempts;
    private final int lastAward;
    private final List<String> badges;

    public Progression(int xp, int level, int xpIntoLevel, int xpToNextLevel,
                       int currentStreak, int completedAttempts, int lastAward, List<String> badges) {
        this.xp = xp;
        this.level = level;
        this.xpIntoLevel = xpIntoLevel;
        this.xpToNextLevel = xpToNextLevel;
        this.currentStreak = currentStreak;
        this.completedAttempts = completedAttempts;
        this.lastAward = lastAward;
        this.badges = List.copyOf(badges);
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int getXpIntoLevel() {
        return xpIntoLevel;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getCompletedAttempts() {
        return completedAttempts;
    }

    public int getLastAward() {
        return lastAward;
    }

    public List<String> getBadges() {
        return badges;
    }
}
