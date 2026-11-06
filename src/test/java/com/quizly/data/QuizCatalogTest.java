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
    void everyQuestionIsValidAndUniqueId() {
        Set<String> questionIds = new HashSet<>();
        for (Quiz quiz : QuizCatalog.all()) {
            for (var question : quiz.getQuestions()) {
                assertTrue(questionIds.add(question.getId()), () -> "Duplicate question id: " + question.getId());
                assertTrue(question.getOptions().size() >= 2);
                assertTrue(question.getCorrectOptionIndex() < question.getOptions().size());
                assertEquals(question.getOptions().get(question.getCorrectOptionIndex()), question.getCorrectOption());
            }
        }
        assertEquals(18, questionIds.size());
    }

    @Test
    void idsAreLookedUpCaseSensitivelyByQuizId() {
        assertTrue(QuizCatalog.findById("java").isPresent());
        assertTrue(QuizCatalog.findById(" brAinTraining ").isEmpty());
        assertTrue(QuizCatalog.findById(null).isEmpty());
        assertTrue(QuizCatalog.findById("").isEmpty());
        assertEquals(18, QuizCatalog.totalQuestions());
    }
}
