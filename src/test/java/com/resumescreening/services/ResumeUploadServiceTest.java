package com.resumescreening.services;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import com.resumescreening.models.Candidate;
import com.resumescreening.models.Resume;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ResumeUploadServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Stores validated resumes and extracted text")
    void storesValidResume() throws Exception {
        Path source = createPdf("valid.pdf");
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(false);
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        ResumeUploadService.UploadResult result = service.upload(
            source, new Candidate("Jane Doe", null, null, null)
        );

        assertTrue(Files.exists(Path.of(result.entry().resume().getFilePath())));
        assertEquals(64, result.entry().resume().getFileHash().length());
        assertTrue(result.entry().resume().getExtractedText().contains("jane.upload@example.test"));
        assertEquals("jane.upload@example.test", result.entry().candidate().getEmail());
    }

    @Test
    @DisplayName("Saves OCR text and extracted candidate details through the existing upload workflow")
    void storesScannedResume() throws Exception {
        Path source = createScannedPdf("scanned-upload.pdf");
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(false);
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        ResumeUploadService.UploadResult result = service.upload(source, new Candidate(null, null, null, null));

        assertTrue(Files.exists(Path.of(result.entry().resume().getFilePath())));
        assertEquals("jane.upload@example.test", result.entry().candidate().getEmail());
        assertEquals("+1 415 555 0134", result.entry().candidate().getPhone());
        assertTrue(result.entry().resume().getExtractedText().contains("Bachelor"));
        assertTrue(result.profile().skills().contains("Java"));
    }

    @Test
    @DisplayName("Does not persist blank scanned PDFs after OCR finds no readable text")
    void doesNotStoreBlankScannedPdf() throws Exception {
        Path source = createScannedPdf("blank-scanned-upload.pdf", new String[0]);
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(false);
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        PdfResumeParser.ResumeValidationException error = assertThrows(
            PdfResumeParser.ResumeValidationException.class,
            () -> service.upload(source, new Candidate("Blank Scan", null, null, null))
        );

        assertEquals(PdfResumeParser.ResumeValidationException.Reason.NO_TEXT, error.getReason());
        assertFalse(Files.exists(storage));
        assertEquals(0, database.hashChecks);
        assertEquals(0, database.saves);
    }

    @Test
    @DisplayName("Does not create resume storage for unsupported or disguised files")
    void rejectedFilesAreNotStored() throws Exception {
        Path jpg = temporaryDirectory.resolve("photo.jpg");
        Files.write(jpg, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        Path renamed = temporaryDirectory.resolve("renamed-image.pdf");
        Files.write(renamed, new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(false);
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        for (Path invalid : new Path[]{jpg, renamed}) {
            assertThrows(PdfResumeParser.ResumeValidationException.class,
                () -> service.upload(invalid, new Candidate("Jane Doe", null, null, null)));
        }

        assertFalse(Files.exists(storage));
        assertEquals(0, database.hashChecks);
        assertEquals(0, database.saves);
    }

    @Test
    @DisplayName("Does not create candidate or storage records for a project synopsis PDF")
    void synopsisIsRejectedBeforePersistence() throws Exception {
        Path synopsis = temporaryDirectory.resolve("project.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 750);
                stream.showText("Project Synopsis Smart city transportation system using Java and Spring Boot architecture");
                stream.endText();
            }
            document.save(synopsis.toFile());
        }
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(false);
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        PdfResumeParser.ResumeValidationException error = assertThrows(
            PdfResumeParser.ResumeValidationException.class,
            () -> service.upload(synopsis, new Candidate("Project Synopsis", null, null, null))
        );

        assertEquals(PdfResumeParser.ResumeValidationException.Reason.NOT_A_RESUME, error.getReason());
        assertFalse(Files.exists(storage));
        assertEquals(0, database.hashChecks);
        assertEquals(0, database.saves);
    }

    @Test
    @DisplayName("Does not write duplicate files to storage")
    void duplicateDoesNotWriteFile() throws Exception {
        Path source = createPdf("duplicate.pdf");
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(true);
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        assertThrows(ResumeUploadService.DuplicateResumeException.class,
            () -> service.upload(source, new Candidate("Jane Doe", null, null, null)));

        assertFalse(Files.exists(storage));
        assertEquals(0, database.saves);
    }

    @Test
    @DisplayName("Removes the stored file when database persistence fails")
    void removesFileAfterDatabaseFailure() throws Exception {
        Path source = createPdf("database-failure.pdf");
        Path storage = temporaryDirectory.resolve("storage");
        StubDatabase database = new StubDatabase(false) {
            @Override
            public Resume saveResume(Candidate candidate, Resume resume) throws SQLException {
                throw new SQLException("database unavailable");
            }
        };
        ResumeUploadService service = new ResumeUploadService(
            database, new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        assertThrows(SQLException.class,
            () -> service.upload(source, new Candidate("Jane Doe", null, null, null)));

        try (var files = Files.list(storage)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    @DisplayName("Removes only orphaned files located directly in managed resume storage")
    void removesOnlyManagedOrphanFiles() throws Exception {
        Path storage = temporaryDirectory.resolve("storage");
        Files.createDirectories(storage);
        Path managedFile = Files.writeString(storage.resolve("orphan.pdf"), "resume");
        Path externalFile = Files.writeString(temporaryDirectory.resolve("external.pdf"), "resume");
        ResumeUploadService service = new ResumeUploadService(
            new StubDatabase(false), new PdfResumeParser(), new ResumeProfileExtractor(), storage
        );

        ResumeUploadService.FileCleanupResult result = service.removeOrphanedFiles(
            List.of(managedFile.toString(), externalFile.toString())
        );

        assertEquals(1, result.deleted());
        assertEquals(1, result.retained());
        assertFalse(Files.exists(managedFile));
        assertTrue(Files.exists(externalFile));
    }

    private Path createPdf(String fileName) throws IOException {
        Path target = temporaryDirectory.resolve(fileName);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 750);
                stream.showText("Jane Doe jane.upload@example.test Java backend engineer with five years of professional work");
                stream.endText();
            }
            document.save(target.toFile());
        }
        return target;
    }

    private Path createScannedPdf(String fileName) throws IOException {
        return createScannedPdf(fileName, new String[]{
            "JANE DOE", "jane.upload@example.test", "+1 415 555 0134",
            "WORK EXPERIENCE", "Java backend engineer - 5 years",
            "EDUCATION", "Bachelor of Science in Computer Science",
            "SKILLS", "Java, Spring Boot, SQL"
        });
    }

    private Path createScannedPdf(String fileName, String[] lines) throws IOException {
        Path target = temporaryDirectory.resolve(fileName);
        BufferedImage image = new BufferedImage(1275, 1650, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 38));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int y = 150;
            for (String line : lines) {
                graphics.drawString(line, 100, y);
                y += 115;
            }
        } finally {
            graphics.dispose();
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.drawImage(LosslessFactory.createFromImage(document, image), 0, 0,
                    page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }
            document.save(target.toFile());
        } finally {
            image.flush();
        }
        return target;
    }

    private static class StubDatabase extends DatabaseService {
        private final boolean duplicate;
        private int hashChecks;
        private int saves;

        StubDatabase(boolean duplicate) {
            this.duplicate = duplicate;
        }

        @Override
        public boolean resumeHashExists(String fileHash) {
            hashChecks++;
            return duplicate;
        }

        @Override
        public Resume saveResume(Candidate candidate, Resume resume) throws SQLException {
            saves++;
            candidate.setId(1);
            resume.setId(1);
            return resume;
        }
    }
}
