package com.quizly.model;

import java.util.Objects;

/**
 * Review row shown on the result page. It contains both the player's answer and
 * the correct answer so the UI can explain the outcome.
 */
public final class AnswerReview {

    private final int number;
    private final String prompt;
    private final String selectedAnswer;
    private final String correctAnswer;
    private final String explanation;

    public AnswerReview(int number, String prompt, String selectedAnswer,
                        String correctAnswer, String explanation) {
        this.number = number;
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.selectedAnswer = selectedAnswer == null || selectedAnswer.isBlank() ? null : selectedAnswer;
        this.correctAnswer = Objects.requireNonNull(correctAnswer, "correctAnswer");
        this.explanation = Objects.requireNonNull(explanation, "explanation");
    }

    public int getNumber() {
        return number;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public String getSelectedAnswerDisplay() {
        return selectedAnswer == null ? "Not answered" : selectedAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isAnswered() {
        return selectedAnswer != null;
    }

    public boolean isCorrect() {
        return correctAnswer.equals(selectedAnswer);
    }

    public String getStatus() {
        if (!isAnswered()) {
            return "Skipped";
        }
        return isCorrect() ? "Correct" : "Incorrect";
    }

    public String getStatusClass() {
        if (!isAnswered()) {
            return "skipped";
        }
        return isCorrect() ? "correct" : "incorrect";
    }
}
