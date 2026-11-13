package com.resumescreening.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility class for versioned password hashing and verification.
 */
public final class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int ITERATIONS = 210_000;
    private static final int MAX_STORED_ITERATIONS = 1_000_000;
    private static final String HASH_PREFIX = "pbkdf2-sha256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Hashes a plain-text password with a freshly generated random salt.
     *
     * @param password Plain-text password.
     * @return A versioned PBKDF2 hash with a random salt.
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = deriveHash(password, salt, ITERATIONS);
        return HASH_PREFIX + "$" + ITERATIONS + "$"
            + Base64.getEncoder().encodeToString(salt) + "$"
            + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies versioned PBKDF2 hashes and the legacy salted SHA-256 format.
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null || candidatePassword.isEmpty()) {
            return false;
        }
        try {
            if (storedHash.startsWith(HASH_PREFIX + "$")) {
                String[] parts = storedHash.split("\\$", -1);
                if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
                    return false;
                }
                int iterations = Integer.parseInt(parts[1]);
                if (iterations < 1_000 || iterations > MAX_STORED_ITERATIONS) {
                    return false;
                }
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
                if (salt.length != SALT_BYTES || expectedHash.length != HASH_BITS / 8) {
                    return false;
                }
                return MessageDigest.isEqual(expectedHash, deriveHash(candidatePassword, salt, iterations));
            }

            return verifyLegacyHash(candidatePassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean needsUpgrade(String storedHash) {
        if (storedHash == null || !storedHash.startsWith(HASH_PREFIX + "$")) {
            return true;
        }
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static byte[] deriveHash(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean verifyLegacyHash(String password, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        try {
            String[] parts = storedHash.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            if (salt.length != SALT_BYTES || expectedHash.length != HASH_BITS / 8) {
                return false;
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return MessageDigest.isEqual(expectedHash, md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            return false;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
