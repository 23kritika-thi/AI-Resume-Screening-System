package com.resumescreening.services;

import com.resumescreening.utils.PasswordHasher;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Manages short-lived, single-use recruiter password recovery challenges.
 *
 * Challenge material exists only in memory and stores a salted digest of the code.
 */
public class PasswordRecoveryService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DatabaseService databaseService;
    private final RecoveryCodeSender codeSender;
    private final Clock clock;
    private final ConcurrentMap<String, Challenge> challenges = new ConcurrentHashMap<>();

    public PasswordRecoveryService(DatabaseService databaseService, RecoveryCodeSender codeSender) {
        this(databaseService, codeSender, Clock.systemUTC());
    }

    PasswordRecoveryService(DatabaseService databaseService, RecoveryCodeSender codeSender, Clock clock) {
        this.databaseService = databaseService;
        this.codeSender = codeSender;
        this.clock = clock;
    }

    /**
     * Requests recovery without returning account-existence information.
     * Callers should show the same confirmation for registered and unregistered addresses.
     */
    public void requestCode(String email)
            throws SQLException, RecoveryValidationException, RecoveryCodeSender.EmailConfigurationException,
                    RecoveryCodeSender.EmailDeliveryException {
            String normalizedEmail = normalizeEmail(email);
            codeSender.validateConfiguration();

        challenges.remove(normalizedEmail);
        if (!databaseService.recruiterEmailExists(normalizedEmail)) {
            return;
        }

        String code = generateCode();
        Challenge challenge = new Challenge(code, clock.instant().plus(CODE_LIFETIME));
        codeSender.sendVerificationCode(normalizedEmail, code);
        challenges.put(normalizedEmail, challenge);
    }

    public boolean verifyCode(String email, String code) throws RecoveryValidationException {
        String normalizedEmail = normalizeEmail(email);
        if (code == null || !code.matches("\\d{" + CODE_LENGTH + "}")) {
            return false;
        }
        Challenge challenge = challenges.get(normalizedEmail);
        if (challenge == null) {
            return false;
        }

        synchronized (challenge) {
            if (challenges.get(normalizedEmail) != challenge) {
                return false;
            }
            if (isExpired(challenge)) {
                challenges.remove(normalizedEmail, challenge);
                return false;
            }
            if (challenge.verified) {
                return false;
            }
            if (!challenge.matches(code)) {
                challenge.failedAttempts++;
                if (challenge.failedAttempts >= MAX_ATTEMPTS) {
                    challenges.remove(normalizedEmail, challenge);
                }
                return false;
            }
            challenge.verified = true;
            return true;
        }
    }

    public void resetPassword(String email, String newPassword, String confirmation)
            throws SQLException, RecoveryValidationException {
        String normalizedEmail = normalizeEmail(email);
        validateNewPassword(newPassword);
        if (confirmation == null || !newPassword.equals(confirmation)) {
            throw new RecoveryValidationException("New passwords do not match.");
        }

        Challenge challenge = challenges.get(normalizedEmail);
        if (challenge == null) {
            throw new RecoveryValidationException("Verify a valid recovery code before changing your password.");
        }

        synchronized (challenge) {
            if (challenges.get(normalizedEmail) != challenge || !challenge.verified || isExpired(challenge)) {
                challenges.remove(normalizedEmail, challenge);
                throw new RecoveryValidationException(
                    "Verify a valid, unexpired recovery code before changing your password."
                );
            }
            databaseService.updateRecruiterPasswordHash(
                normalizedEmail,
                PasswordHasher.hashPassword(newPassword)
            );
            challenges.remove(normalizedEmail, challenge);
        }
    }

    private boolean isExpired(Challenge challenge) {
        return !clock.instant().isBefore(challenge.expiresAt);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RecoveryValidationException("Enter your email address.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new RecoveryValidationException("Enter a valid email address.");
        }
        return normalized;
    }

    private static void validateNewPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new RecoveryValidationException("Enter a new password.");
        }
        if (password.length() < 8 || password.length() > MAX_PASSWORD_LENGTH) {
            throw new RecoveryValidationException("New password must be between 8 and 128 characters.");
        }
    }

    private static String generateCode() {
        int value = SECURE_RANDOM.nextInt(100_000_000);
        return String.format(Locale.ROOT, "%08d", value);
    }

    public static class RecoveryValidationException extends IllegalArgumentException {
        public RecoveryValidationException(String message) {
            super(message);
        }
    }

    private static final class Challenge {
        private final String codeHash;
        private final Instant expiresAt;
        private int failedAttempts;
        private boolean verified;

        private Challenge(String code, Instant expiresAt) {
            codeHash = PasswordHasher.hashPassword(code);
            this.expiresAt = expiresAt;
        }

        private boolean matches(String code) {
            return PasswordHasher.verifyPassword(code, codeHash);
        }
    }
}
