package com.quizly.repository;

import com.quizly.db.Database;
import com.quizly.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * JDBC-backed user accounts.
 */
public final class UserRepository {

    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public void create(User user) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO users (id, name, email, password_hash, created_at, last_login_at)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, user.getId());
            statement.setString(2, user.getName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.setObject(5, OffsetDateTime.ofInstant(user.getCreatedAt(), java.time.ZoneOffset.UTC));
            statement.setObject(6, user.getLastLoginAt() == null
                    ? null
                    : OffsetDateTime.ofInstant(user.getLastLoginAt(), java.time.ZoneOffset.UTC));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create the user account", exception);
        }
    }

    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT * FROM users WHERE email = ?", email);
    }

    public Optional<User> findById(String id) {
        return queryOne("SELECT * FROM users WHERE id = ?", id);
    }

    public void recordLogin(String userId, Instant loginAt) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET last_login_at = ? WHERE id = ?")) {
            statement.setObject(1, OffsetDateTime.ofInstant(loginAt, java.time.ZoneOffset.UTC));
            statement.setString(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update the user account", exception);
        }
    }

    public long count() {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count users", exception);
        }
    }

    private Optional<User> queryOne(String sql, String parameter) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the user account", exception);
        }
    }

    private User map(ResultSet resultSet) throws SQLException {
        OffsetDateTime lastLogin = resultSet.getObject("last_login_at", OffsetDateTime.class);
        return new User(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                lastLogin == null ? null : lastLogin.toInstant()
        );
    }
}
