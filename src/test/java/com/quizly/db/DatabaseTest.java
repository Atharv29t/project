package com.quizly.db;

import com.quizly.data.QuizCatalog;
import com.quizly.repository.QuizRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @Test
    void initializationAddsNewQuestionsWithoutRemovingStoredQuestions() {
        InMemoryDocumentStore store = new InMemoryDocumentStore();
        Map<String, Object> customQuestion = new HashMap<>();
        customQuestion.put("id", "custom-java-question");
        customQuestion.put("prompt", "Custom question");
        customQuestion.put("options", List.of("A", "B"));
        customQuestion.put("correctOptionIndex", 0);
        customQuestion.put("difficulty", 2);
        customQuestion.put("explanation", "Custom explanation");

        Map<String, Object> existingQuiz = new HashMap<>();
        existingQuiz.put("title", "Java Fundamentals");
        existingQuiz.put("tagline", "Lambdas, collections & streams");
        existingQuiz.put("description", "Existing quiz");
        existingQuiz.put("icon", "cup");
        existingQuiz.put("difficulty", "Beginner");
        existingQuiz.put("estimatedMinutes", 4);
        existingQuiz.put("accentColor", "#38BDF8");
        existingQuiz.put("sortOrder", 0);
        existingQuiz.put("questions", List.of(customQuestion));
        store.set("quizzes", "java", existingQuiz);
        Database database = new Database(store);

        database.initialize();
        database.initialize();

        var javaQuiz = new QuizRepository(database).findById("java").orElseThrow();
        assertEquals(25, javaQuiz.getQuestionCount());
        assertTrue(javaQuiz.getQuestions().stream()
                .anyMatch(question -> question.getId().equals("custom-java-question")));
        assertEquals(3, new QuizRepository(database).findAll().size());
        assertEquals(24, QuizCatalog.findById("dsa").orElseThrow().getQuestionCount());
    }
}
