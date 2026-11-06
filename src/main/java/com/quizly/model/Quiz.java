package com.quizly.model;

import java.util.List;
import java.util.Objects;

/**
 * A quiz track: metadata shown on the home page plus its immutable question
 * collection.
 */
public final class Quiz {

    private final String id;
    private final String title;
    private final String tagline;
    private final String description;
    private final String icon;
    private final String level;
    private final int estimatedMinutes;
    private final String accentColor;
    private final List<Question> questions;

    public Quiz(String id, String title, String tagline, String description, String icon,
                String level, int estimatedMinutes, String accentColor, List<Question> questions) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.tagline = Objects.requireNonNull(tagline, "tagline");
        this.description = Objects.requireNonNull(description, "description");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.level = Objects.requireNonNull(level, "level");
        this.estimatedMinutes = estimatedMinutes;
        this.accentColor = Objects.requireNonNull(accentColor, "accentColor");
        this.questions = List.copyOf(Objects.requireNonNull(questions, "questions"));
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTagline() {
        return tagline;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public String getLevel() {
        return level;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getQuestionCount() {
        return questions.size();
    }
}
