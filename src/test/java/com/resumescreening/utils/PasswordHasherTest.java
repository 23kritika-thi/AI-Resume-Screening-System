package com.resumescreening.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    @DisplayName("Should hash password and successfully verify correct password")
    void testHashAndVerifySuccess() {
        String rawPassword = "RecruiterSecure2026!";
        String hash = PasswordHasher.hashPassword(rawPassword);

        assertNotNull(hash);
        assertTrue(hash.startsWith("pbkdf2-sha256$"), "Hash must identify the PBKDF2 format");
        assertTrue(PasswordHasher.verifyPassword(rawPassword, hash));
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void testVerifyWrongPassword() {
        String hash = PasswordHasher.hashPassword("CorrectPass123");
        assertFalse(PasswordHasher.verifyPassword("WrongPass123", hash));
    }

    @Test
    @DisplayName("Should produce different salts for identical passwords")
    void testUniqueSalts() {
        String p = "SamePassword";
        String hash1 = PasswordHasher.hashPassword(p);
        String hash2 = PasswordHasher.hashPassword(p);

        assertNotEquals(hash1, hash2, "Salts must ensure distinct hashes");
        assertTrue(PasswordHasher.verifyPassword(p, hash1));
        assertTrue(PasswordHasher.verifyPassword(p, hash2));
    }

    @Test
    @DisplayName("Should upgrade legacy salted SHA-256 hashes after successful verification")
    void testLegacyHashVerification() throws Exception {
        String password = "LegacyPassword123";
        byte[] salt = new byte[16];
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(
            concat(salt, password.getBytes(StandardCharsets.UTF_8))
        );
        String legacyHash = Base64.getEncoder().encodeToString(salt)
            + ":" + Base64.getEncoder().encodeToString(hash);

        assertTrue(PasswordHasher.verifyPassword(password, legacyHash));
        assertTrue(PasswordHasher.needsUpgrade(legacyHash));
        assertFalse(PasswordHasher.verifyPassword(password, "broken:hash:value"));
    }

    private byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
