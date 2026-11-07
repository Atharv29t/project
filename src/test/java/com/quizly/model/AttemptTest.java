package com.quizly.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptTest {

    @Test
    void correctAnswerSelectsAHarderNextQuestion() {
        Attempt attempt = attempt();

        attempt.answerCurrent("medium", "right");

        assertEquals("hard", attempt.getCurrentQuestion().getId());
        assertEquals(3, attempt.getCurrentQuestion().getDifficulty());
    }

    @Test
    void incorrectAnswerSelectsAnEasierNextQuestion() {
        Attempt attempt = attempt();

        attempt.answerCurrent("medium", "wrong");

        assertEquals("easy", attempt.getCurrentQuestion().getId());
        assertEquals(1, attempt.getCurrentQuestion().getDifficulty());
    }

    @Test
    void bothPowerUpsCanBeUsedOncePerQuizAndCanApplyToTheSameQuestion() {
        Attempt attempt = attempt();
        attempt.usePowerUp("fiftyFifty");
        assertEquals(2, attempt.getCurrentOptions().size());
        assertTrue(attempt.getCurrentOptions().contains("right"));
        assertThrows(IllegalStateException.class, () -> attempt.usePowerUp("fiftyFifty"));

        attempt.usePowerUp("doublePoints");
        assertEquals("medium", attempt.getDoublePointsQuestionId());
        assertThrows(IllegalStateException.class, () -> attempt.usePowerUp("doublePoints"));
        assertFalse(attempt.isPowerUpAvailable("fiftyFifty"));
        assertFalse(attempt.isPowerUpAvailable("doublePoints"));
    }

    @Test
    void removedPowerUpsAreNotAccepted() {
        Attempt attempt = attempt();
        assertThrows(IllegalArgumentException.class, () -> attempt.usePowerUp("freezeTimer"));
        assertThrows(IllegalArgumentException.class, () -> attempt.usePowerUp("skip"));
    }

    private Attempt attempt() {
        Quiz quiz = new Quiz("test", "Test", "Test", "Test", "?", "Test", 1, "#000000", List.of());
        return new Attempt("attempt", "Ada", quiz, List.of(
                question("medium", 2),
                question("easy", 1),
                question("hard", 3)
        ), Instant.now());
    }

    private QuestionInstance question(String id, int difficulty) {
        return new QuestionInstance(id, id, List.of("wrong", "right"), "right", "explain", difficulty);
    }
}
