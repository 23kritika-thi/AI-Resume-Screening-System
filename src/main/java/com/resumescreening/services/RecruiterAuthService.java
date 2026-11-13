package com.resumescreening.services;

import com.resumescreening.models.Recruiter;
import com.resumescreening.utils.PasswordHasher;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validates recruiter credentials and coordinates account persistence.
 */
public class RecruiterAuthService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final DatabaseService databaseService;

    public RecruiterAuthService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Recruiter register(String fullName, String email, String password, String confirmation)
            throws SQLException, DuplicateEmailException {
        String normalizedName = validateName(fullName);
        String normalizedEmail = normalizeAndValidateEmail(email);
        validatePassword(password);
        if (confirmation == null || !password.equals(confirmation)) {
            throw new ValidationException("Passwords do not match.");
        }

        if (databaseService.recruiterEmailExists(normalizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        String username = "recruiter-" + UUID.randomUUID();
        Recruiter recruiter = new Recruiter(
            username,
            PasswordHasher.hashPassword(password),
            normalizedName,
            normalizedEmail
        );
        try {
            databaseService.createRecruiter(recruiter);
        } catch (SQLException e) {
            if (isConstraintViolation(e) && databaseService.recruiterEmailExists(normalizedEmail)) {
                throw new DuplicateEmailException("An account with this email already exists.", e);
            }
            throw e;
        }
        recruiter.setPasswordHash(null);
        return recruiter;
    }

    public Recruiter login(String email, String password) throws SQLException, AuthenticationException {
        String normalizedEmail = normalizeAndValidateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Enter your password.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password cannot exceed 128 characters.");
        }

        Recruiter recruiter = databaseService.findRecruiterByEmail(normalizedEmail);
        if (recruiter == null || !PasswordHasher.verifyPassword(password, recruiter.getPasswordHash())) {
            throw new AuthenticationException("Email or password is incorrect.");
        }

        if (PasswordHasher.needsUpgrade(recruiter.getPasswordHash())) {
            databaseService.updateRecruiterPasswordHash(
                recruiter.getId(),
                PasswordHasher.hashPassword(password)
            );
        }
        recruiter.setPasswordHash(null);
        return recruiter;
    }

    public String updateFullName(int recruiterId, String fullName) throws SQLException {
        if (recruiterId <= 0) {
            throw new ValidationException("A signed-in recruiter account is required.");
        }
        String normalizedName = validateName(fullName);
        databaseService.updateRecruiterFullName(recruiterId, normalizedName);
        return normalizedName;
    }

    private static String validateName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Enter your full name.");
        }
        String normalized = fullName.trim();
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw new ValidationException("Full name must be between 2 and 100 characters.");
        }
        return normalized;
    }

    private static String normalizeAndValidateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Enter your email address.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Enter a valid email address (maximum 100 characters).");
        }
        return normalized;
    }

    private static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Enter a password.");
        }
        if (password.length() < 8 || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be between 8 and 128 characters.");
        }
    }

    private static boolean isConstraintViolation(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if (current.getSQLState() != null && current.getSQLState().startsWith("23")) {
                return true;
            }
        }
        return false;
    }

    public static class ValidationException extends IllegalArgumentException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class DuplicateEmailException extends Exception {
        public DuplicateEmailException(String message) {
            super(message);
        }

        public DuplicateEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
