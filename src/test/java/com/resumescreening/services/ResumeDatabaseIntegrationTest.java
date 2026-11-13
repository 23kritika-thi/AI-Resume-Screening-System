package com.resumescreening.services;

import com.resumescreening.models.Candidate;
import com.resumescreening.models.Job;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ScreeningEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResumeDatabaseIntegrationTest {

    @Test
    @DisplayName("Persists a candidate resume, job, and weighted screening result through MySQL")
    void persistsResumeAndScreening() throws Exception {
        DatabaseService database = new DatabaseService();
        assertTrue(database.setupDatabase(), "Database schema and Phase 5 migrations must be available.");
        RecruiterAuthService auth = new RecruiterAuthService(database);
        String suffix = UUID.randomUUID().toString();
        String recruiterEmail = "phase5-" + suffix + "@example.test";
        String candidateEmail = "candidate-" + suffix + "@example.test";
        String sharedCandidateEmail = "shared-candidate-" + suffix + "@example.test";
        String fileHash = UUID.randomUUID().toString().replace("-", "").repeat(2);
        int recruiterId = 0;
        try {
            var recruiter = auth.register(
                "Phase Five Integration", recruiterEmail, "IntegrationPass123!", "IntegrationPass123!"
            );
            recruiterId = recruiter.getId();
            String updatedName = auth.updateFullName(recruiterId, "Updated Integration Recruiter");
            assertEquals("Updated Integration Recruiter", updatedName);
            assertEquals(updatedName, database.findRecruiterByEmail(recruiterEmail).getFullName());
            Job job = new Job(
                recruiterId, "Integration Engineer", "Engineering",
                "Build Java backend services with Spring Boot and SQL.",
                "Java, Spring Boot", 2
            );
            job.setPreferredSkills("Docker");
            database.createJob(job);

            String extractedText = "Integration Candidate\nEmail: " + candidateEmail.toUpperCase()
                + "\nJava Spring Boot Docker SQL backend services with 3 years of professional experience";
            String extractedEmail = new ResumeProfileExtractor().extract(extractedText).email();
            Candidate candidate = new Candidate("Integration Candidate", extractedEmail, "+1 415 555 0199", null);
            Resume resume = new Resume(
                0, 0, "integration-resume.pdf", "storage/resumes/integration-test.pdf",
                extractedText,
                fileHash, null
            );
            database.saveResume(candidate, resume);
            int candidateId = resume.getCandidateId();

            assertTrue(database.resumeHashExists(fileHash));
            assertTrue(database.getCandidates().stream()
                .anyMatch(savedCandidate -> candidateEmail.equalsIgnoreCase(savedCandidate.getEmail())));
            assertTrue(database.getResumes().stream()
                .anyMatch(entry -> entry.resume().getId() == resume.getId()
                    && candidateEmail.equalsIgnoreCase(entry.candidate().getEmail())));

            List<ScreeningEntry> returned = new ResumeScreeningService(database).screen(recruiterId, job.getId());
            assertTrue(returned.stream().anyMatch(entry -> entry.resume().getId() == resume.getId()));
            ScreeningEntry saved = database.getLatestScreeningResults(job.getId()).stream()
                .filter(entry -> entry.resume().getId() == resume.getId())
                .findFirst()
                .orElseThrow();
            var report = database.getReportsSnapshot(recruiterId);
            assertTrue(report.jobs().stream().anyMatch(jobReport ->
                jobReport.job().getId() == job.getId()),
                () -> "Expected recruiter job " + job.getId() + " in report; found "
                    + report.jobs().stream().map(jobReport -> jobReport.job().getId()).toList());
            assertTrue(report.jobs().stream()
                .filter(jobReport -> jobReport.job().getId() == job.getId())
                .findFirst().orElseThrow().applicantCount() >= 1);
            assertTrue(report.screenings().stream().anyMatch(entry ->
                entry.resume().getId() == resume.getId()));
            assertTrue(saved.result().getMatchScore().doubleValue() > 0);
            assertNotNull(saved.result().getRequiredSkillsScore());
            assertNotNull(saved.result().getPreferredSkillsScore());
            assertNotNull(saved.result().getExperienceScore());
            assertNotNull(saved.result().getTextSimilarityScore());

            Resume sharedResume = new Resume(
                0, 0, "shared-resume.pdf", resume.getFilePath(), extractedText,
                UUID.randomUUID().toString().replace("-", "").repeat(2), null
            );
            database.saveResume(
                new Candidate("Shared Candidate", sharedCandidateEmail, null, null), sharedResume
            );

            assertTrue(database.deleteCandidate(candidateId).isEmpty(),
                "The resume file is still referenced by the second candidate and must be retained.");
            assertTrue(database.getResumes().stream()
                .anyMatch(entry -> entry.resume().getId() == sharedResume.getId()));

            assertEquals(List.of(resume.getFilePath()), database.deleteCandidate(sharedResume.getCandidateId()));
            assertFalse(database.getCandidates().stream().anyMatch(savedCandidate ->
                candidateEmail.equalsIgnoreCase(savedCandidate.getEmail())
                    || sharedCandidateEmail.equalsIgnoreCase(savedCandidate.getEmail())));
            assertFalse(database.getResumes().stream().anyMatch(entry ->
                entry.resume().getId() == resume.getId() || entry.resume().getId() == sharedResume.getId()));
            assertFalse(database.getLatestScreeningResults(job.getId()).stream()
                .anyMatch(entry -> entry.candidate().getId() == candidateId));
        } finally {
            try (Connection connection = database.getDatabaseConnection().getConnection()) {
                for (String email : List.of(candidateEmail, sharedCandidateEmail)) {
                    try (PreparedStatement deleteCandidate = connection.prepareStatement(
                        "DELETE FROM candidates WHERE email = ?"
                    )) {
                        deleteCandidate.setString(1, email);
                        deleteCandidate.executeUpdate();
                    }
                }
                if (recruiterId > 0) {
                    try (PreparedStatement deleteRecruiter = connection.prepareStatement(
                        "DELETE FROM recruiter_accounts WHERE id = ?"
                    )) {
                        deleteRecruiter.setInt(1, recruiterId);
                        deleteRecruiter.executeUpdate();
                    }
                }
            }
        }
    }
}
