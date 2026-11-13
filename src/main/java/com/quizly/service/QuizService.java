package com.quizly.service;

import com.quizly.model.AnswerReview;
import com.quizly.model.Attempt;
import com.quizly.model.Question;
import com.quizly.model.QuestionInstance;
import com.quizly.model.Quiz;
import com.quizly.model.Result;
import com.quizly.model.User;
import com.quizly.repository.QuizRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Creates shuffled quiz attempts and grades the answers at the end.
 */
public final class QuizService {

    public static final int QUESTIONS_PER_ATTEMPT = 6;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 40;
    private static final int PASS_MARK = 60;

    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public Attempt startAttempt(String rawName, String rawQuizId) {
        return startAttempt(null, rawName, rawQuizId);
    }

    public Attempt startAttemptForUser(User user, String rawQuizId) {
        if (user == null) {
            throw new IllegalArgumentException("Please log in before starting a quiz");
        }
        return startAttempt(user.getId(), user.getName(), rawQuizId);
    }

    private Attempt startAttempt(String userId, String rawName, String rawQuizId) {
        String playerName = normaliseName(rawName);
        Quiz quiz = quizRepository.findPublishedById(rawQuizId)
                .orElseThrow(() -> new IllegalArgumentException("Please choose a valid quiz"));

        List<QuestionInstance> questionBank = quiz.getQuestions().stream()
                .map(this::toShuffledInstance)
                .collect(Collectors.toCollection(ArrayList::new));
        List<QuestionInstance> shuffledQuestions = selectAttemptQuestions(questionBank);
        int easiestIndex = IntStream.range(0, shuffledQuestions.size())
                .boxed()
                .min(java.util.Comparator.comparingInt(index -> shuffledQuestions.get(index).getDifficulty()))
                .orElseThrow();
        java.util.Collections.swap(shuffledQuestions, 0, easiestIndex);

        return new Attempt(UUID.randomUUID().toString(), userId, playerName, quiz, shuffledQuestions, Instant.now());
    }

    private List<QuestionInstance> selectAttemptQuestions(List<QuestionInstance> questionBank) {
        List<QuestionInstance> selected = new ArrayList<>();
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            int targetDifficulty = difficulty;
            List<QuestionInstance> candidates = questionBank.stream()
                    .filter(question -> question.getDifficulty() == targetDifficulty)
                    .collect(Collectors.toCollection(ArrayList::new));
            java.util.Collections.shuffle(candidates);
            selected.addAll(candidates.subList(0, Math.min(2, candidates.size())));
        }
        if (selected.size() < QUESTIONS_PER_ATTEMPT) {
            List<QuestionInstance> remaining = new ArrayList<>(questionBank);
            remaining.removeAll(selected);
            java.util.Collections.shuffle(remaining);
            selected.addAll(remaining.subList(0,
                    Math.min(QUESTIONS_PER_ATTEMPT - selected.size(), remaining.size())));
        }
        java.util.Collections.shuffle(selected);
        return selected;
    }

    public Optional<Quiz> findQuiz(String quizId) {
        return quizRepository.findPublishedById(quizId);
    }

    /**
     * Evaluates every stored answer and builds a reviewable result.
     */
    public Result grade(Attempt attempt) {
        List<AnswerReview> reviews = IntStream.range(0, attempt.getQuestions().size())
                .mapToObj(index -> {
                    QuestionInstance question = attempt.getQuestions().get(index);
                    String answer = attempt.getAnswers().get(question.getId());
                    return new AnswerReview(
                            index + 1,
                            question.getPrompt(),
                            answer,
                            question.getCorrectOption(),
                            question.getExplanation()
                    );
                })
                .toList();

        int score = reviews.stream().filter(AnswerReview::isCorrect).mapToInt(review -> 1).sum();
        int total = reviews.size();
        int percentage = total == 0 ? 0 : (int) Math.round(score * 100.0 / total);
        int bonusPoints = 0;
        String doublePointsQuestionId = attempt.getDoublePointsQuestionId();
        if (doublePointsQuestionId != null) {
            for (QuestionInstance question : attempt.getQuestions()) {
                if (question.getId().equals(doublePointsQuestionId)
                        && question.isCorrect(attempt.getAnswers().get(question.getId()))) {
                    bonusPoints = 1;
                    break;
                }
            }
        }
        return new Result(
                attempt.getId(),
                attempt.getUserId(),
                attempt.getPlayerName(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getQuiz().getIcon(),
                attempt.getQuiz().getAccentColor(),
                score,
                bonusPoints,
                total,
                percentage,
                Instant.now(),
                reviews
        );
    }

    public int passMark() {
        return PASS_MARK;
    }

    private String normaliseName(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Please enter your name");
        }
        String name = rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name must be 1–40 characters long");
        }
        return name;
    }

    private QuestionInstance toShuffledInstance(Question question) {
        List<String> shuffledOptions = new ArrayList<>(question.getOptions());
        java.util.Collections.shuffle(shuffledOptions);
        return new QuestionInstance(
                question.getId(),
                question.getPrompt(),
                shuffledOptions,
                question.getCorrectOption(),
                question.getExplanation(),
                question.getDifficulty()
        );
    }
}
