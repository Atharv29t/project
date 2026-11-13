package com.quizly.model;

import java.util.List;
import java.util.Objects;

/**
 * A question as it is stored in the quiz catalogue. The correct option is kept
 * on the server and is never rendered into the quiz page.
 */
public final class Question {

    private final String id;
    private final String prompt;
    private final List<String> options;
    private final int correctOptionIndex;
    private final String explanation;
    private final int difficulty;
    private final ContentStatus status;

    public Question(String id, String prompt, List<String> options, int correctOptionIndex, String explanation) {
        this(id, prompt, options, correctOptionIndex, explanation, 2);
    }

    public Question(String id, String prompt, List<String> options, int correctOptionIndex,
                    String explanation, int difficulty) {
        this(id, prompt, options, correctOptionIndex, explanation, difficulty, ContentStatus.PUBLISHED);
    }

    public Question(String id, String prompt, List<String> options, int correctOptionIndex,
                    String explanation, int difficulty, ContentStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = Objects.requireNonNull(explanation, "explanation");
        this.difficulty = difficulty;
        this.status = Objects.requireNonNull(status, "status");

        if (options.size() < 2) {
            throw new IllegalArgumentException("A question needs at least two options");
        }
        if (correctOptionIndex < 0 || correctOptionIndex >= options.size()) {
            throw new IllegalArgumentException("correctOptionIndex is out of range");
        }
        if (difficulty < 1 || difficulty > 3) {
            throw new IllegalArgumentException("difficulty must be between 1 and 3");
        }
    }

    public String getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getOptionsMultiline() {
        return String.join("\n", options);
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public String getCorrectOption() {
        return options.get(correctOptionIndex);
    }

    public String getExplanation() {
        return explanation;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public String getStatusValue() {
        return status.firestoreValue();
    }

    public boolean isPublished() {
        return status == ContentStatus.PUBLISHED;
    }
}
