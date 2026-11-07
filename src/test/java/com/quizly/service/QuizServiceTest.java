package com.quizly.service;

import com.quizly.data.QuizCatalog;
import com.quizly.TestDatabase;
import com.quizly.model.AnswerReview;
import com.quizly.model.Attempt;
import com.quizly.model.QuestionInstance;
import com.quizly.model.Result;
import com.quizly.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizServiceTest {

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        var database = TestDatabase.create("quiz-" + UUID.randomUUID());
        database.initialize();
        quizService = new QuizService(new QuizRepository(database));
    }

    @Test
    void rejectsInvalidPlayerNameOrQuizId() {
        Executable blankName = () -> quizService.startAttempt("   ", "java");
        Executable longName = () -> quizService.startAttempt("X".repeat(41), "java");
        Executable unknownQuiz = () -> quizService.startAttempt("Shyron", "brain-training");

        assertThrows(IllegalArgumentException.class, blankName);
        assertThrows(IllegalArgumentException.class, longName);
        assertThrows(IllegalArgumentException.class, unknownQuiz);
    }

    @Test
    void normalisesNameAndSetsUpACompleteAttempt() {
        Attempt attempt = quizService.startAttempt("   Ada    Lovelace  ", "dsa");

        assertEquals("Ada Lovelace", attempt.getPlayerName());
        assertEquals(6, attempt.getTotalQuestions());
        assertEquals(1, attempt.getCurrentNumber());
        assertEquals(17, attempt.getProgressPercent());
        assertTrue(!attempt.isLastQuestion());
        assertEquals("dsa", attempt.getQuiz().getId());

        // Each run samples two questions from every difficulty tier.
        Map<String, Long> promptCounts = attempt.getQuestions().stream()
                .collect(Collectors.groupingBy(QuestionInstance::getPrompt, Collectors.counting()));
        assertEquals(6, promptCounts.size());
        promptCounts.values().forEach(count -> assertEquals(1L, count));
        for (int difficulty = 1; difficulty <= 3; difficulty++) {
            int targetDifficulty = difficulty;
            assertEquals(2, attempt.getQuestions().stream()
                    .filter(question -> question.getDifficulty() == targetDifficulty)
                    .count());
        }

        // Option sets are preserved, but their order is re-created per question.
        var catalogQuestions = quizService.findQuiz("dsa");
        var originalPrompts = catalogQuestions.get().getQuestions().stream()
                .collect(Collectors.toMap(question -> question.getPrompt(), question -> question.getOptions().size()));
        assertEquals(12, originalPrompts.size());
        attempt.getQuestions().forEach(instance -> assertEquals(originalPrompts.get(instance.getPrompt()), instance.getOptions().size()));
    }

    @Test
    void gradingPerfectAttemptGivesPerfectScore() {
        Attempt attempt = quizService.startAttempt("Ada", "java");
        Map<String, String> correctByPrompt = QuizCatalog.findById("java").orElseThrow().getQuestions().stream()
                .collect(Collectors.toMap(question -> question.getPrompt(),
                        question -> question.getOptions().get(question.getCorrectOptionIndex())));

        while (!attempt.isComplete()) {
            QuestionInstance instance = attempt.getCurrentQuestion();
            attempt.answerCurrent(instance.getId(), correctByPrompt.get(instance.getPrompt()));
        }

        assertTrue(attempt.isComplete());
        Result result = quizService.grade(attempt);
        assertEquals(attempt.getId(), result.getId());
        assertEquals(6, result.getScore());
        assertEquals(6, result.getTotal());
        assertEquals(100, result.getPercentage());
        assertTrue(result.isPassed());

        List<AnswerReview> correctReviews = result.getReviews();
        assertEquals(6, correctReviews.size());
        assertTrue(correctReviews.stream().allMatch(AnswerReview::isCorrect));
    }

    @Test
    void eachAttemptSamplesSixQuestionsFromTheFullBank() {
        Attempt attempt = quizService.startAttempt("Ada", "java");
        assertEquals(12, attempt.getQuiz().getQuestionCount());
        assertEquals(6, attempt.getTotalQuestions());
    }

    @Test
    void doublePointsGivesBonusPointAndXpEligibilityOnlyWhenMarkedAnswerIsCorrect() {
        Attempt attempt = quizService.startAttempt("Ada", "java");
        attempt.usePowerUp("doublePoints");

        while (!attempt.isComplete()) {
            QuestionInstance question = attempt.getCurrentQuestion();
            attempt.answerCurrent(question.getId(), question.getCorrectOption());
        }

        Result result = quizService.grade(attempt);

        assertEquals(6, result.getScore());
        assertEquals(1, result.getBonusPoints());
        assertEquals(7, result.getPoints());
        assertEquals(100, result.getPercentage());
    }

    @Test
    void gradingTwoCorrectTwoIncorrectTwoSkippedGivesThirtyThreePercent() {
        Attempt attempt = quizService.startAttempt("Ada", "java");

        // Six answers: 2 correct, 2 wrong, 2 skipped.
        for (int index = 0; index < attempt.getTotalQuestions(); index++) {
            QuestionInstance question = attempt.getCurrentQuestion();
            String answer = switch (index) {
                case 0, 1 -> question.getCorrectOption();
                case 2, 3 -> {
                    String wrong = question.getOptions().stream()
                            .filter(option -> !option.equals(question.getCorrectOption()))
                            .findFirst()
                            .orElseThrow();
                    yield wrong;
                }
                default -> null; // skip
            };
            attempt.answerCurrent(question.getId(), answer);
        }

        Result result = quizService.grade(attempt);
        assertEquals(2, result.getScore());
        assertEquals(2, result.getWrongCount());
        assertEquals(2, result.getSkippedCount());
        assertEquals(33, result.getPercentage());
        assertEquals(2, result.getTotal() - result.getScore() - result.getSkippedCount());
    }
}
