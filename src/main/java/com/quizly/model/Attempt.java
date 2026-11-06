package com.quizly.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable state for one player's run through a quiz. The instance lives in the
 * HTTP session and is discarded once the quiz has been graded.
 */
public final class Attempt {

    private final String id;
    private final String userId;
    private final String playerName;
    private final Quiz quiz;
    private final List<QuestionInstance> questions;
    private final Map<String, String> answers = new LinkedHashMap<>();
    private final Instant startedAt;
    private int currentIndex;

    public Attempt(String id, String playerName, Quiz quiz, List<QuestionInstance> questions, Instant startedAt) {
        this(id, null, playerName, quiz, questions, startedAt);
    }

    public Attempt(String id, String userId, String playerName, Quiz quiz,
                   List<QuestionInstance> questions, Instant startedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = userId;
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.quiz = Objects.requireNonNull(quiz, "quiz");
        this.questions = List.copyOf(Objects.requireNonNull(questions, "questions"));
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("An attempt needs at least one question");
        }
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

    public Quiz getQuiz() {
        return quiz;
    }

    public List<QuestionInstance> getQuestions() {
        return questions;
    }

    public Map<String, String> getAnswers() {
        return Collections.unmodifiableMap(answers);
    }

    public Optional<String> getAnswer(String questionId) {
        return Optional.ofNullable(answers.get(questionId)).filter(value -> !value.isBlank());
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getCurrentNumber() {
        return Math.min(currentIndex + 1, questions.size());
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public int getProgressPercent() {
        return (int) Math.round((currentIndex + 1) * 100.0 / questions.size());
    }

    public boolean isLastQuestion() {
        return currentIndex == questions.size() - 1;
    }

    public boolean isComplete() {
        return currentIndex >= questions.size();
    }

    public QuestionInstance getCurrentQuestion() {
        if (isComplete()) {
            throw new IllegalStateException("The attempt has no current question");
        }
        return questions.get(currentIndex);
    }

    /**
     * Stores the answer for the current question and advances the cursor.
     * A blank answer is recorded as skipped.
     */
    public void answerCurrent(String questionId, String answer) {
        QuestionInstance question = getCurrentQuestion();
        if (!question.getId().equals(questionId)) {
            throw new IllegalArgumentException("That question is not the current question");
        }

        String cleanAnswer = answer == null ? "" : answer.strip();
        if (!cleanAnswer.isEmpty() && !question.getOptions().contains(cleanAnswer)) {
            throw new IllegalArgumentException("Please choose one of the available options");
        }

        answers.put(questionId, cleanAnswer);
        currentIndex++;
    }
}
