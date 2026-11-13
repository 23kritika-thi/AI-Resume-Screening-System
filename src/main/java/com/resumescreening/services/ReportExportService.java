package com.resumescreening.services;

import com.resumescreening.models.JobAnalytics;
import com.resumescreening.models.ReportsSnapshot;
import com.resumescreening.models.ScreeningEntry;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Exports recruiter-scoped database report snapshots to CSV or PDF. */
public class ReportExportService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void exportCsv(Path destination, ReportsSnapshot snapshot) throws IOException {
        requireExport(destination, snapshot, ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writeCsvRow(writer, List.of(
                "record_type", "total_jobs", "total_candidates", "total_resumes", "screened_candidates",
                "job_id", "job_title", "department", "status", "applicant_count",
                "average_score", "rank", "candidate_name", "match_score", "required_skills_score",
                "preferred_skills_score", "experience_score", "text_similarity_score",
                "matched_skills", "missing_skills", "screened_at"
            ));
            writeCsvRow(writer, List.of(
                "SUMMARY", Integer.toString(snapshot.metrics().jobs()),
                Integer.toString(snapshot.metrics().candidates()),
                Integer.toString(snapshot.metrics().resumes()),
                Integer.toString(snapshot.screenedCandidateCount()),
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""
            ));
            for (JobAnalytics job : snapshot.jobs()) {
                writeCsvRow(writer, List.of(
                    "JOB", "", "", "", "", Integer.toString(job.job().getId()),
                    safe(job.job().getTitle()), safe(job.job().getDepartment()), safe(job.job().getStatus()),
                    Integer.toString(job.applicantCount()), job.averageScore().toPlainString(),
                    "", "", "", "", "", "", "", "", "", ""
                ));
                for (ScreeningEntry entry : job.screenings()) {
                    var result = entry.result();
                    writeCsvRow(writer, List.of(
                        "SCREENING", "", "", "", "", Integer.toString(job.job().getId()),
                        safe(job.job().getTitle()), safe(job.job().getDepartment()), safe(job.job().getStatus()),
                        Integer.toString(job.applicantCount()), job.averageScore().toPlainString(),
                        value(result.getRanking()), safe(entry.candidate().getFullName()),
                        decimal(result.getMatchScore()), decimal(result.getRequiredSkillsScore()),
                        decimal(result.getPreferredSkillsScore()), decimal(result.getExperienceScore()),
                        decimal(result.getTextSimilarityScore()), safe(result.getMatchedSkills()),
                        safe(result.getMissingSkills()), value(result.getScreenedAt())
                    ));
                }
            }
        }
    }

    public void exportPdf(Path destination, ReportsSnapshot snapshot) throws IOException {
        requireExport(destination, snapshot, ".pdf");
        try (PDDocument document = new PDDocument()) {
            PdfWriter writer = new PdfWriter(document);
            writer.write("Reports and Analytics");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP));
            writer.write("");
            writer.write("Total jobs: " + snapshot.metrics().jobs());
            writer.write("Total candidates: " + snapshot.metrics().candidates());
            writer.write("Uploaded resumes: " + snapshot.metrics().resumes());
            writer.write("Screened candidates: " + snapshot.screenedCandidateCount());
            writer.write("");
            writer.write("Job-wise applicant summary");
            for (JobAnalytics job : snapshot.jobs()) {
                writer.write(job.job().getTitle() + " | Applicants: " + job.applicantCount()
                    + " | Average score: " + job.averageScore().toPlainString()
                    + " | Status: " + safe(job.job().getStatus()));
            }
            writer.write("");
            writer.write("Screening rankings (scores out of 100)");
            for (JobAnalytics job : snapshot.jobs()) {
                writer.write(job.job().getTitle() + " - " + job.applicantCount() + " applicant(s)");
                for (ScreeningEntry entry : job.screenings()) {
                    var result = entry.result();
                    writer.write("  #" + value(result.getRanking()) + " "
                        + safe(entry.candidate().getFullName()) + " | "
                        + decimal(result.getMatchScore()) + "/100"
                        + " (Required " + decimal(result.getRequiredSkillsScore())
                        + ", Preferred " + decimal(result.getPreferredSkillsScore())
                        + ", Experience " + decimal(result.getExperienceScore())
                        + ", Text " + decimal(result.getTextSimilarityScore()) + ")");
                    if (result.getMatchedSkills() != null && !result.getMatchedSkills().isBlank()) {
                        writer.write("    Matched: " + result.getMatchedSkills());
                    }
                    if (result.getMissingSkills() != null && !result.getMissingSkills().isBlank()) {
                        writer.write("    Missing: " + result.getMissingSkills());
                    }
                }
            }
            writer.close();
            document.save(destination.toFile());
        }
    }

    private void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        List<String> escaped = new ArrayList<>(values.size());
        for (String value : values) {
            String safeValue = safeCsvValue(value);
            escaped.add("\"" + safeValue.replace("\"", "\"\"") + "\"");
        }
        writer.write(String.join(",", escaped));
        writer.newLine();
    }

    private void requireExport(Path destination, ReportsSnapshot snapshot, String extension) {
        if (destination == null || destination.getFileName() == null
            || !destination.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            throw new IllegalArgumentException("Choose a destination file ending in " + extension + ".");
        }
        if (snapshot == null || snapshot.metrics() == null) {
            throw new IllegalArgumentException("Reports data is not available to export.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeCsvValue(String value) {
        String safeValue = value == null ? "" : value;
        int firstMeaningful = 0;
        while (firstMeaningful < safeValue.length()
            && Character.isWhitespace(safeValue.charAt(firstMeaningful))) {
            firstMeaningful++;
        }
        if (firstMeaningful < safeValue.length()
            && "=+-@".indexOf(safeValue.charAt(firstMeaningful)) >= 0) {
            return "'" + safeValue;
        }
        if (!safeValue.isEmpty() && "\t\r\n".indexOf(safeValue.charAt(0)) >= 0) {
            return "'" + safeValue;
        }
        return safeValue;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String pdfSafe(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
        return decomposed.replaceAll("\\p{M}+", "")
            .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static final class PdfWriter {
        private final PDDocument document;
        private final PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private PDPageContentStream stream;
        private float y;

        private PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void write(String rawText) throws IOException {
            String text = pdfSafe(rawText);
            for (String line : wrap(text, 105)) {
                if (y < 48) {
                    stream.close();
                    newPage();
                }
                stream.beginText();
                stream.setFont(font, 9);
                stream.newLineAtOffset(45, y);
                stream.showText(line);
                stream.endText();
                y -= 14;
            }
        }

        private void close() throws IOException {
            stream.close();
        }

        private void newPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 48;
        }

        private List<String> wrap(String text, int limit) {
            List<String> lines = new ArrayList<>();
            String remaining = text;
            while (remaining.length() > limit) {
                int split = remaining.lastIndexOf(' ', limit);
                if (split <= 0) {
                    split = limit;
                }
                lines.add(remaining.substring(0, split));
                remaining = remaining.substring(split).stripLeading();
            }
            lines.add(remaining);
            return lines;
        }
    }
}
