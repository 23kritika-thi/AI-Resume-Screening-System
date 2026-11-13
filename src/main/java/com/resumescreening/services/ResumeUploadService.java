package com.resumescreening.services;

import com.resumescreening.models.Candidate;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ResumeEntry;
import com.resumescreening.models.ResumeProfile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates, extracts, and stores resume files only after successful PDF validation.
 */
public class ResumeUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeUploadService.class);
    private final DatabaseService databaseService;
    private final PdfResumeParser parser;
    private final ResumeProfileExtractor profileExtractor;
    private final Path storageDirectory;

    public ResumeUploadService(DatabaseService databaseService) {
        this(databaseService, new PdfResumeParser(), new ResumeProfileExtractor(),
            Path.of("storage", "resumes").toAbsolutePath().normalize());
    }

    public ResumeUploadService(DatabaseService databaseService, PdfResumeParser parser,
                               ResumeProfileExtractor profileExtractor, Path storageDirectory) {
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.profileExtractor = Objects.requireNonNull(profileExtractor, "profileExtractor");
        this.storageDirectory = Objects.requireNonNull(storageDirectory, "storageDirectory")
            .toAbsolutePath().normalize();
    }

    public synchronized UploadResult upload(Path source, Candidate candidate)
        throws PdfResumeParser.ResumeValidationException, SQLException, IOException, DuplicateResumeException {
        String extractedText = parser.extractText(source);
        ResumeProfile profile = profileExtractor.extract(extractedText);
        Candidate resolvedCandidate = mergeCandidate(candidate, profile);
        if (resolvedCandidate.getFullName() == null || resolvedCandidate.getFullName().isBlank()) {
            throw new IllegalArgumentException("Enter the candidate's name to save this resume.");
        }
        String hash = sha256(source);
        if (databaseService.resumeHashExists(hash)) {
            throw new DuplicateResumeException("This resume has already been uploaded.");
        }

        Files.createDirectories(storageDirectory);
        Path storedFile = storageDirectory.resolve(hash + ".pdf").normalize();
        if (!storedFile.getParent().equals(storageDirectory)) {
            throw new IOException("The resume storage path is invalid.");
        }
        Path temporaryFile = Files.createTempFile(storageDirectory, hash, ".upload");
        boolean stored = false;
        try {
            Files.copy(source, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporaryFile, storedFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, storedFile, StandardCopyOption.REPLACE_EXISTING);
            }
            stored = true;
            Resume resume = new Resume(
                0, 0, source.getFileName().toString(), storedFile.toString(), extractedText, hash, null
            );
            Resume saved = databaseService.saveResume(resolvedCandidate, resume);
            return new UploadResult(new ResumeEntry(resolvedCandidate, saved), profile);
        } catch (SQLException | RuntimeException | IOException e) {
            if (stored) {
                Files.deleteIfExists(storedFile);
            }
            throw e;
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public synchronized FileCleanupResult removeOrphanedFiles(List<String> filePaths) {
        int deleted = 0;
        int retained = 0;
        for (String filePath : filePaths) {
            Path file;
            try {
                file = Path.of(filePath).toAbsolutePath().normalize();
            } catch (java.nio.file.InvalidPathException e) {
                retained++;
                continue;
            }
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            if (!file.startsWith(storageDirectory) || file.equals(storageDirectory)) {
                retained++;
                continue;
            }
            try {
                if (!file.getParent().toRealPath().equals(storageDirectory.toRealPath())) {
                    retained++;
                    continue;
                }
                if (Files.deleteIfExists(file)) {
                    deleted++;
                }
            } catch (IOException e) {
                logger.warn("An unreferenced resume file could not be removed from managed storage.", e);
                retained++;
            }
        }
        return new FileCleanupResult(deleted, retained);
    }

    private Candidate mergeCandidate(Candidate provided, ResumeProfile profile) {
        Candidate candidate = provided == null ? new Candidate() : provided;
        if (isBlank(candidate.getFullName())) {
            candidate.setFullName(profile.candidateName());
        }
        if (isBlank(candidate.getEmail())) {
            candidate.setEmail(profile.email());
        }
        if (isBlank(candidate.getPhone())) {
            candidate.setPhone(profile.phone());
        }
        return candidate;
    }

    private String sha256(Path source) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(source)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for duplicate resume detection.", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record UploadResult(ResumeEntry entry, ResumeProfile profile) {
    }

    public record FileCleanupResult(int deleted, int retained) {
    }

    public static class DuplicateResumeException extends Exception {
        public DuplicateResumeException(String message) {
            super(message);
        }
    }
}
