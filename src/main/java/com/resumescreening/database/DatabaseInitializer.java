package com.resumescreening.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes database schemas, runs DDL scripts, and verifies required table structures.
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static final List<String> REQUIRED_TABLES = List.of(
        "recruiter_accounts",
        "jobs",
        "candidates",
        "resumes",
        "screening_results",
        "recruiter_notes"
    );

    private final DatabaseConnection databaseConnection;

    public DatabaseInitializer() {
        this(new DatabaseConnection());
    }

    public DatabaseInitializer(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Initializes the database by executing the schema.sql script from classpath.
     *
     * @return true if initialization and table verification succeeded.
     */
    public boolean initializeDatabase() {
        logger.info("Starting database initialization...");

        // Step 1: Ensure database exists by connecting to MySQL server
        try (Connection serverConn = databaseConnection.getServerConnection();
             Statement stmt = serverConn.createStatement()) {
            String dbName = databaseConnection.getConfig().getDatabaseName();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            logger.info("Database '{}' checked/created.", dbName);
        } catch (SQLException e) {
            logger.error("Failed to connect to MySQL server or create database: {}", e.getMessage());
            return false;
        }

        // Step 2: Execute schema statements on target database
        List<String> sqlStatements = loadSchemaStatements("schema.sql");
        if (sqlStatements.isEmpty()) {
            logger.error("No SQL statements found in schema.sql");
            return false;
        }

        try (Connection dbConn = databaseConnection.getConnection();
             Statement stmt = dbConn.createStatement()) {
            for (String sql : sqlStatements) {
                if (!sql.isBlank()) {
                    stmt.execute(sql);
                }
            }
            logger.info("Successfully executed schema definition scripts.");
        } catch (SQLException e) {
            logger.error("Failed to execute schema SQL statements: {}", e.getMessage());
            return false;
        }

        if (!migratePhaseFiveColumns()) {
            return false;
        }

        // Step 3: Verify all required tables exist
        return verifyTables().isEmpty();
    }

    public boolean applyPhaseFiveMigrations() {
        return migratePhaseFiveColumns();
    }

    private boolean migratePhaseFiveColumns() {
        List<ColumnMigration> migrations = List.of(
            new ColumnMigration("jobs", "preferred_skills",
                "ALTER TABLE jobs ADD COLUMN preferred_skills TEXT NULL"),
            new ColumnMigration("screening_results", "required_skills_score",
                "ALTER TABLE screening_results ADD COLUMN required_skills_score DECIMAL(5,2) NOT NULL DEFAULT 0"),
            new ColumnMigration("screening_results", "preferred_skills_score",
                "ALTER TABLE screening_results ADD COLUMN preferred_skills_score DECIMAL(5,2) NOT NULL DEFAULT 0"),
            new ColumnMigration("screening_results", "experience_score",
                "ALTER TABLE screening_results ADD COLUMN experience_score DECIMAL(5,2) NOT NULL DEFAULT 0"),
            new ColumnMigration("screening_results", "text_similarity_score",
                "ALTER TABLE screening_results ADD COLUMN text_similarity_score DECIMAL(5,2) NOT NULL DEFAULT 0")
        );
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            for (ColumnMigration migration : migrations) {
                if (!columnExists(connection, migration.table(), migration.column())) {
                    statement.executeUpdate(migration.ddl());
                    logger.info("Added database column {}.{}", migration.table(), migration.column());
                }
            }
            return true;
        } catch (SQLException e) {
            logger.error("Unable to apply Phase 5 database migrations: {}", e.getMessage());
            return false;
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet results = metadata.getColumns(
            databaseConnection.getConfig().getDatabaseName(), null, table, column
        )) {
            while (results.next()) {
                if (column.equalsIgnoreCase(results.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private record ColumnMigration(String table, String column, String ddl) {
    }

    /**
     * Verifies existence of required tables.
     *
     * @return List of missing table names, or empty list if all tables are present.
     */
    public List<String> verifyTables() {
        List<String> missingTables = new ArrayList<>();
        Set<String> existingTables = new HashSet<>();

        try (Connection conn = databaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = databaseConnection.getConfig().getDatabaseName();
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (tableName != null) {
                        existingTables.add(tableName.toLowerCase());
                    }
                }
            }

            for (String required : REQUIRED_TABLES) {
                if (!existingTables.contains(required.toLowerCase())) {
                    missingTables.add(required);
                }
            }

            if (missingTables.isEmpty()) {
                logger.info("All required tables verified: {}", existingTables);
            } else {
                logger.warn("Missing required tables: {}", missingTables);
            }
        } catch (SQLException e) {
            logger.error("Error inspecting database metadata: {}", e.getMessage());
            missingTables.addAll(REQUIRED_TABLES);
        }

        return missingTables;
    }

    /**
     * Splits schema.sql into individual executable statements.
     */
    public static List<String> loadSchemaStatements(String resourceName) {
        List<String> statements = new ArrayList<>();
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                logger.error("Schema resource '{}' not found on classpath", resourceName);
                return statements;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder current = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                        continue;
                    }
                    current.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String stmt = current.toString().trim();
                        if (stmt.endsWith(";")) {
                            stmt = stmt.substring(0, stmt.length() - 1).trim();
                        }
                        if (!stmt.isEmpty()) {
                            statements.add(stmt);
                        }
                        current.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error reading schema file {}: {}", resourceName, e.getMessage());
        }
        return statements;
    }
}
