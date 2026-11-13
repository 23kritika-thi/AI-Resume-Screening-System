package com.resumescreening.services;

import java.io.IOException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class PdfResumeParserTest {

    @TempDir
    Path temporaryDirectory;

    private final PdfResumeParser parser = new PdfResumeParser();

    @Test
    @DisplayName("Extracts meaningful text across all pages")
    void extractsMultiPageResume() throws Exception {
        Path pdf = temporaryDirectory.resolve("resume.pdf");
        createPdf(pdf,
            "Jane Doe | jane.doe@example.test | +1 415 555 0134 | Work Experience | Java backend engineer",
            "Education: Bachelor of Science | Technical Skills: Spring Boot, SQL, and cloud services");

        String extracted = parser.extractText(pdf);

        assertTrue(extracted.contains("Jane Doe"));
        assertTrue(extracted.contains("Spring Boot"));
    }

    @Test
    @DisplayName("Uses OCR for an image-only PDF resume and extracts candidate profile details")
    void extractsScannedResumeWithOcr() throws Exception {
        Path pdf = temporaryDirectory.resolve("scanned-resume.pdf");
        createScannedResumePdf(pdf);

        String extracted = parser.extractText(pdf);
        var profile = new ResumeProfileExtractor().extract(extracted);

        assertTrue(extracted.toLowerCase().contains("education"));
        assertEquals("jane.ocr@example.com", profile.email());
        assertEquals("+1 415 555 0134", profile.phone());
        assertTrue(profile.candidateName().toLowerCase().contains("jane"));
        assertTrue(profile.education().stream().anyMatch(line -> line.toLowerCase().contains("bachelor")));
        assertTrue(profile.skills().contains("Java"));
        assertTrue(profile.skills().contains("Spring Boot"));
    }

    @Test
    @DisplayName("Uses OCR when selectable PDF text exists but is not meaningful resume content")
    void fallsBackWhenSelectableTextIsNotResumeContent() throws Exception {
        Path pdf = temporaryDirectory.resolve("scanned-resume-with-metadata.pdf");
        createImagePdf(pdf, new String[]{
            "JANE DOE", "jane.ocr@example.com", "+1 415 555 0134",
            "PROFESSIONAL EXPERIENCE", "Java backend engineer",
            "EDUCATION", "Bachelor of Science in Computer Science",
            "SKILLS", "Java and Spring Boot"
        }, "Scanner metadata tracking identifier 123456789");

        String extracted = parser.extractText(pdf);

        assertEquals("jane.ocr@example.com", new ResumeProfileExtractor().extract(extracted).email());
        assertTrue(extracted.contains("Bachelor"));
    }

    @Test
    @DisplayName("Does not invoke OCR when PDFBox already extracts meaningful text")
    void prefersSelectableTextBeforeOcr() throws Exception {
        Path pdf = temporaryDirectory.resolve("text-resume.pdf");
        createPdf(pdf,
            "Jane Doe | jane.doe@example.test | Work Experience | Java backend engineer",
            "Education | Bachelor of Science | Skills | Spring Boot");
        AtomicInteger ocrCalls = new AtomicInteger();
        PdfOcrService ocrService = new PdfOcrService() {
            @Override
            public String extractText(PDDocument document) {
                ocrCalls.incrementAndGet();
                return "";
            }
        };
        PdfResumeParser textFirstParser = new PdfResumeParser(new ResumeContentValidator(), ocrService);

        assertTrue(textFirstParser.extractText(pdf).contains("Jane Doe"));
        assertEquals(0, ocrCalls.get());
    }

    @Test
    @DisplayName("Extracts an email split by PDF page and punctuation spacing")
    void extractsEmailFromPdfLineBreaks() throws Exception {
        Path pdf = temporaryDirectory.resolve("wrapped-email.pdf");
        createPdf(pdf,
            "Jane Doe | Work Experience | Java backend engineer",
            "JANE.DOE@",
            "EXAMPLE . COM | Education: Bachelor of Science");

        String extracted = parser.extractText(pdf);

        assertEquals("jane.doe@example.com", new ResumeProfileExtractor().extract(extracted).email());
    }

    @Test
    @DisplayName("Rejects readable project synopsis documents that are not resumes")
    void rejectsProjectSynopsis() throws Exception {
        Path synopsis = temporaryDirectory.resolve("project-synopsis.pdf");
        createPdf(synopsis,
            "Project Synopsis - Smart city transportation management system. "
                + "This project describes a Java and Spring Boot architecture for routing and data analysis.");

        PdfResumeParser.ResumeValidationException error = assertThrows(
            PdfResumeParser.ResumeValidationException.class, () -> parser.extractText(synopsis)
        );
        assertEquals(PdfResumeParser.ResumeValidationException.Reason.NOT_A_RESUME, error.getReason());
        assertTrue(error.getMessage().contains("does not appear to be a resume"));
    }

    @Test
    @DisplayName("Rejects image, office, and other unsupported formats")
    void rejectsUnsupportedExtensions() throws Exception {
        for (String extension : new String[]{"jpg", "jpeg", "png", "doc", "docx", "txt"}) {
            Path file = temporaryDirectory.resolve("document." + extension);
            Files.write(file, "%PDF-1.7 not actually a PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            PdfResumeParser.ResumeValidationException error = assertThrows(
                PdfResumeParser.ResumeValidationException.class, () -> parser.extractText(file)
            );
            assertEquals(PdfResumeParser.ResumeValidationException.Reason.UNSUPPORTED_FORMAT, error.getReason());
        }
    }

    @Test
    @DisplayName("Rejects renamed images and documents even with a PDF extension")
    void rejectsRenamedNonPdfContent() throws Exception {
        Path renamed = temporaryDirectory.resolve("image.pdf");
        Files.write(renamed, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});

        PdfResumeParser.ResumeValidationException error = assertThrows(
            PdfResumeParser.ResumeValidationException.class, () -> parser.extractText(renamed)
        );
        assertEquals(PdfResumeParser.ResumeValidationException.Reason.UNSUPPORTED_FORMAT, error.getReason());
    }

    @Test
    @DisplayName("Rejects empty, corrupted, unreadable, and textless PDFs")
    void rejectsInvalidPdfDocuments() throws Exception {
        Path empty = temporaryDirectory.resolve("empty.pdf");
        Files.createFile(empty);
        assertReason(empty, PdfResumeParser.ResumeValidationException.Reason.EMPTY);

        Path corrupted = temporaryDirectory.resolve("corrupted.pdf");
        Files.writeString(corrupted, "%PDF-1.7 this is not a valid document");
        assertReason(corrupted, PdfResumeParser.ResumeValidationException.Reason.CORRUPTED);

        Path noText = temporaryDirectory.resolve("no-text.pdf");
        createPdf(noText, "");
        assertReason(noText, PdfResumeParser.ResumeValidationException.Reason.NO_TEXT);

        Path blankScanned = temporaryDirectory.resolve("blank-scanned.pdf");
        createBlankScannedPdf(blankScanned);
        assertReason(blankScanned, PdfResumeParser.ResumeValidationException.Reason.NO_TEXT);

        Path missing = temporaryDirectory.resolve("missing.pdf");
        assertReason(missing, PdfResumeParser.ResumeValidationException.Reason.UNREADABLE);
    }

    @Test
    @DisplayName("Normalizes whitespace and removes non-text control characters")
    void cleansExtractedText() {
        assertEquals("Java Spring\nBoot", PdfResumeParser.cleanText(" Java   \tSpring \r\nBoot\u0001 "));
        assertEquals("resume@example.com",
            PdfResumeParser.cleanText("resume@\u00ADexample.com"));
    }

    private void assertReason(Path path, PdfResumeParser.ResumeValidationException.Reason reason) {
        PdfResumeParser.ResumeValidationException error = assertThrows(
            PdfResumeParser.ResumeValidationException.class, () -> parser.extractText(path)
        );
        assertEquals(reason, error.getReason());
    }

    private void createPdf(Path target, String... pageContents) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (String content : pageContents) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (!content.isEmpty()) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(50, 750);
                        stream.showText(content);
                        stream.endText();
                    }
                }
            }
            document.save(target.toFile());
        }
    }

    private void createScannedResumePdf(Path target) throws IOException {
        String[] lines = {
            "JANE DOE",
            "jane.ocr@example.com",
            "+1 415 555 0134",
            "PROFESSIONAL EXPERIENCE",
            "Software Engineer - 5 years",
            "Java Spring Boot MySQL",
            "EDUCATION",
            "Bachelor of Science in Computer Science",
            "SKILLS",
            "Java, Spring Boot, SQL"
        };
        createImagePdf(target, lines);
    }

    private void createBlankScannedPdf(Path target) throws IOException {
        createImagePdf(target, new String[0]);
    }

    private void createImagePdf(Path target, String[] lines) throws IOException {
        createImagePdf(target, lines, "");
    }

    private void createImagePdf(Path target, String[] lines, String selectableText) throws IOException {
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
                if (!selectableText.isBlank()) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    stream.newLineAtOffset(24, 24);
                    stream.showText(selectableText);
                    stream.endText();
                }
            }
            document.save(target.toFile());
        } finally {
            image.flush();
        }
    }
}
