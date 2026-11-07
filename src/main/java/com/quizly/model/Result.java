package com.quizly.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The graded outcome of an attempt. Result objects are stored by the repository
 * and are safe to expose to JSP EL through their JavaBean getters.
 */
public final class Result {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT_FORMAT =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private final String id;
    private final String userId;
    private final String playerName;
    private final String quizId;
    private final String quizTitle;
    private final String quizIcon;
    private final String accentColor;
    private final int score;
    private final int bonusPoints;
    private final int total;
    private final int percentage;
    private final Instant completedAt;
    private final List<AnswerReview> reviews;

    public Result(String id, String playerName, String quizId, String quizTitle, String quizIcon,
                  String accentColor, int score, int total, int percentage,
                  Instant completedAt, List<AnswerReview> reviews) {
        this(id, null, playerName, quizId, quizTitle, quizIcon, accentColor,
                score, total, percentage, completedAt, reviews);
    }

    public Result(String id, String userId, String playerName, String quizId, String quizTitle, String quizIcon,
                  String accentColor, int score, int total, int percentage,
                  Instant completedAt, List<AnswerReview> reviews) {
        this(id, userId, playerName, quizId, quizTitle, quizIcon, accentColor,
                score, 0, total, percentage, completedAt, reviews);
    }

    public Result(String id, String userId, String playerName, String quizId, String quizTitle, String quizIcon,
                  String accentColor, int score, int bonusPoints, int total, int percentage,
                  Instant completedAt, List<AnswerReview> reviews) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = userId;
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.quizId = Objects.requireNonNull(quizId, "quizId");
        this.quizTitle = Objects.requireNonNull(quizTitle, "quizTitle");
        this.quizIcon = Objects.requireNonNull(quizIcon, "quizIcon");
        this.accentColor = Objects.requireNonNull(accentColor, "accentColor");
        this.score = score;
        this.bonusPoints = bonusPoints;
        this.total = total;
        this.percentage = percentage;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
        this.reviews = List.copyOf(Objects.requireNonNull(reviews, "reviews"));
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getQuizId() {
        return quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public String getQuizIcon() {
        return quizIcon;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public int getScore() {
        return score;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public int getPoints() {
        return score + bonusPoints;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentage() {
        return percentage;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getCompletedAtDisplay() {
        return DISPLAY_FORMAT.format(completedAt);
    }

    public String getCompletedDateShort() {
        return SHORT_FORMAT.format(completedAt);
    }

    public List<AnswerReview> getReviews() {
        return reviews;
    }

    public int getWrongCount() {
        return (int) reviews.stream().filter(review -> review.isAnswered() && !review.isCorrect()).count();
    }

    public int getSkippedCount() {
        return (int) reviews.stream().filter(review -> !review.isAnswered()).count();
    }

    public boolean isPassed() {
        return percentage >= 60;
    }

    public String getInitials() {
        String[] parts = playerName.strip().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "?";
        }
        StringBuilder initials = new StringBuilder(parts[0].substring(0, 1));
        if (parts.length > 1) {
            initials.append(parts[parts.length - 1], 0, 1);
        }
        return initials.toString().toUpperCase(Locale.ROOT);
    }

    public String getFeedbackTitle() {
        if (percentage == 100) {
            return "Flawless victory";
        }
        if (percentage >= 80) {
            return "Excellent work";
        }
        if (percentage >= 60) {
            return "Nicely done";
        }
        if (percentage >= 40) {
            return "Good start";
        }
        return "Keep practising";
    }

    public String getFeedbackMessage() {
        if (percentage == 100) {
            return "Every answer landed. That was a masterclass — try another track and keep the streak going.";
        }
        if (percentage >= 80) {
            return "You have a strong grasp of this topic. Review the explanations below to turn the near-misses into perfect scores.";
        }
        if (percentage >= 60) {
            return "A solid result. The review below shows exactly where the extra points are hiding.";
        }
        if (percentage >= 40) {
            return "You are building the right instincts. Read each explanation, then take the quiz again — the options are reshuffled.";
        }
        return "Every expert started here. Work through the explanations below, then give it another shot.";
    }

    public String getTone() {
        if (percentage >= 80) {
            return "great";
        }
        if (percentage >= 60) {
            return "good";
        }
        return "practice";
    }

    public String getScoreLabel() {
        return score + " / " + total;
    }
}
