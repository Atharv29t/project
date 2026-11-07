package com.quizly.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable state for one player's run through a quiz. The instance lives in the
 * HTTP session and is discarded once the quiz has been graded.
 */
public final class Attempt {

    private static final long QUESTION_SECONDS = 30;
    private final String id;
    private final String userId;
    private final String playerName;
    private final Quiz quiz;
    private final List<QuestionInstance> questions;
    private final Map<String, String> answers = new LinkedHashMap<>();
    private final Set<String> usedPowerUps = new LinkedHashSet<>();
    private final Instant startedAt;
    private int currentIndex;
    private Instant questionDeadline;
    private String fiftyFiftyQuestionId;
    private List<String> fiftyFiftyOptions = List.of();
    private String doublePointsQuestionId;

    public Attempt(String id, String playerName, Quiz quiz, List<QuestionInstance> questions, Instant startedAt) {
        this(id, null, playerName, quiz, questions, startedAt);
    }

    public Attempt(String id, String userId, String playerName, Quiz quiz,
                   List<QuestionInstance> questions, Instant startedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = userId;
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.quiz = Objects.requireNonNull(quiz, "quiz");
        this.questions = new ArrayList<>(Objects.requireNonNull(questions, "questions"));
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.questionDeadline = startedAt.plusSeconds(QUESTION_SECONDS);

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

    public synchronized List<QuestionInstance> getQuestions() {
        return List.copyOf(questions);
    }

    public synchronized Map<String, String> getAnswers() {
        return Map.copyOf(answers);
    }

    public synchronized Optional<String> getAnswer(String questionId) {
        return Optional.ofNullable(answers.get(questionId)).filter(value -> !value.isBlank());
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized int getCurrentNumber() {
        return Math.min(currentIndex + 1, questions.size());
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public synchronized int getProgressPercent() {
        return (int) Math.round((currentIndex + 1) * 100.0 / questions.size());
    }

    public synchronized boolean isLastQuestion() {
        return currentIndex == questions.size() - 1;
    }

    public synchronized boolean isComplete() {
        return currentIndex >= questions.size();
    }

    public synchronized QuestionInstance getCurrentQuestion() {
        if (isComplete()) {
            throw new IllegalStateException("The attempt has no current question");
        }
        return questions.get(currentIndex);
    }

    public synchronized List<String> getCurrentOptions() {
        QuestionInstance current = getCurrentQuestion();
        return current.getId().equals(fiftyFiftyQuestionId) ? fiftyFiftyOptions : current.getOptions();
    }

    public synchronized Set<String> getUsedPowerUps() {
        return Set.copyOf(usedPowerUps);
    }

    public synchronized boolean isPowerUpAvailable(String name) {
        return !usedPowerUps.contains(name);
    }

    public synchronized String getDoublePointsQuestionId() {
        return doublePointsQuestionId;
    }

    public synchronized long getRemainingSeconds() {
        long millis = java.time.Duration.between(Instant.now(), questionDeadline).toMillis();
        return Math.max(0, (millis + 999) / 1000);
    }

    public synchronized void usePowerUp(String name) {
        QuestionInstance current = getCurrentQuestion();
        if (getRemainingSeconds() == 0) {
            throw new IllegalStateException("Time is up for this question");
        }
        if (!Set.of("fiftyFifty", "doublePoints").contains(name)) {
            throw new IllegalArgumentException("Unknown power-up");
        }
        if (!usedPowerUps.add(name)) {
            throw new IllegalStateException("You have already used this power-up");
        }
        switch (name) {
            case "fiftyFifty" -> {
                List<String> wrongOptions = new ArrayList<>(current.getOptions());
                wrongOptions.remove(current.getCorrectOption());
                Collections.shuffle(wrongOptions);
                fiftyFiftyQuestionId = current.getId();
                fiftyFiftyOptions = List.of(current.getCorrectOption(), wrongOptions.get(0));
            }
            case "doublePoints" -> doublePointsQuestionId = current.getId();
            default -> throw new IllegalStateException("Unhandled power-up");
        }
    }

    /**
     * Stores the answer for the current question and advances the cursor.
     * A blank answer is recorded as skipped.
     */
    public synchronized void answerCurrent(String questionId, String answer) {
        QuestionInstance question = getCurrentQuestion();
        if (!question.getId().equals(questionId)) {
            throw new IllegalArgumentException("That question is not the current question");
        }

        String cleanAnswer = Instant.now().isAfter(questionDeadline) || answer == null ? "" : answer.strip();
        if (!cleanAnswer.isEmpty() && !question.getOptions().contains(cleanAnswer)) {
            throw new IllegalArgumentException("Please choose one of the available options");
        }

        answers.put(questionId, cleanAnswer);
        advance(question.isCorrect(cleanAnswer));
    }

    private void advance(boolean answeredCorrectly) {
        int previousIndex = currentIndex;
        currentIndex++;
        fiftyFiftyQuestionId = null;
        fiftyFiftyOptions = List.of();
        if (isComplete()) {
            return;
        }

        int targetDifficulty = questions.get(previousIndex).getDifficulty();
        if (answeredCorrectly) {
            targetDifficulty = Math.min(3, targetDifficulty + 1);
        } else {
            targetDifficulty = Math.max(1, targetDifficulty - 1);
        }

        int selectedIndex = currentIndex;
        int closestDifference = Integer.MAX_VALUE;
        for (int index = currentIndex; index < questions.size(); index++) {
            int difference = Math.abs(questions.get(index).getDifficulty() - targetDifficulty);
            if (difference < closestDifference) {
                selectedIndex = index;
                closestDifference = difference;
            }
        }
        Collections.swap(questions, currentIndex, selectedIndex);
        questionDeadline = Instant.now().plusSeconds(QUESTION_SECONDS);
    }
}
