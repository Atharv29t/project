package com.quizly.service;

import com.quizly.db.Database;
import com.quizly.model.User;
import com.quizly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private AuthService authService;
    private UserRepository users;

    @BeforeEach
    void setUp() {
        Database database = Database.inMemory("auth-" + UUID.randomUUID());
        database.initialize();
        users = new UserRepository(database);
        authService = new AuthService(users);
    }

    @Test
    void registersAndLogsInWithHashedPassword() {
        User user = authService.register(" Ada Lovelace ", " ADA@Example.com ", "analytical-engine", "analytical-engine");

        assertEquals("Ada Lovelace", user.getName());
        assertEquals("ada@example.com", user.getEmail());
        assertFalse(user.getPasswordHash().contains("analytical-engine"));
        assertEquals(1, users.count());

        User loggedIn = authService.login("ada@example.com", "analytical-engine");
        assertEquals(user.getId(), loggedIn.getId());
        assertNotNull(loggedIn.getLastLoginAt());
    }

    @Test
    void rejectsDuplicateEmailAndWrongPassword() {
        authService.register("Ada", "ada@example.com", "password123", "password123");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Grace", "ADA@example.com", "password123", "password123"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("ada@example.com", "not-the-password"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("missing@example.com", "password123"));
    }

    @Test
    void validatesRegistrationFields() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("A", "ada@example.com", "password123", "password123"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Ada", "not-an-email", "password123", "password123"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Ada", "ada@example.com", "short", "short"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Ada", "ada@example.com", "password123", "different123"));
    }
}
