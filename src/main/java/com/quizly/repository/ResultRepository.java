package com.quizly.repository;

import com.quizly.db.Database;
import com.quizly.model.AnswerReview;
import com.quizly.model.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed result repository. Completed attempts and their answer reviews
 * survive application restarts.
 */
public final class ResultRepository {

    private final Database database;

    public ResultRepository(Database database) {
        this.database = database;
    }

    public void save(Result result) {
        if (result.getReviews().isEmpty()) {
            throw new IllegalArgumentException("A stored result must contain reviews for validation");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                insertResult(connection, result);
                insertReviews(connection, result);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the quiz result", exception);
        }
    }

    public List<Result> findAll() {
        return queryResults("SELECT * FROM results ORDER BY completed_at DESC", null);
    }

    public List<Result> findByQuiz(String quizId) {
        return queryResults("SELECT * FROM results WHERE quiz_id = ? ORDER BY completed_at DESC", quizId);
    }

    public List<Result> findByUser(String userId) {
        return queryResults("SELECT * FROM results WHERE user_id = ? ORDER BY completed_at DESC", userId);
    }

    public Optional<Result> findById(String id) {
        String sql = "SELECT * FROM results WHERE id = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Result result = mapResult(resultSet, loadReviews(connection, id));
                return Optional.of(result);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the quiz result", exception);
        }
    }

    public long count() {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM results");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count results", exception);
        }
    }

    private List<Result> queryResults(String sql, String parameter) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Result> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapResult(resultSet, List.of()));
                }
                return List.copyOf(results);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read quiz results", exception);
        }
    }

    private void insertResult(Connection connection, Result result) throws SQLException {
        String sql = """
                INSERT INTO results
                    (id, user_id, player_name, quiz_id, quiz_title, quiz_icon, accent_color,
                     score, total, percentage, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, result.getId());
            statement.setString(2, result.getUserId());
            statement.setString(3, result.getPlayerName());
            statement.setString(4, result.getQuizId());
            statement.setString(5, result.getQuizTitle());
            statement.setString(6, result.getQuizIcon());
            statement.setString(7, result.getAccentColor());
            statement.setInt(8, result.getScore());
            statement.setInt(9, result.getTotal());
            statement.setInt(10, result.getPercentage());
            statement.setObject(11, OffsetDateTime.ofInstant(result.getCompletedAt(), ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    private void insertReviews(Connection connection, Result result) throws SQLException {
        String sql = """
                INSERT INTO result_answers
                    (result_id, question_number, prompt, selected_answer, correct_answer, explanation)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (AnswerReview review : result.getReviews()) {
                statement.setString(1, result.getId());
                statement.setInt(2, review.getNumber());
                statement.setString(3, review.getPrompt());
                statement.setString(4, review.getSelectedAnswer());
                statement.setString(5, review.getCorrectAnswer());
                statement.setString(6, review.getExplanation());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<AnswerReview> loadReviews(Connection connection, String resultId) throws SQLException {
        String sql = """
                SELECT question_number, prompt, selected_answer, correct_answer, explanation
                FROM result_answers
                WHERE result_id = ?
                ORDER BY question_number
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resultId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AnswerReview> reviews = new ArrayList<>();
                while (resultSet.next()) {
                    reviews.add(new AnswerReview(
                            resultSet.getInt("question_number"),
                            resultSet.getString("prompt"),
                            resultSet.getString("selected_answer"),
                            resultSet.getString("correct_answer"),
                            resultSet.getString("explanation")
                    ));
                }
                return List.copyOf(reviews);
            }
        }
    }

    private Result mapResult(ResultSet resultSet, List<AnswerReview> reviews) throws SQLException {
        return new Result(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("player_name"),
                resultSet.getString("quiz_id"),
                resultSet.getString("quiz_title"),
                resultSet.getString("quiz_icon"),
                resultSet.getString("accent_color"),
                resultSet.getInt("score"),
                resultSet.getInt("total"),
                resultSet.getInt("percentage"),
                resultSet.getObject("completed_at", OffsetDateTime.class).toInstant(),
                reviews
        );
    }
}
