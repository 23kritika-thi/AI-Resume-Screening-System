package com.resumescreening.services;

import com.resumescreening.models.Candidate;
import com.resumescreening.models.DashboardMetrics;
import com.resumescreening.models.Job;
import com.resumescreening.models.JobAnalytics;
import com.resumescreening.models.ReportsSnapshot;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.models.ScreeningResult;
import com.resumescreening.ui.theme.UITheme;
import com.resumescreening.ui.views.ReportsAnalyticsView;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ReportsAnalyticsTest {

    @TempDir
    Path tempDirectory;

    @BeforeAll
    static void initializeTheme() {
        UITheme.setupTheme();
    }

    @Test
    @DisplayName("Report view shows database-backed job, screening, and chart values")
    void reportViewDisplaysSnapshotAndEmptyState() {
        ReportsAnalyticsView view = new ReportsAnalyticsView();
        ReportsSnapshot snapshot = sampleSnapshot();

        assertEquals("Load Demo Data", view.getLoadDemoButton().getText());
        assertEquals("Remove Demo Data", view.getRemoveDemoButton().getText());
        view.setReports(snapshot);

        assertEquals(List.of("1", "1", "1", "1"), view.getMetricValues());
        assertEquals(1, view.getJobsTable().getRowCount());
        assertEquals("87.50", view.getJobsTable().getValueAt(0, 4));
        assertEquals(1, view.getScreeningsTable().getRowCount());
        assertEquals(1, view.getApplicantsChart().getValues().get("Backend, Engineer (#12)"));
        assertEquals(1, view.getScoresChart().getValues().get("80-100"));
        assertTrue(view.getStatusMessage().contains("loaded"));

        view.setReports(new ReportsSnapshot(new DashboardMetrics(0, 0, 0, 0), List.of()));
        assertEquals(List.of("0", "0", "0", "0"), view.getMetricValues());
        assertEquals(0, view.getJobsTable().getRowCount());
        assertTrue(view.getStatusMessage().contains("No jobs"));
        assertTrue(view.getApplicantsChart().getValues().isEmpty());

        view.setLoading(true);
        assertFalse(view.getLoadDemoButton().isEnabled());
        assertFalse(view.getRemoveDemoButton().isEnabled());
        view.setLoading(false);
        assertTrue(view.getLoadDemoButton().isEnabled());
        assertTrue(view.getRemoveDemoButton().isEnabled());
    }

    @Test
    @DisplayName("CSV export includes summary, screening detail, and escaped values")
    void csvExportHasSummaryAndEscapedData() throws Exception {
        Path destination = tempDirectory.resolve("report.csv");
        new ReportExportService().exportCsv(destination, sampleSnapshot());

        List<String> rows = Files.readAllLines(destination);
        assertEquals(4, rows.size());
        assertTrue(rows.get(1).startsWith("\"SUMMARY\",\"1\",\"1\",\"1\",\"1\""));
        assertTrue(rows.get(2).contains("\"Backend, Engineer\""));
        assertTrue(rows.get(3).contains("\"Doe, Jane \"\"J\"\"\""));
        assertTrue(rows.get(3).contains("\"87.5\""));
        assertTrue(rows.stream().allMatch(row -> csvColumnCount(row) == 21));
    }

    @Test
    @DisplayName("CSV export neutralizes formula-like recruiter and candidate data")
    void csvExportNeutralizesSpreadsheetFormulas() throws Exception {
        Path destination = tempDirectory.resolve("formula-report.csv");
        ReportsSnapshot snapshot = sampleSnapshot();
        snapshot.jobs().get(0).screenings().get(0).candidate().setFullName("=1+1");

        new ReportExportService().exportCsv(destination, snapshot);

        String screeningRow = Files.readAllLines(destination).get(3);
        assertTrue(screeningRow.contains("\"'=1+1\""));
        assertFalse(screeningRow.contains("\",\"=1+1\""));
    }

    @Test
    @DisplayName("PDF export creates a readable report with summaries and ranking")
    void pdfExportCreatesReadableReport() throws Exception {
        Path destination = tempDirectory.resolve("report.pdf");
        new ReportExportService().exportPdf(destination, sampleSnapshot());

        try (var document = Loader.loadPDF(destination.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertEquals(1, document.getNumberOfPages());
            assertTrue(text.contains("Total jobs: 1"));
            assertTrue(text.contains("Screened candidates: 1"));
            assertTrue(text.contains("Backend, Engineer"));
            assertTrue(text.contains("Doe, Jane"));
            assertTrue(text.contains("87.5/100"));
        }
    }

    @Test
    @DisplayName("Export rejects a destination with the wrong extension")
    void exportRejectsWrongExtension() {
        assertThrows(IllegalArgumentException.class,
            () -> new ReportExportService().exportCsv(tempDirectory.resolve("report.txt"), sampleSnapshot()));
    }

    private ReportsSnapshot sampleSnapshot() {
        Job job = new Job(12, 4, "Backend, Engineer", "Engineering", "Build services",
            "Java, SQL", 2, "OPEN", null, null);
        Candidate candidate = new Candidate(31, "Doe, Jane \"J\"", "jane@example.test", "555-0123",
            null, LocalDateTime.of(2026, 1, 1, 0, 0));
        Resume resume = new Resume(45, 31, "jane.pdf", "storage/jane.pdf", "Java SQL", "hash", null);
        ScreeningResult result = new ScreeningResult();
        result.setJobId(12);
        result.setCandidateId(31);
        result.setResumeId(45);
        result.setMatchScore(new BigDecimal("87.50"));
        result.setRequiredSkillsScore(new BigDecimal("45"));
        result.setPreferredSkillsScore(new BigDecimal("18"));
        result.setExperienceScore(new BigDecimal("12"));
        result.setTextSimilarityScore(new BigDecimal("12.5"));
        result.setMatchedSkills("Java, SQL");
        result.setMissingSkills("");
        result.setRanking(1);
        result.setScreenedAt(LocalDateTime.of(2026, 2, 1, 10, 30));
        ScreeningEntry entry = new ScreeningEntry(candidate, resume, result);
        return new ReportsSnapshot(new DashboardMetrics(1, 1, 1, 1),
            List.of(new JobAnalytics(job, List.of(entry))));
    }

    private int csvColumnCount(String row) {
        int columns = 1;
        boolean quoted = false;
        for (int i = 0; i < row.length(); i++) {
            char current = row.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                columns++;
            }
        }
        return columns;
    }
}
