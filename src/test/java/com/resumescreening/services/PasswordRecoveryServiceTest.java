package com.resumescreening.services;

import com.resumescreening.models.Recruiter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordRecoveryServiceTest {

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
    @DisplayName("Should reject incorrect verification codes")
    void testInvalidCode() throws Exception {
        String email = createRecruiter();
        RecordingSender sender = new RecordingSender();
        PasswordRecoveryService recovery = new PasswordRecoveryService(databaseService, sender);

        recovery.requestCode(email);

        String invalidCode = "00000000".equals(sender.code) ? "00000001" : "00000000";
        assertFalse(recovery.verifyCode(email, invalidCode));
        assertTrue(recovery.verifyCode(email, sender.code));
    }

    @Test
    @DisplayName("Should reject codes after the ten-minute expiry")
    void testExpiredCode() throws Exception {
        String email = createRecruiter();
        RecordingSender sender = new RecordingSender();
        MutableClock clock = new MutableClock(Instant.parse("2026-11-13T09:00:00Z"));
        PasswordRecoveryService recovery = new PasswordRecoveryService(databaseService, sender, clock);

        recovery.requestCode(email);
        clock.advance(Duration.ofMinutes(10));

        assertFalse(recovery.verifyCode(email, sender.code));
    }

    @Test
    @DisplayName("Should allow a recovery code to verify and reset a password only once")
    void testSuccessfulResetAndCodeReuse() throws Exception {
        String email = createRecruiter();
        RecordingSender sender = new RecordingSender();
        PasswordRecoveryService recovery = new PasswordRecoveryService(databaseService, sender);

        recovery.requestCode(email);
        assertTrue(recovery.verifyCode(email, sender.code));
        assertFalse(recovery.verifyCode(email, sender.code));

        recovery.resetPassword(email, "UpdatedSecure123!", "UpdatedSecure123!");
        assertThrows(
            RecruiterAuthService.AuthenticationException.class,
            () -> authService.login(email, "InitialSecure123!")
        );
        Recruiter signedIn = authService.login(email, "UpdatedSecure123!");
        assertEquals("Recovery Test Recruiter", signedIn.getFullName());
        assertFalse(recovery.verifyCode(email, sender.code));
    }

    @Test
    @DisplayName("Should keep a verified code available when confirmation does not match")
    void testIncorrectPasswordConfirmation() throws Exception {
        String email = createRecruiter();
        RecordingSender sender = new RecordingSender();
        PasswordRecoveryService recovery = new PasswordRecoveryService(databaseService, sender);

        recovery.requestCode(email);
        assertTrue(recovery.verifyCode(email, sender.code));
        assertThrows(
            PasswordRecoveryService.RecoveryValidationException.class,
            () -> recovery.resetPassword(email, "UpdatedSecure123!", "DifferentSecure123!")
        );

        assertThrows(
            RecruiterAuthService.AuthenticationException.class,
            () -> authService.login(email, "UpdatedSecure123!")
        );
        recovery.resetPassword(email, "UpdatedSecure123!", "UpdatedSecure123!");
        assertNotNull(authService.login(email, "UpdatedSecure123!"));
    }

    @Test
    @DisplayName("Should require SMTP configuration and avoid exposing account existence")
    void testConfigurationAndUnknownEmailHandling() throws Exception {
        String email = createRecruiter();
        PasswordRecoveryService unavailable = new PasswordRecoveryService(
            databaseService,
            new RecordingSender(false)
        );
        RecoveryCodeSender.EmailConfigurationException error = assertThrows(
            RecoveryCodeSender.EmailConfigurationException.class,
            () -> unavailable.requestCode(email)
        );
        assertTrue(error.getMessage().contains("SMTP_HOST"));

        RecordingSender sender = new RecordingSender();
        PasswordRecoveryService recovery = new PasswordRecoveryService(databaseService, sender);
        recovery.requestCode("unknown-" + UUID.randomUUID() + "@example.test");
        assertNull(sender.code);

        SmtpRecoveryCodeSender smtpSender = new SmtpRecoveryCodeSender(Map.of());
        assertFalse(smtpSender.isConfigured());
    }

    private String createRecruiter() throws Exception {
        String email = "recovery-" + UUID.randomUUID() + "@example.test";
        testEmails.add(email);
        authService.register(
            "Recovery Test Recruiter",
            email,
            "InitialSecure123!",
            "InitialSecure123!"
        );
        return email;
    }

    private static class RecordingSender implements RecoveryCodeSender {
        private final boolean configured;
        private String code;

        private RecordingSender() {
            this(true);
        }

        private RecordingSender(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public void sendVerificationCode(String email, String code) {
            this.code = code;
        }
    }

    private static class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
