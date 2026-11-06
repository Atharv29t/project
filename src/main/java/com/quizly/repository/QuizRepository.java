package com.quizly.repository;

import com.quizly.db.Database;
import com.quizly.model.Question;
import com.quizly.model.Quiz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the quiz catalogue, questions and options from the database.
 */
public final class QuizRepository {

    private final Database database;

    public QuizRepository(Database database) {
        this.database = database;
    }

    public List<Quiz> findAll() {
        String sql = """
                SELECT id, title, tagline, description, icon, difficulty,
                       estimated_minutes, accent_color
                FROM quizzes
                ORDER BY sort_order, title
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Quiz> quizzes = new ArrayList<>();
            while (resultSet.next()) {
                quizzes.add(mapQuiz(connection, resultSet));
            }
            return List.copyOf(quizzes);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the quiz catalogue", exception);
        }
    }

    public Optional<Quiz> findById(String quizId) {
        if (quizId == null || quizId.isBlank()) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, title, tagline, description, icon, difficulty,
                       estimated_minutes, accent_color
                FROM quizzes
                WHERE id = ?
                """;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapQuiz(connection, resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the quiz", exception);
        }
    }

    public int totalQuestions() {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM questions");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count questions", exception);
        }
    }

    private Quiz mapQuiz(Connection connection, ResultSet resultSet) throws SQLException {
        String quizId = resultSet.getString("id");
        return new Quiz(
                quizId,
                resultSet.getString("title"),
                resultSet.getString("tagline"),
                resultSet.getString("description"),
                resultSet.getString("icon"),
                resultSet.getString("difficulty"),
                resultSet.getInt("estimated_minutes"),
                resultSet.getString("accent_color"),
                loadQuestions(connection, quizId)
        );
    }

    private List<Question> loadQuestions(Connection connection, String quizId) throws SQLException {
        String sql = """
                SELECT id, prompt, explanation
                FROM questions
                WHERE quiz_id = ?
                ORDER BY sort_order
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Question> questions = new ArrayList<>();
                while (resultSet.next()) {
                    questions.add(loadQuestion(connection, resultSet));
                }
                return List.copyOf(questions);
            }
        }
    }

    private Question loadQuestion(Connection connection, ResultSet resultSet) throws SQLException {
        String questionId = resultSet.getString("id");
        List<String> options = new ArrayList<>();
        int correctIndex = 0;

        String sql = """
                SELECT option_text, is_correct
                FROM question_options
                WHERE question_id = ?
                ORDER BY sort_order
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, questionId);
            try (ResultSet optionsResult = statement.executeQuery()) {
                while (optionsResult.next()) {
                    if (optionsResult.getBoolean("is_correct")) {
                        correctIndex = options.size();
                    }
                    options.add(optionsResult.getString("option_text"));
                }
            }
        }

        return new Question(
                questionId,
                resultSet.getString("prompt"),
                options,
                correctIndex,
                resultSet.getString("explanation")
        );
    }
}
