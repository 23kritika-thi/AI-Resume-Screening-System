package com.resumescreening;

import com.resumescreening.database.DatabaseConfig;
import com.resumescreening.database.DatabaseConnection;
import com.resumescreening.services.DatabaseService;
import com.resumescreening.ui.MainFrame;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application bootstrap for the AI-Based Resume Screening and Candidate Ranking System.
 * Launches the modern Swing Desktop GUI by default, or runs diagnostics when --cli is specified.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        boolean cliMode = false;
        boolean setupMode = false;
        for (String arg : args) {
            if ("--setup-db".equalsIgnoreCase(arg)) {
                setupMode = true;
            }
            if ("--cli".equalsIgnoreCase(arg) || "--diagnostics".equalsIgnoreCase(arg) || "--test-db".equalsIgnoreCase(arg)) {
                cliMode = true;
                break;
            }
        }

        if (setupMode) {
            runDatabaseSetup();
            return;
        }
        if (cliMode || GraphicsEnvironment.isHeadless()) {
            runDiagnostics();
        } else {
            logger.info("Starting AI Resume Screening desktop user interface...");
            MainFrame.launchApp();
        }
    }

    private static void runDatabaseSetup() {
        DatabaseService service = new DatabaseService();
        boolean initialized = service.setupDatabase();
        System.out.println(initialized
            ? "Database schema setup and migrations completed."
            : "Database schema setup failed. Review the application logs and database permissions.");
        if (!initialized) {
            System.exit(1);
        }
    }

    public static void runDiagnostics() {
        System.out.println("===============================================================================");
        System.out.println(" AI-Based Resume Screening and Candidate Ranking System - System Diagnostics  ");
        System.out.println("===============================================================================");

        DatabaseConfig config = new DatabaseConfig();
        System.out.println("Configuration Summary: " + config);
        System.out.println("JDBC Target URL: " + config.getJdbcUrl());

        DatabaseConnection connection = new DatabaseConnection(config);
        DatabaseService service = new DatabaseService(connection);

        System.out.println("\nTesting database connection...");
        boolean connected = service.isConnected();
        System.out.println("Database Connection Status: " + (connected ? "CONNECTED (SUCCESS)" : "NOT CONNECTED (FAILED)"));

        if (connected) {
            System.out.println("\nTable Status Verification:");
            Map<String, Boolean> status = service.getTableStatus();
            for (Map.Entry<String, Boolean> entry : status.entrySet()) {
                System.out.printf("  - %-20s: %s%n", entry.getKey(), entry.getValue() ? "EXISTS (OK)" : "MISSING");
            }
        } else {
            System.out.println("\nNote: Verify the DB_* environment variables or project-root database.properties.");
        }
        System.out.println("===============================================================================");

        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception e) {
            logger.warn("MySQL driver cleanup did not complete during diagnostics.", e);
        }
    }
}
