package com.quizly.model;

import java.time.Instant;

public final class RoomPlayerSnapshot {

    private final String userId;
    private final String name;
    private final int answered;
    private final int total;
    private final boolean finished;
    private final int points;
    private final int percentage;
    private final Instant completedAt;

    public RoomPlayerSnapshot(String userId, String name, int answered, int total,
                              boolean finished, int points, int percentage, Instant completedAt) {
        this.userId = userId;
        this.name = name;
        this.answered = answered;
        this.total = total;
        this.finished = finished;
        this.points = points;
        this.percentage = percentage;
        this.completedAt = completedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public int getAnswered() {
        return answered;
    }

    public int getTotal() {
        return total;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getPercentage() {
        return percentage;
    }

    public int getPoints() {
        return points;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
