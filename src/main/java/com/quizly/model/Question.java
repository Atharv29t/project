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

    public Question(String id, String prompt, List<String> options, int correctOptionIndex, String explanation) {
        this.id = Objects.requireNonNull(id, "id");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = Objects.requireNonNull(explanation, "explanation");

        if (options.size() < 2) {
            throw new IllegalArgumentException("A question needs at least two options");
        }
        if (correctOptionIndex < 0 || correctOptionIndex >= options.size()) {
            throw new IllegalArgumentException("correctOptionIndex is out of range");
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

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public String getCorrectOption() {
        return options.get(correctOptionIndex);
    }

    public String getExplanation() {
        return explanation;
    }
}
