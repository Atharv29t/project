package com.quizly.data;

import com.quizly.model.Quiz;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizCatalogTest {

    @Test
    void containsExactlyThreeTracks() {
        List<Quiz> quizzes = QuizCatalog.all();
        assertEquals(3, quizzes.size());
        assertEquals(Set.of("java", "dsa", "web"), quizzes.stream().map(Quiz::getId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void everyTrackHasTwelveValidQuestionsAcrossAllDifficultyLevels() {
        Set<String> questionIds = new HashSet<>();
        for (Quiz quiz : QuizCatalog.all()) {
            assertEquals(12, quiz.getQuestionCount(), quiz.getId());
            assertEquals(Set.of(1, 2, 3), quiz.getQuestions().stream()
                    .map(question -> question.getDifficulty())
                    .collect(java.util.stream.Collectors.toSet()));
            for (int difficulty = 1; difficulty <= 3; difficulty++) {
                int targetDifficulty = difficulty;
                assertEquals(4, quiz.getQuestions().stream()
                        .filter(question -> question.getDifficulty() == targetDifficulty)
                        .count(), quiz.getId() + " difficulty " + targetDifficulty);
            }
            for (var question : quiz.getQuestions()) {
                assertTrue(questionIds.add(question.getId()), () -> "Duplicate question id: " + question.getId());
                assertTrue(question.getOptions().size() >= 2);
                assertTrue(question.getCorrectOptionIndex() < question.getOptions().size());
                assertEquals(question.getOptions().get(question.getCorrectOptionIndex()), question.getCorrectOption());
            }
        }
        assertEquals(36, questionIds.size());
    }

    @Test
    void idsAreLookedUpCaseSensitivelyByQuizId() {
        assertTrue(QuizCatalog.findById("java").isPresent());
        assertTrue(QuizCatalog.findById(" brAinTraining ").isEmpty());
        assertTrue(QuizCatalog.findById(null).isEmpty());
        assertTrue(QuizCatalog.findById("").isEmpty());
        assertEquals(36, QuizCatalog.totalQuestions());
    }
}
