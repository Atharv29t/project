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

    public QuestionInstance(String id, String prompt, List<String> options,
                            String correctOption, String explanation) {
        this.id = Objects.requireNonNull(id, "id");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        this.correctOption = Objects.requireNonNull(correctOption, "correctOption");
        this.explanation = Objects.requireNonNull(explanation, "explanation");
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

    public boolean isCorrect(String answer) {
        return answer != null && correctOption.equals(answer);
    }
}
