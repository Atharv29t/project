package com.quizly.model;

import java.util.List;
import java.util.Objects;

/**
 * A per-attempt copy of a question. Options are shuffled for every attempt so
 * repeating a quiz does not mean memorising answer positions.
 */
public final class QuestionInstance {

    private final String id;
    private final String prompt;
    private final List<String> options;
    private final String correctOption;
    private final String explanation;
    private final int difficulty;

    public QuestionInstance(String id, String prompt, List<String> options,
                            String correctOption, String explanation) {
        this(id, prompt, options, correctOption, explanation, 2);
    }

    public QuestionInstance(String id, String prompt, List<String> options,
                            String correctOption, String explanation, int difficulty) {
        this.id = Objects.requireNonNull(id, "id");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.correctOption = Objects.requireNonNull(correctOption, "correctOption");
        this.explanation = Objects.requireNonNull(explanation, "explanation");
        if (difficulty < 1 || difficulty > 3) {
            throw new IllegalArgumentException("difficulty must be between 1 and 3");
        }
        this.difficulty = difficulty;
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

    public String getCorrectOption() {
        return correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public boolean isCorrect(String answer) {
        return answer != null && correctOption.equals(answer);
    }
}
