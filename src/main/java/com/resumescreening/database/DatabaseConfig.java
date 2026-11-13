package com.resumescreening.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages database configuration loaded from external or classpath properties files,
 * with optional environment variable and system property overrides.
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String DEFAULT_PROPERTIES_FILE = "database.properties";

    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private boolean useSSL;
    private boolean allowPublicKeyRetrieval;
    private String serverTimezone;
    private String characterEncoding;

    public DatabaseConfig() {
        this(DEFAULT_PROPERTIES_FILE);
    }

    public DatabaseConfig(String propertiesResource) {
        loadProperties(propertiesResource);
    }

    private void loadProperties(String propertiesResource) {
        Properties props = new Properties();

        // 1. Try to load from external file in working directory if available
        File externalFile = new File(propertiesResource);
        if (externalFile.exists() && externalFile.isFile()) {
            try (InputStream in = new FileInputStream(externalFile)) {
                props.load(in);
                logger.info("Loaded database configuration from external file: {}", externalFile.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to load external properties file {}: {}", externalFile.getAbsolutePath(), e.getMessage());
            }
        }

        // 2. Otherwise load from classpath
        if (props.isEmpty()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(propertiesResource)) {
                if (in != null) {
                    props.load(in);
                    logger.info("Loaded database configuration from classpath: {}", propertiesResource);
                } else {
                    logger.warn("Classpath resource '{}' not found, falling back to defaults/environment.", propertiesResource);
                }
            } catch (IOException e) {
                logger.warn("Failed to load properties from classpath: {}", e.getMessage());
            }
        }

        // Apply properties with environment/system property overrides
        this.host = getEnvOrProperty("DB_HOST", "db.host", props.getProperty("db.host", "localhost"));
        this.port = Integer.parseInt(getEnvOrProperty("DB_PORT", "db.port", props.getProperty("db.port", "3306")));
        this.databaseName = getEnvOrProperty("DB_NAME", "db.name", props.getProperty("db.name", "resume_screening_db"));
        this.username = getEnvOrProperty("DB_USER", "db.user", props.getProperty("db.user", "root"));
        this.password = getEnvOrProperty("DB_PASSWORD", "db.password", props.getProperty("db.password", ""));
        this.useSSL = Boolean.parseBoolean(props.getProperty("db.useSSL", "false"));
        this.allowPublicKeyRetrieval = Boolean.parseBoolean(props.getProperty("db.allowPublicKeyRetrieval", "true"));
        this.serverTimezone = props.getProperty("db.serverTimezone", "UTC");
        this.characterEncoding = props.getProperty("db.characterEncoding", "UTF-8");
    }

    private String getEnvOrProperty(String envKey, String sysPropKey, String defaultValue) {
        String sysVal = System.getProperty(sysPropKey);
        if (sysVal != null && !sysVal.isBlank()) {
            return sysVal.trim();
        }
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        return defaultValue != null ? defaultValue.trim() : "";
    }

    /**
     * Builds the full JDBC URL including the database name and query parameters.
     */
    public String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?createDatabaseIfNotExist=true&useSSL=%b&allowPublicKeyRetrieval=%b&serverTimezone=%s&characterEncoding=%s",
            host, port, databaseName, useSSL, allowPublicKeyRetrieval, serverTimezone, characterEncoding
        );
    }

    /**
     * Builds a JDBC URL connecting to the MySQL instance without a specific database selected,
     * useful for administrative tasks like initial schema creation.
     */
    public String getServerJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/?useSSL=%b&allowPublicKeyRetrieval=%b&serverTimezone=%s&characterEncoding=%s",
            host, port, useSSL, allowPublicKeyRetrieval, serverTimezone, characterEncoding
        );
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public boolean isAllowPublicKeyRetrieval() {
        return allowPublicKeyRetrieval;
    }

    public String getServerTimezone() {
        return serverTimezone;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", databaseName='" + databaseName + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" +
                ", useSSL=" + useSSL +
                ", allowPublicKeyRetrieval=" + allowPublicKeyRetrieval +
                '}';
    }
}
