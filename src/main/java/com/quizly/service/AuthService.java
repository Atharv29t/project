package com.quizly.service;

import com.quizly.model.User;
import com.quizly.repository.UserRepository;
import com.quizly.security.PasswordHasher;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Registration, login and input validation for player accounts.
 */
public final class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 40;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String rawName, String rawEmail, String password, String confirmPassword) {
        String name = normaliseName(rawName);
        String email = normaliseEmail(rawEmail);
        validatePassword(password, confirmPassword);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with that email already exists");
        }

        Instant now = Instant.now();
        User user = new User(
                UUID.randomUUID().toString(),
                name,
                email,
                PasswordHasher.hash(password),
                now,
                now
        );
        userRepository.create(user);
        return user;
    }

    public User login(String rawEmail, String password) {
        String email = normaliseEmail(rawEmail);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }

        Optional<User> candidate = userRepository.findByEmail(email);
        if (candidate.isEmpty() || !PasswordHasher.matches(password, candidate.get().getPasswordHash())) {
            throw new IllegalArgumentException("Email or password is incorrect");
        }

        Instant now = Instant.now();
        userRepository.recordLogin(candidate.get().getId(), now);
        return candidate.get().withLastLoginAt(now);
    }

    private String normaliseName(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Please enter your name");
        }
        String name = rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name must be 2–40 characters long");
        }
        return name;
    }

    private String normaliseEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("Please enter your email address");
        }
        String email = rawEmail.strip().toLowerCase(Locale.ROOT);
        if (email.length() > 190 || !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        return email;
    }

    private void validatePassword(String password, String confirmPassword) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at most 72 characters long");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("The two passwords do not match");
        }
    }
}
