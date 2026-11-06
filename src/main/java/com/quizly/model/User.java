package com.quizly.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * A registered Quizly player. The password hash is only used by the
 * authentication service and is never rendered in a view.
 */
public final class User {

    private static final DateTimeFormatter MEMBER_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private final String id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Instant createdAt;
    private final Instant lastLoginAt;

    public User(String id, String name, String email, String passwordHash,
                Instant createdAt, Instant lastLoginAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lastLoginAt = lastLoginAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getInitials() {
        String[] parts = name.strip().split("\\s+");
        StringBuilder initials = new StringBuilder(parts[0].substring(0, 1));
        if (parts.length > 1) {
            initials.append(parts[parts.length - 1], 0, 1);
        }
        return initials.toString().toUpperCase(Locale.ROOT);
    }

    public String getMemberSinceDisplay() {
        return MEMBER_FORMAT.format(createdAt);
    }

    public String getLastLoginDisplay() {
        return lastLoginAt == null ? "—" : MEMBER_FORMAT.format(lastLoginAt);
    }

    public User withLastLoginAt(Instant loginAt) {
        return new User(id, name, email, passwordHash, createdAt, loginAt);
    }
}
