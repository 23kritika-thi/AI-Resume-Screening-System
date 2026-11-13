package com.resumescreening.services;

import com.resumescreening.database.DatabaseConfig;
import com.resumescreening.database.DatabaseConnection;
import com.resumescreening.models.Recruiter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecruiterAuthServiceTest {

    private final DatabaseService databaseService = new DatabaseService();
    private final RecruiterAuthService authService = new RecruiterAuthService(databaseService);
    private final List<String> testEmails = new ArrayList<>();

    @AfterEach
    void removeTestAccounts() throws SQLException {
        String sql = "DELETE FROM recruiter_accounts WHERE email = ?";
        try (Connection connection = databaseService.getDatabaseConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String email : testEmails) {
                statement.setString(1, email);
                statement.executeUpdate();
            }
        }
    }

    @Test
    @DisplayName("Should register a recruiter and authenticate with the stored hash")
    void testRegistrationAndLogin() throws Exception {
        String email = uniqueEmail();
        Recruiter registered = authService.register("Test Recruiter", email.toUpperCase(), "SecureTest123!", "SecureTest123!");

        assertTrue(registered.getId() > 0);
        assertEquals(email, registered.getEmail());
        assertNull(registered.getPasswordHash());

        Recruiter stored = databaseService.findRecruiterByEmail(email);
        assertNotNull(stored);
        assertNotEquals("SecureTest123!", stored.getPasswordHash());
        assertTrue(stored.getPasswordHash().startsWith("pbkdf2-sha256$"));

        Recruiter signedIn = authService.login(email.toUpperCase(), "SecureTest123!");
        assertEquals(registered.getId(), signedIn.getId());
        assertEquals("Test Recruiter", signedIn.getFullName());
        assertNull(signedIn.getPasswordHash());
    }

    @Test
    @DisplayName("Should reject incorrect recruiter passwords")
    void testIncorrectPassword() throws Exception {
        String email = uniqueEmail();
        authService.register("Password Test", email, "SecureTest123!", "SecureTest123!");

        RecruiterAuthService.AuthenticationException error = assertThrows(
            RecruiterAuthService.AuthenticationException.class,
            () -> authService.login(email, "WrongPassword123!")
        );
        assertEquals("Email or password is incorrect.", error.getMessage());
    }

    @Test
    @DisplayName("Should reject duplicate emails regardless of letter case")
    void testDuplicateEmailRegistration() throws Exception {
        String email = uniqueEmail();
        authService.register("First Recruiter", email, "SecureTest123!", "SecureTest123!");

        assertThrows(
            RecruiterAuthService.DuplicateEmailException.class,
            () -> authService.register("Second Recruiter", email.toUpperCase(), "AnotherPass123!", "AnotherPass123!")
        );
    }

    @Test
    @DisplayName("Should report invalid registration and login fields")
    void testValidation() {
        String email = uniqueEmail();
        assertThrows(
            RecruiterAuthService.ValidationException.class,
            () -> authService.register("", email, "SecureTest123!", "SecureTest123!")
        );
        assertThrows(
            RecruiterAuthService.ValidationException.class,
            () -> authService.register("Test Recruiter", "not-an-email", "SecureTest123!", "SecureTest123!")
        );
        assertThrows(
            RecruiterAuthService.ValidationException.class,
            () -> authService.register("Test Recruiter", email, "short", "short")
        );
        assertThrows(
            RecruiterAuthService.ValidationException.class,
            () -> authService.register("Test Recruiter", email, "SecureTest123!", "DifferentPass123!")
        );
        assertThrows(
            RecruiterAuthService.ValidationException.class,
            () -> authService.login("not-an-email", "SecureTest123!")
        );
    }

    @Test
    @DisplayName("Should surface database failures instead of treating them as failed credentials")
    void testDatabaseErrorHandling() {
        DatabaseConnection unavailableConnection = new DatabaseConnection(new DatabaseConfig()) {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("database unavailable");
            }
        };
        RecruiterAuthService unavailableAuth = new RecruiterAuthService(
            new DatabaseService(unavailableConnection)
        );

        SQLException error = assertThrows(
            SQLException.class,
            () -> unavailableAuth.login("recruiter@example.test", "SecureTest123!")
        );
        assertEquals("database unavailable", error.getMessage());
    }

    @Test
    @DisplayName("Should clear authenticated recruiter data on logout")
    void testLogout() {
        RecruiterSession session = new RecruiterSession();
        Recruiter recruiter = new Recruiter("test", null, "Logout Recruiter", "logout@example.test");
        session.login(recruiter);

        assertTrue(session.isAuthenticated());
        assertSame(recruiter, session.getCurrentRecruiter());

        session.logout();

        assertFalse(session.isAuthenticated());
        assertNull(session.getCurrentRecruiter());
    }

    private String uniqueEmail() {
        String email = "phase4-" + UUID.randomUUID() + "@example.test";
        testEmails.add(email);
        return email;
    }
}
