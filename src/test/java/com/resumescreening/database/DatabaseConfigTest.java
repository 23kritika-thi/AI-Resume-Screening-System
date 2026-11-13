package com.resumescreening.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    @DisplayName("Should successfully load default configuration from database.properties")
    void testLoadDefaultProperties() {
        DatabaseConfig config = new DatabaseConfig();
        assertNotNull(config.getHost(), "Host should not be null");
        assertTrue(config.getPort() > 0, "Port should be positive");
        assertNotNull(config.getDatabaseName(), "Database name should not be null");
        assertNotNull(config.getUsername(), "Username should not be null");
        assertNotNull(config.getPassword(), "Password should not be null");
    }

    @Test
    @DisplayName("Should format JDBC URL with all expected query parameters")
    void testGetJdbcUrl() {
        DatabaseConfig config = new DatabaseConfig();
        String url = config.getJdbcUrl();
        assertTrue(url.startsWith("jdbc:mysql://"), "URL should use mysql scheme");
        assertTrue(url.contains(config.getHost()), "URL should contain host");
        assertTrue(url.contains(String.valueOf(config.getPort())), "URL should contain port");
        assertTrue(url.contains(config.getDatabaseName()), "URL should contain db name");
        assertTrue(url.contains("allowPublicKeyRetrieval=true"), "URL should allow public key retrieval");
    }

    @Test
    @DisplayName("Should format Server JDBC URL without specific database name")
    void testGetServerJdbcUrl() {
        DatabaseConfig config = new DatabaseConfig();
        String serverUrl = config.getServerJdbcUrl();
        assertTrue(serverUrl.startsWith("jdbc:mysql://"), "Server URL should use mysql scheme");
        assertFalse(serverUrl.contains(config.getDatabaseName() + "?"), "Server URL should not bind to db directly");
    }

    @Test
    @DisplayName("ToString should never expose plain-text password")
    void testPasswordMasking() {
        DatabaseConfig config = new DatabaseConfig();
        config.setPassword("super_secret_pwd_123");
        String stringRep = config.toString();
        assertFalse(stringRep.contains("super_secret_pwd_123"), "Password must be masked in toString()");
        assertTrue(stringRep.contains("***"), "Masking symbol should appear in toString()");
    }
}
