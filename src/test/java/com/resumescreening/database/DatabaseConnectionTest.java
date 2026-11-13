package com.resumescreening.database;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("Should successfully instantiate connection manager with valid configuration")
    void testConnectionManagerInstantiation() {
        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection connection = new DatabaseConnection(config);
        assertNotNull(connection.getConfig());
        assertEquals("root", connection.getConfig().getUsername());
    }

    @Test
    @DisplayName("Should test live connection without unhandled exceptions")
    void testLiveConnectionSafe() {
        DatabaseConfig config = new DatabaseConfig();
        DatabaseConnection connection = new DatabaseConnection(config);
        assertTrue(connection.testConnection(2), "Configured MySQL connection should be available.");
    }

    @Test
    @DisplayName("Should report a real connection failure for an unreachable MySQL endpoint")
    void testUnreachableDatabaseConnection() throws Exception {
        Path configFile = tempDirectory.resolve("database.properties");
        Files.writeString(configFile, "db.host=127.0.0.1\ndb.port=1\ndb.name=unreachable\n"
            + "db.user=test\ndb.password=not-used\n");
        String[] keys = {"db.host", "db.port", "db.name", "db.user", "db.password"};
        String[] originalValues = new String[keys.length];
        try {
            for (int i = 0; i < keys.length; i++) {
                originalValues[i] = System.getProperty(keys[i]);
            }
            System.setProperty("db.host", "127.0.0.1");
            System.setProperty("db.port", "1");
            System.setProperty("db.name", "unreachable");
            System.setProperty("db.user", "test");
            System.setProperty("db.password", "not-used");

            DatabaseConnection connection = new DatabaseConnection(new DatabaseConfig(configFile.toString()));
            assertFalse(connection.testConnection(1),
                "The Settings connection check must not report Connected for an unreachable server.");
        } finally {
            for (int i = 0; i < keys.length; i++) {
                if (originalValues[i] == null) {
                    System.clearProperty(keys[i]);
                } else {
                    System.setProperty(keys[i], originalValues[i]);
                }
            }
        }
    }
}
