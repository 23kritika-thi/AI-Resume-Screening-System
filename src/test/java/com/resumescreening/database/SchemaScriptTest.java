package com.resumescreening.database;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaScriptTest {

    @Test
    @DisplayName("Should parse all statements from schema.sql")
    void testLoadSchemaStatements() {
        List<String> statements = DatabaseInitializer.loadSchemaStatements("schema.sql");
        assertNotNull(statements, "Statements list should not be null");
        assertFalse(statements.isEmpty(), "Schema statements should not be empty");

        // Verify statements define all 6 required tables
        String combined = String.join(" ", statements).toLowerCase();
        for (String table : DatabaseInitializer.REQUIRED_TABLES) {
            assertTrue(
                combined.contains("create table if not exists " + table) || combined.contains("create table " + table),
                "Schema should contain table creation statement for: " + table
            );
        }
        assertTrue(combined.contains("preferred_skills"));
        assertTrue(combined.contains("required_skills_score"));
        assertTrue(combined.contains("preferred_skills_score"));
        assertTrue(combined.contains("experience_score"));
        assertTrue(combined.contains("text_similarity_score"));
    }
}
