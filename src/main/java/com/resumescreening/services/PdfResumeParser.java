package com.resumescreening.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Validates PDF resumes, preferring selectable text and falling back to OCR when needed.
 */
public class PdfResumeParser {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final Pattern NON_TEXT_CONTROL = Pattern.compile("[\\p{Cc}&&[^\\r\\n\\t]]");
    private static final Pattern EXCESS_WHITESPACE = Pattern.compile("[\\t\\x0B\\f ]+");
    private final ResumeContentValidator contentValidator;
    private final PdfOcrService ocrService;

    public PdfResumeParser() {
        this(new ResumeContentValidator(), new PdfOcrService());
    }

    public PdfResumeParser(ResumeContentValidator contentValidator) {
        this(contentValidator, new PdfOcrService());
    }

    public PdfResumeParser(ResumeContentValidator contentValidator, PdfOcrService ocrService) {
        this.contentValidator = contentValidator;
        this.ocrService = ocrService;
    }

    public String extractText(Path path) throws ResumeValidationException {
        if (path == null || path.getFileName() == null
            || !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResumeValidationException(
                "Invalid file! Please upload a PDF resume only.",
                ResumeValidationException.Reason.UNSUPPORTED_FORMAT
            );
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResumeValidationException("The selected PDF cannot be read.", ResumeValidationException.Reason.UNREADABLE);
        }
        try {
            long size = Files.size(path);
            if (size == 0) {
                throw new ResumeValidationException("The selected PDF is empty.", ResumeValidationException.Reason.EMPTY);
            }
            if (!hasPdfSignature(path)) {
                throw new ResumeValidationException(
                    "Invalid file! Please upload a PDF resume only.",
                    ResumeValidationException.Reason.UNSUPPORTED_FORMAT
                );
            }
            String cleaned;
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                if (document.isEncrypted()) {
                    throw new ResumeValidationException(
                        "This PDF is password-protected and cannot be read.",
                        ResumeValidationException.Reason.UNREADABLE
                    );
                }
                cleaned = cleanText(new PDFTextStripper().getText(document));
                long selectableCharacterCount = meaningfulCharacterCount(cleaned);
                if (selectableCharacterCount < 20 || !contentValidator.isResume(cleaned)) {
                    try {
                        String ocrText = cleanText(ocrService.extractText(document));
                        if (selectableCharacterCount < 20 || contentValidator.isResume(ocrText)) {
                            cleaned = ocrText;
                        }
                    } catch (PdfOcrService.OcrException e) {
                        if (selectableCharacterCount < 20) {
                            throw new ResumeValidationException(e.getMessage(), e,
                                ResumeValidationException.Reason.OCR_FAILED);
                        }
                    }
                }
            }
            if (meaningfulCharacterCount(cleaned) < 20) {
                throw new ResumeValidationException(
                    "No meaningful readable content was found, even after OCR. The PDF may be blank or too unclear to read.",
                    ResumeValidationException.Reason.NO_TEXT
                );
            }
            if (!contentValidator.isResume(cleaned)) {
                throw new ResumeValidationException(
                    "This PDF does not appear to be a resume. Upload a resume PDF only.",
                    ResumeValidationException.Reason.NOT_A_RESUME
                );
            }
            return cleaned;
        } catch (ResumeValidationException e) {
            throw e;
        } catch (InvalidPasswordException e) {
            throw new ResumeValidationException(
                "This PDF is password-protected and cannot be read.", e,
                ResumeValidationException.Reason.UNREADABLE
            );
        } catch (IOException | RuntimeException e) {
            throw new ResumeValidationException("The PDF is corrupted or could not be parsed.", e,
                ResumeValidationException.Reason.CORRUPTED);
        }
    }

    public static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n')
            .replace('\u00A0', ' ').replace('\u200B', ' ').replace('\uFEFF', ' ').replace("\u00AD", "");
        normalized = NON_TEXT_CONTROL.matcher(normalized).replaceAll("");
        normalized = EXCESS_WHITESPACE.matcher(normalized).replaceAll(" ");
        normalized = normalized.replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n");
        return normalized.strip();
    }

    private long meaningfulCharacterCount(String text) {
        return text.codePoints().filter(Character::isLetterOrDigit).count();
    }

    private boolean hasPdfSignature(Path path) throws IOException {
        byte[] header = new byte[PDF_SIGNATURE.length];
        try (var input = Files.newInputStream(path)) {
            int read = input.read(header);
            if (read != PDF_SIGNATURE.length) {
                return false;
            }
        }
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (header[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    public static class ResumeValidationException extends Exception {
        public enum Reason {
            UNSUPPORTED_FORMAT, EMPTY, UNREADABLE, CORRUPTED, NO_TEXT, OCR_FAILED, NOT_A_RESUME
        }

        private final Reason reason;

        public ResumeValidationException(String message, Reason reason) {
            super(message);
            this.reason = reason;
        }

        public ResumeValidationException(String message, Throwable cause, Reason reason) {
            super(message, cause);
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }
}
