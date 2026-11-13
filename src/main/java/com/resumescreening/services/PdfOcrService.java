package com.resumescreening.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Renders PDF pages and extracts text with the bundled Tesseract English model. */
public class PdfOcrService {

    private static final String MODEL_RESOURCE = "/tessdata/eng.traineddata";
    private static final float RENDER_DPI = 300;
    private static volatile Path tessdataDirectory;

    public String extractText(PDDocument document) throws OcrException {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(getTessdataDirectory().toString());
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);

            PDFRenderer renderer = new PDFRenderer(document);
            StringJoiner text = new StringJoiner("\n");
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, RENDER_DPI, ImageType.RGB);
                try {
                    text.add(tesseract.doOCR(
                        image.getWidth(), image.getHeight(), toGrayscale(image), 8, "", null
                    ));
                } finally {
                    image.flush();
                }
            }
            return text.toString();
        } catch (IOException | TesseractException | RuntimeException | LinkageError e) {
            throw new OcrException(
                "OCR could not read this scanned PDF. Check that the PDF pages are clear and try again.", e
            );
        }
    }

    private ByteBuffer toGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int luminance = (((rgb >>> 16) & 0xFF) * 299
                    + ((rgb >>> 8) & 0xFF) * 587
                    + (rgb & 0xFF) * 114) / 1000;
                pixels.put((byte) luminance);
            }
        }
        pixels.flip();
        return pixels;
    }

    private Path getTessdataDirectory() throws IOException {
        Path current = tessdataDirectory;
        if (current != null) {
            return current;
        }
        synchronized (PdfOcrService.class) {
            if (tessdataDirectory == null) {
                Path directory = Files.createTempDirectory("resume-screening-tessdata-");
                try (InputStream model = PdfOcrService.class.getResourceAsStream(MODEL_RESOURCE)) {
                    if (model == null) {
                        throw new IOException("The bundled English OCR model is unavailable.");
                    }
                    Files.copy(model, directory.resolve("eng.traineddata"));
                } catch (IOException e) {
                    Files.deleteIfExists(directory);
                    throw e;
                }
                directory.toFile().deleteOnExit();
                directory.resolve("eng.traineddata").toFile().deleteOnExit();
                tessdataDirectory = directory;
            }
            return tessdataDirectory;
        }
    }

    public static class OcrException extends Exception {
        public OcrException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
