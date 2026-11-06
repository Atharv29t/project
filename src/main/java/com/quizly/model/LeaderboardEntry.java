package com.quizly.model;

import java.util.Objects;

public final class LeaderboardEntry {

    private final int rank;
    private final Result result;

    public LeaderboardEntry(int rank, Result result) {
        this.rank = rank;
        this.result = Objects.requireNonNull(result, "result");
    }

    public int getRank() {
        return rank;
    }

    public Result getResult() {
        return result;
    }

    public String getRowClass() {
        return rank <= 3 ? "podium podium-" + rank : "";
    }

    public String getMedal() {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "";
        };
    }
}
