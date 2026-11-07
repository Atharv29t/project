package com.quizly.repository;

import com.quizly.db.Database;
import com.quizly.db.DocumentStore;
import com.quizly.model.User;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Firestore-backed player accounts.
 */
public final class UserRepository {

    private static final String USERS = "users";
    private static final String EMAIL_LOOKUPS = "userEmailLookups";
    private final DocumentStore store;

    public UserRepository(Database database) {
        this.store = database.store();
    }

    public void create(User user) {
        String emailKey = emailKey(user.getEmail());
        Map<String, Object> document = new HashMap<>();
        document.put("name", user.getName());
        document.put("email", user.getEmail());
        document.put("passwordHash", user.getPasswordHash());
        document.put("createdAt", Date.from(user.getCreatedAt()));
        document.put("lastLoginAt", user.getLastLoginAt() == null ? null : Date.from(user.getLastLoginAt()));
        store.transact(transaction -> {
            if (transaction.find(EMAIL_LOOKUPS, emailKey) != null) {
                throw new IllegalStateException("A user account with that ID or email already exists");
            }
            if (transaction.find(USERS, user.getId()) != null) {
                throw new IllegalStateException("A user account with that ID or email already exists");
            }
            transaction.create(USERS, user.getId(), document);
            transaction.create(EMAIL_LOOKUPS, emailKey, Map.of("userId", user.getId()));
            return null;
        });
    }

    public Optional<User> findByEmail(String email) {
        String key = emailKey(email);
        Map<String, Object> lookup = store.find(EMAIL_LOOKUPS, key);
        if (lookup != null && lookup.get("userId") instanceof String userId) {
            return findById(userId);
        }
        return store.findAll(USERS).stream()
                .filter(document -> email.equalsIgnoreCase((String) document.get("email")))
                .findFirst().map(this::map);
    }

    public Optional<User> findById(String id) {
        Map<String, Object> document = store.find(USERS, id);
        return document == null ? Optional.empty() : Optional.of(map(document));
    }

    public void recordLogin(String userId, Instant loginAt) {
        store.update(USERS, userId, Map.of("lastLoginAt", Date.from(loginAt)));
    }

    public long count() {
        return store.findAll(USERS).size();
    }

    private User map(Map<String, Object> document) {
        return new User(
                (String) document.get("_id"),
                (String) document.get("name"),
                (String) document.get("email"),
                (String) document.get("passwordHash"),
                instant(document.get("createdAt")),
                document.get("lastLoginAt") == null ? null : instant(document.get("lastLoginAt"))
        );
    }

    private Instant instant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof com.google.cloud.Timestamp timestamp) {
            return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("Unexpected Firestore timestamp value");
    }

    private String emailKey(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
