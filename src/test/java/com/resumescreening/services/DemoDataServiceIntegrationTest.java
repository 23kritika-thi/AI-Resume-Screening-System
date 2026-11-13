package com.resumescreening.services;

import com.resumescreening.models.Job;
import com.resumescreening.models.ScreeningEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataServiceIntegrationTest {

    @Test
    @DisplayName("Loads fictional PDFs through MySQL, screening, reports, exports, and safe repeat/removal")
    void loadsAndRemovesDemoDatasetThroughExistingWorkflow() throws Exception {
        DatabaseService database = new DatabaseService();
        assertTrue(database.setupDatabase(), "Database schema and Phase 5 migrations must be available.");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String recruiterEmail = "demo-loader-" + suffix + "@example.test";
        String namespace = "t" + suffix.substring(0, 8);
        Path storage = Files.createTempDirectory("resume-demo-integration-");
        Path exports = Files.createTempDirectory("resume-demo-exports-");
        int recruiterId = 0;
        DemoDataService demo = null;
        try {
            var recruiter = new RecruiterAuthService(database).register(
                "Demo Integration Recruiter", recruiterEmail, "IntegrationPass123!", "IntegrationPass123!"
            );
            recruiterId = recruiter.getId();
            ResumeUploadService uploads = new ResumeUploadService(
                database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
            );
            demo = new DemoDataService(database, uploads, new ResumeScreeningService(database), namespace);

            Job ordinaryJob = database.createJob(new Job(
                recruiterId, "Ordinary integration job " + suffix, "Engineering",
                "Existing records must remain untouched.", "Java", 1
            ));

            DemoDataService.LoadResult first = demo.load(recruiterId);
            assertEquals(8, first.jobsCreated());
            assertEquals(0, first.jobsReused());
            assertEquals(18, first.resumesCreated());
            assertEquals(0, first.resumesReused());
            assertEquals(32, first.screeningsCreated());
            assertEquals(0, first.screeningsReused());

            var jobs = database.getJobsForRecruiter(recruiterId);
            assertEquals(9, jobs.size());
            assertTrue(jobs.stream().anyMatch(job -> job.getId() == ordinaryJob.getId()));

            String emailSuffix = "." + namespace + "@demo.resume-screening.example";
            var demoResumes = database.getResumes().stream()
                .filter(entry -> entry.candidate().getEmail() != null
                    && entry.candidate().getEmail().endsWith(emailSuffix))
                .toList();
            assertEquals(18, demoResumes.size());
            for (var entry : demoResumes) {
                assertTrue(Files.isRegularFile(Path.of(entry.resume().getFilePath())));
                assertTrue(entry.resume().getFileName().startsWith("DEMO-"));
                assertTrue(entry.resume().getExtractedText().contains("DEMO DATASET:"));
                var profile = new ResumeProfileExtractor().extract(entry.resume().getExtractedText());
                assertEquals(entry.candidate().getFullName(), profile.candidateName());
                assertEquals(entry.candidate().getEmail(), profile.email());
                assertFalse(profile.skills().isEmpty());
                assertFalse(profile.education().isEmpty());
            }

            var report = database.getReportsSnapshot(recruiterId);
            assertEquals(8, report.jobs().stream()
                .filter(job -> job.job().getTitle().startsWith("[DEMO] ")).count());
            assertEquals(32, report.screenings().size());
            assertTrue(report.jobs().stream()
                .filter(job -> job.job().getTitle().startsWith("[DEMO] "))
                .allMatch(job -> job.applicantCount() > 0));
            assertTrue(report.screenings().stream()
                .allMatch(entry -> entry.result().getMatchScore().doubleValue() >= 0));
            Job javaDeveloper = report.jobs().stream()
                .map(job -> job.job())
                .filter(job -> job.getTitle().equals("[DEMO] Java Developer (" + namespace + ")"))
                .findFirst()
                .orElseThrow();
            List<ScreeningEntry> javaRankings = database.getLatestScreeningResults(javaDeveloper.getId());
            assertEquals(Set.of("Morgan Lee", "Taylor Reed", "Jamie Bell", "Emerson Cole"),
                javaRankings.stream().map(entry -> entry.candidate().getFullName()).collect(
                    java.util.stream.Collectors.toSet()
                ));
            for (int i = 0; i < javaRankings.size(); i++) {
                assertEquals(i + 1, javaRankings.get(i).result().getRanking());
                if (i > 0) {
                    assertTrue(javaRankings.get(i - 1).result().getMatchScore()
                        .compareTo(javaRankings.get(i).result().getMatchScore()) >= 0);
                }
            }

            Path csv = exports.resolve("demo-report.csv");
            Path pdf = exports.resolve("demo-report.pdf");
            new ReportExportService().exportCsv(csv, report);
            new ReportExportService().exportPdf(pdf, report);
            String csvText = Files.readString(csv);
            assertTrue(csvText.contains("[DEMO] Java Developer"));
            assertTrue(csvText.contains("Morgan Lee"));
            try (var document = Loader.loadPDF(pdf.toFile())) {
                String pdfText = new PDFTextStripper().getText(document);
                assertTrue(pdfText.contains("[DEMO] Java Developer"));
                assertTrue(pdfText.contains("Morgan Lee"));
            }

            DemoDataService.LoadResult repeated = demo.load(recruiterId);
            assertEquals(0, repeated.jobsCreated());
            assertEquals(8, repeated.jobsReused());
            assertEquals(0, repeated.resumesCreated());
            assertEquals(18, repeated.resumesReused());
            assertEquals(0, repeated.screeningsCreated());
            assertEquals(32, repeated.screeningsReused());
            assertEquals(32, database.getReportsSnapshot(recruiterId).screenings().size());

            DemoDataService.RemoveResult removed = demo.remove(recruiterId);
            assertEquals(8, removed.jobsDeleted());
            assertEquals(18, removed.candidatesDeleted());
            assertEquals(0, removed.candidatesRetained());
            assertEquals(18, removed.filesDeleted());
            assertEquals(0, removed.filesRetained());
            assertEquals(List.of(ordinaryJob.getId()),
                database.getJobsForRecruiter(recruiterId).stream().map(Job::getId).toList());
            assertFalse(database.getCandidates().stream().anyMatch(candidate ->
                candidate.getEmail() != null && candidate.getEmail().endsWith(emailSuffix)));
            assertEquals(0, database.getReportsSnapshot(recruiterId).screenings().size());
            try (Stream<Path> files = Files.list(storage)) {
                assertEquals(0, files.count());
            }
        } finally {
            if (demo != null && recruiterId > 0) {
                demo.remove(recruiterId);
            }
            if (recruiterId > 0) {
                try (Connection connection = database.getDatabaseConnection().getConnection();
                     PreparedStatement deleteRecruiter =
                         connection.prepareStatement("DELETE FROM recruiter_accounts WHERE id = ?")) {
                    deleteRecruiter.setInt(1, recruiterId);
                    deleteRecruiter.executeUpdate();
                }
            }
            deleteTree(storage);
            deleteTree(exports);
        }
    }

    private void deleteTree(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
