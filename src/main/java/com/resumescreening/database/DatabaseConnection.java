package com.resumescreening.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides database connections using JDBC and DatabaseConfig.
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found on classpath: {}", e.getMessage());
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    private final DatabaseConfig config;

    public DatabaseConnection() {
        this(new DatabaseConfig());
    }

    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Gets a connection to the application database.
     */
    public Connection getConnection() throws SQLException {
        String url = config.getJdbcUrl();
        logger.debug("Connecting to MySQL: {} (user: {})", url, config.getUsername());
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }

    /**
     * Gets a connection to the MySQL server without selecting a specific database.
     */
    public Connection getServerConnection() throws SQLException {
        String url = config.getServerJdbcUrl();
        logger.debug("Connecting to MySQL server: {} (user: {})", url, config.getUsername());
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }

    /**
     * Validates if a live connection can be established.
     */
    public boolean testConnection(int timeoutSeconds) {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(timeoutSeconds);
        } catch (SQLException e) {
            logger.warn("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
