package com.quizly.model;

/**
 * Aggregate numbers for the leaderboard header cards.
 */
public final class LeaderboardStats {

    private final long attempts;
    private final int averagePercentage;
    private final int bestPercentage;

    public LeaderboardStats(long attempts, int averagePercentage, int bestPercentage) {
        this.attempts = attempts;
        this.averagePercentage = averagePercentage;
        this.bestPercentage = bestPercentage;
    }

    public long getAttempts() {
        return attempts;
    }

    public int getAveragePercentage() {
        return averagePercentage;
    }

    public int getBestPercentage() {
        return bestPercentage;
    }
}
