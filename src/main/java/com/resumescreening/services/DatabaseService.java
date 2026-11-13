package com.resumescreening.services;

import com.resumescreening.database.DatabaseConnection;
import com.resumescreening.database.DatabaseInitializer;
import com.resumescreening.models.Candidate;
import com.resumescreening.models.DashboardMetrics;
import com.resumescreening.models.Job;
import com.resumescreening.models.JobAnalytics;
import com.resumescreening.models.Recruiter;
import com.resumescreening.models.ReportsSnapshot;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ResumeEntry;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.models.ScreeningResult;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service layer coordinating database connectivity, recruiter accounts, jobs, resumes, and screening.
 */
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private final DatabaseConnection databaseConnection;
    private final DatabaseInitializer databaseInitializer;

    public DatabaseService() {
        this(new DatabaseConnection());
    }

    public DatabaseService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.databaseInitializer = new DatabaseInitializer(databaseConnection);
    }

    public boolean isConnected() {
        return databaseConnection.testConnection(5);
    }

    public boolean setupDatabase() {
        return databaseInitializer.initializeDatabase();
    }

    public boolean applyPhaseFiveMigrations() {
        return databaseInitializer.applyPhaseFiveMigrations();
    }

    public Map<String, Boolean> getTableStatus() {
        Map<String, Boolean> statusMap = new HashMap<>();
        List<String> missing = databaseInitializer.verifyTables();
        for (String table : DatabaseInitializer.REQUIRED_TABLES) {
            statusMap.put(table, !missing.contains(table));
        }
        return statusMap;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public boolean recruiterEmailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM recruiter_accounts WHERE email = ? LIMIT 1";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    public Recruiter findRecruiterByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, email, created_at, updated_at "
            + "FROM recruiter_accounts WHERE email = ? LIMIT 1";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapRecruiter(results) : null;
            }
        }
    }

    public Recruiter createRecruiter(Recruiter recruiter) throws SQLException {
        String sql = "INSERT INTO recruiter_accounts (username, password_hash, full_name, email) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, recruiter.getUsername());
            statement.setString(2, recruiter.getPasswordHash());
            statement.setString(3, recruiter.getFullName());
            statement.setString(4, recruiter.getEmail());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating recruiter account did not return an ID.");
                }
                recruiter.setId(generatedKeys.getInt(1));
                return recruiter;
            }
        }
    }

    public void updateRecruiterPasswordHash(int recruiterId, String passwordHash) throws SQLException {
        String sql = "UPDATE recruiter_accounts SET password_hash = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setInt(2, recruiterId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Recruiter password hash could not be updated.");
            }
        }
    }

    public void updateRecruiterPasswordHash(String email, String passwordHash) throws SQLException {
        String sql = "UPDATE recruiter_accounts SET password_hash = ? WHERE email = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, email);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Recruiter password hash could not be updated.");
            }
        }
    }

    public void updateRecruiterFullName(int recruiterId, String fullName) throws SQLException {
        String sql = "UPDATE recruiter_accounts SET full_name = ? WHERE id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setInt(2, recruiterId);
            if (statement.executeUpdate() == 0) {
                try (PreparedStatement verify = connection.prepareStatement(
                    "SELECT 1 FROM recruiter_accounts WHERE id = ?"
                )) {
                    verify.setInt(1, recruiterId);
                    try (ResultSet results = verify.executeQuery()) {
                        if (!results.next()) {
                            throw new SQLException("Recruiter profile could not be updated because the account no longer exists.");
                        }
                    }
                }
            }
        }
    }

    public List<Job> getJobsForRecruiter(int recruiterId) throws SQLException {
        String sql = "SELECT id, recruiter_id, title, department, description, required_skills, preferred_skills, "
            + "min_experience_years, status, created_at, updated_at FROM jobs "
            + "WHERE recruiter_id = ? ORDER BY created_at DESC";
        List<Job> jobs = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recruiterId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    Job job = new Job(
                        results.getInt("id"), results.getInt("recruiter_id"), results.getString("title"),
                        results.getString("department"), results.getString("description"),
                        results.getString("required_skills"), results.getInt("min_experience_years"),
                        results.getString("status"), toLocalDateTime(results.getTimestamp("created_at")),
                        toLocalDateTime(results.getTimestamp("updated_at"))
                    );
                    job.setPreferredSkills(results.getString("preferred_skills"));
                    jobs.add(job);
                }
            }
        }
        return jobs;
    }

    public DashboardMetrics getDashboardMetrics(int recruiterId) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM jobs WHERE recruiter_id = ?) AS jobs, "
            + "(SELECT COUNT(DISTINCT candidate_id) FROM resumes) AS candidates, "
            + "(SELECT COUNT(*) FROM resumes) AS resumes, "
            + "(SELECT COUNT(*) FROM screening_results s JOIN jobs j ON j.id = s.job_id "
            + "WHERE j.recruiter_id = ?) AS screenings";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recruiterId);
            statement.setInt(2, recruiterId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("Dashboard metrics could not be loaded.");
                }
                return new DashboardMetrics(
                    results.getInt("jobs"), results.getInt("candidates"),
                    results.getInt("resumes"), results.getInt("screenings")
                );
            }
        }
    }

    public ReportsSnapshot getReportsSnapshot(int recruiterId) throws SQLException {
        List<JobAnalytics> reports = new ArrayList<>();
        for (Job job : getJobsForRecruiter(recruiterId)) {
            reports.add(new JobAnalytics(job, getLatestScreeningResults(job.getId())));
        }
        return new ReportsSnapshot(getDashboardMetrics(recruiterId), reports);
    }

    public int getScreenedResumeCountForJob(int jobId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT resume_id) FROM screening_results WHERE job_id = ?";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, jobId);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

    public List<ScreeningEntry> getRecentScreeningsForRecruiter(int recruiterId, int limit) throws SQLException {
        List<ScreeningEntry> recent = new ArrayList<>();
        for (Job job : getJobsForRecruiter(recruiterId)) {
            recent.addAll(getLatestScreeningResults(job.getId()));
        }
        recent.sort((left, right) -> {
            if (left.result().getScreenedAt() == null) {
                return 1;
            }
            if (right.result().getScreenedAt() == null) {
                return -1;
            }
            return right.result().getScreenedAt().compareTo(left.result().getScreenedAt());
        });
        return recent.stream().limit(Math.max(0, limit)).toList();
    }

    public Job createJob(Job job) throws SQLException {
        String sql = "INSERT INTO jobs (recruiter_id, title, department, description, required_skills, "
            + "preferred_skills, min_experience_years, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN')";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, job.getRecruiterId());
            statement.setString(2, job.getTitle());
            statement.setString(3, job.getDepartment());
            statement.setString(4, job.getDescription());
            statement.setString(5, job.getRequiredSkills());
            statement.setString(6, job.getPreferredSkills());
            statement.setInt(7, job.getMinExperienceYears());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating the job did not return an ID.");
                }
                job.setId(keys.getInt(1));
            }
        }
        return job;
    }

    public boolean resumeHashExists(String fileHash) throws SQLException {
        String sql = "SELECT 1 FROM resumes WHERE file_hash = ? LIMIT 1";
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileHash);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    public Resume saveResume(Candidate candidate, Resume resume) throws SQLException {
        if (candidate == null || candidate.getFullName() == null || candidate.getFullName().isBlank()) {
            throw new IllegalArgumentException("A candidate name is required to save a resume.");
        }
        String findCandidateSql = "SELECT id FROM candidates WHERE LOWER(email) = LOWER(?) LIMIT 1";
        String createCandidateSql = "INSERT INTO candidates (full_name, email, phone, linkedin_url) VALUES (?, ?, ?, ?)";
        String createResumeSql = "INSERT INTO resumes (candidate_id, file_name, file_path, extracted_text, file_hash) "
            + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer candidateId = null;
                if (candidate.getEmail() != null && !candidate.getEmail().isBlank()) {
                    try (PreparedStatement find = connection.prepareStatement(findCandidateSql)) {
                        find.setString(1, candidate.getEmail().strip());
                        try (ResultSet results = find.executeQuery()) {
                            if (results.next()) {
                                candidateId = results.getInt("id");
                            }
                        }
                    }
                }
                if (candidateId == null) {
                    try (PreparedStatement create = connection.prepareStatement(
                        createCandidateSql, Statement.RETURN_GENERATED_KEYS
                    )) {
                        create.setString(1, candidate.getFullName().strip());
                        create.setString(2, blankToNull(candidate.getEmail()));
                        create.setString(3, blankToNull(candidate.getPhone()));
                        create.setString(4, blankToNull(candidate.getLinkedinUrl()));
                        create.executeUpdate();
                        try (ResultSet keys = create.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("Creating a candidate did not return an ID.");
                            }
                            candidateId = keys.getInt(1);
                        }
                    }
                } else {
                    String update = "UPDATE candidates SET full_name = ?, phone = COALESCE(?, phone) WHERE id = ?";
                    try (PreparedStatement updateCandidate = connection.prepareStatement(update)) {
                        updateCandidate.setString(1, candidate.getFullName().strip());
                        updateCandidate.setString(2, blankToNull(candidate.getPhone()));
                        updateCandidate.setInt(3, candidateId);
                        updateCandidate.executeUpdate();
                    }
                }
                resume.setCandidateId(candidateId);
                try (PreparedStatement createResume = connection.prepareStatement(
                    createResumeSql, Statement.RETURN_GENERATED_KEYS
                )) {
                    createResume.setInt(1, candidateId);
                    createResume.setString(2, resume.getFileName());
                    createResume.setString(3, resume.getFilePath());
                    createResume.setString(4, resume.getExtractedText());
                    createResume.setString(5, resume.getFileHash());
                    createResume.executeUpdate();
                    try (ResultSet keys = createResume.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Saving the resume did not return an ID.");
                        }
                        resume.setId(keys.getInt(1));
                    }
                }
                connection.commit();
                return resume;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<ResumeEntry> getResumes() throws SQLException {
        String sql = "SELECT c.id AS candidate_id, c.full_name, c.email, c.phone, c.linkedin_url, "
            + "c.created_at AS candidate_created_at, r.id AS resume_id, r.file_name, r.file_path, "
            + "r.extracted_text, r.file_hash, r.uploaded_at FROM resumes r "
            + "JOIN candidates c ON c.id = r.candidate_id ORDER BY r.uploaded_at DESC";
        List<ResumeEntry> entries = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                Candidate candidate = new Candidate(
                    results.getInt("candidate_id"), results.getString("full_name"), results.getString("email"),
                    results.getString("phone"), results.getString("linkedin_url"),
                    toLocalDateTime(results.getTimestamp("candidate_created_at"))
                );
                Resume resume = new Resume(
                    results.getInt("resume_id"), candidate.getId(), results.getString("file_name"),
                    results.getString("file_path"), results.getString("extracted_text"),
                    results.getString("file_hash"), toLocalDateTime(results.getTimestamp("uploaded_at"))
                );
                entries.add(new ResumeEntry(candidate, resume));
            }
        }
        return entries;
    }

    public List<Candidate> getCandidates() throws SQLException {
        String sql = "SELECT id, full_name, email, phone, linkedin_url, created_at "
            + "FROM candidates ORDER BY full_name, id";
        List<Candidate> candidates = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                candidates.add(new Candidate(
                    results.getInt("id"),
                    results.getString("full_name"),
                    results.getString("email"),
                    results.getString("phone"),
                    results.getString("linkedin_url"),
                    toLocalDateTime(results.getTimestamp("created_at"))
                ));
            }
        }
        return candidates;
    }

    /**
     * Deletes a candidate and returns resume paths that are no longer referenced by any resume row.
     * Database foreign-key cascades remove associated resumes and screening results.
     */
    public List<String> deleteCandidate(int candidateId) throws SQLException {
        if (candidateId <= 0) {
            throw new IllegalArgumentException("A valid candidate ID is required.");
        }

        String findPathsSql = "SELECT file_path FROM resumes WHERE candidate_id = ? FOR UPDATE";
        String deleteCandidateSql = "DELETE FROM candidates WHERE id = ?";
        String pathInUseSql = "SELECT 1 FROM resumes WHERE file_path = ? LIMIT 1 FOR UPDATE";
        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Set<String> candidatePaths = new LinkedHashSet<>();
                try (PreparedStatement findPaths = connection.prepareStatement(findPathsSql)) {
                    findPaths.setInt(1, candidateId);
                    try (ResultSet results = findPaths.executeQuery()) {
                        while (results.next()) {
                            String filePath = results.getString("file_path");
                            if (filePath != null && !filePath.isBlank()) {
                                candidatePaths.add(filePath);
                            }
                        }
                    }
                }

                try (PreparedStatement delete = connection.prepareStatement(deleteCandidateSql)) {
                    delete.setInt(1, candidateId);
                    if (delete.executeUpdate() != 1) {
                        throw new SQLException("The candidate no longer exists.");
                    }
                }

                List<String> orphanedPaths = new ArrayList<>();
                try (PreparedStatement checkPath = connection.prepareStatement(pathInUseSql)) {
                    for (String filePath : candidatePaths) {
                        checkPath.setString(1, filePath);
                        try (ResultSet references = checkPath.executeQuery()) {
                            if (!references.next()) {
                                orphanedPaths.add(filePath);
                            }
                        }
                    }
                }
                connection.commit();
                return List.copyOf(orphanedPaths);
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public DemoDataCleanup deleteDemoData(int recruiterId) throws SQLException {
        return deleteDemoData(recruiterId, "");
    }

    public DemoDataCleanup deleteDemoData(int recruiterId, String namespace) throws SQLException {
        if (recruiterId <= 0) {
            throw new IllegalArgumentException("A valid recruiter ID is required.");
        }
        String normalizedNamespace = namespace == null ? "" : namespace.strip().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedNamespace.isEmpty()
            && !normalizedNamespace.matches("[a-z0-9-]{1,24}")) {
            throw new IllegalArgumentException("The demo namespace is invalid.");
        }

        String suffix = normalizedNamespace.isEmpty() ? "" : " (" + normalizedNamespace + ")";
        String demoEmailPattern = normalizedNamespace.isEmpty()
            ? "%@demo.resume-screening.example"
            : "%." + normalizedNamespace + "@demo.resume-screening.example";
        String demoJobConstraint = normalizedNamespace.isEmpty() ? ""
            : " AND RIGHT(title, ?) = ?";
        String findJobsSql = "SELECT id FROM jobs WHERE recruiter_id = ? AND LEFT(title, 7) = '[DEMO] '"
            + demoJobConstraint + " FOR UPDATE";
        String deleteJobSql = "DELETE FROM jobs WHERE id = ? AND recruiter_id = ?";
        String findCandidatesSql = "SELECT id FROM candidates WHERE email LIKE ? FOR UPDATE";
        String findPathsSql = "SELECT file_path FROM resumes WHERE candidate_id = ? FOR UPDATE";
        String deleteCandidateSql = "DELETE FROM candidates WHERE id = ? AND email LIKE ? "
            + "AND NOT EXISTS (SELECT 1 FROM screening_results WHERE candidate_id = candidates.id) "
            + "AND NOT EXISTS (SELECT 1 FROM jobs WHERE LEFT(title, 7) = '[DEMO] '"
            + demoJobConstraint + ")";
        String pathInUseSql = "SELECT 1 FROM resumes WHERE file_path = ? LIMIT 1 FOR UPDATE";

        try (Connection connection = databaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                List<Integer> jobIds = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(findJobsSql)) {
                    statement.setInt(1, recruiterId);
                    if (!normalizedNamespace.isEmpty()) {
                        statement.setInt(2, suffix.length());
                        statement.setString(3, suffix);
                    }
                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            jobIds.add(results.getInt(1));
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(deleteJobSql)) {
                    for (int jobId : jobIds) {
                        statement.setInt(1, jobId);
                        statement.setInt(2, recruiterId);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                List<Integer> candidateIds = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(findCandidatesSql)) {
                    statement.setString(1, demoEmailPattern);
                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            candidateIds.add(results.getInt(1));
                        }
                    }
                }

                Set<String> candidatePaths = new LinkedHashSet<>();
                int deletedCandidates = 0;
                int retainedCandidates = 0;
                for (int candidateId : candidateIds) {
                    try (PreparedStatement statement = connection.prepareStatement(findPathsSql)) {
                        statement.setInt(1, candidateId);
                        try (ResultSet results = statement.executeQuery()) {
                            while (results.next()) {
                                String path = results.getString(1);
                                if (path != null && !path.isBlank()) {
                                    candidatePaths.add(path);
                                }
                            }
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(deleteCandidateSql)) {
                        statement.setInt(1, candidateId);
                        statement.setString(2, demoEmailPattern);
                        if (!normalizedNamespace.isEmpty()) {
                            statement.setInt(3, suffix.length());
                            statement.setString(4, suffix);
                        }
                        if (statement.executeUpdate() == 1) {
                            deletedCandidates++;
                        } else {
                            retainedCandidates++;
                        }
                    }
                }

                List<String> orphanedPaths = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(pathInUseSql)) {
                    for (String path : candidatePaths) {
                        statement.setString(1, path);
                        try (ResultSet results = statement.executeQuery()) {
                            if (!results.next()) {
                                orphanedPaths.add(path);
                            }
                        }
                    }
                }
                connection.commit();
                return new DemoDataCleanup(jobIds.size(), deletedCandidates, retainedCandidates,
                    List.copyOf(orphanedPaths));
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public record DemoDataCleanup(int jobsDeleted, int candidatesDeleted, int candidatesRetained,
                                  List<String> orphanedResumePaths) {
        public DemoDataCleanup {
            orphanedResumePaths = List.copyOf(orphanedResumePaths);
        }
    }

    public void saveScreeningResults(List<ScreeningResult> resultsToSave) throws SQLException {
        String sql = "INSERT INTO screening_results (job_id, candidate_id, resume_id, match_score, "
            + "required_skills_score, preferred_skills_score, experience_score, text_similarity_score, "
            + "matched_skills, missing_skills, ranking, recommendation) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (ScreeningResult result : resultsToSave) {
                    statement.setInt(1, result.getJobId());
                    statement.setInt(2, result.getCandidateId());
                    statement.setInt(3, result.getResumeId());
                    statement.setBigDecimal(4, result.getMatchScore());
                    statement.setBigDecimal(5, nonNullScore(result.getRequiredSkillsScore()));
                    statement.setBigDecimal(6, nonNullScore(result.getPreferredSkillsScore()));
                    statement.setBigDecimal(7, nonNullScore(result.getExperienceScore()));
                    statement.setBigDecimal(8, nonNullScore(result.getTextSimilarityScore()));
                    statement.setString(9, result.getMatchedSkills());
                    statement.setString(10, result.getMissingSkills());
                    if (result.getRanking() == null) {
                        statement.setNull(11, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(11, result.getRanking());
                    }
                    statement.setString(12, result.getRecommendation());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<ScreeningEntry> getLatestScreeningResults(int jobId) throws SQLException {
        String sql = "SELECT c.id AS candidate_id, c.full_name, c.email, c.phone, c.linkedin_url, "
            + "c.created_at AS candidate_created_at, r.id AS resume_id, r.file_name, r.file_path, "
            + "r.extracted_text, r.file_hash, r.uploaded_at, s.id AS screening_id, s.match_score, "
            + "s.required_skills_score, s.preferred_skills_score, s.experience_score, "
            + "s.text_similarity_score, s.matched_skills, s.missing_skills, s.ranking, "
            + "s.recommendation, s.screened_at FROM screening_results s "
            + "JOIN resumes r ON r.id = s.resume_id JOIN candidates c ON c.id = s.candidate_id "
            + "WHERE s.job_id = ? AND s.id = (SELECT s2.id FROM screening_results s2 "
            + "WHERE s2.job_id = s.job_id AND s2.resume_id = s.resume_id "
            + "ORDER BY s2.screened_at DESC, s2.id DESC LIMIT 1) "
            + "ORDER BY s.ranking ASC, s.match_score DESC";
        List<ScreeningEntry> entries = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, jobId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    Candidate candidate = new Candidate(
                        results.getInt("candidate_id"), results.getString("full_name"), results.getString("email"),
                        results.getString("phone"), results.getString("linkedin_url"),
                        toLocalDateTime(results.getTimestamp("candidate_created_at"))
                    );
                    Resume resume = new Resume(
                        results.getInt("resume_id"), candidate.getId(), results.getString("file_name"),
                        results.getString("file_path"), results.getString("extracted_text"),
                        results.getString("file_hash"), toLocalDateTime(results.getTimestamp("uploaded_at"))
                    );
                    ScreeningResult screening = new ScreeningResult(
                        results.getInt("screening_id"), jobId, candidate.getId(), resume.getId(),
                        results.getBigDecimal("match_score"), results.getString("matched_skills"),
                        results.getString("missing_skills"), (Integer) results.getObject("ranking"),
                        results.getString("recommendation"), toLocalDateTime(results.getTimestamp("screened_at"))
                    );
                    screening.setRequiredSkillsScore(results.getBigDecimal("required_skills_score"));
                    screening.setPreferredSkillsScore(results.getBigDecimal("preferred_skills_score"));
                    screening.setExperienceScore(results.getBigDecimal("experience_score"));
                    screening.setTextSimilarityScore(results.getBigDecimal("text_similarity_score"));
                    entries.add(new ScreeningEntry(candidate, resume, screening));
                }
            }
        }
        return entries;
    }

    private Recruiter mapRecruiter(ResultSet results) throws SQLException {
        Timestamp createdAt = results.getTimestamp("created_at");
        Timestamp updatedAt = results.getTimestamp("updated_at");
        return new Recruiter(
            results.getInt("id"), results.getString("username"), results.getString("password_hash"),
            results.getString("full_name"), results.getString("email"),
            createdAt == null ? null : createdAt.toLocalDateTime(),
            updatedAt == null ? null : updatedAt.toLocalDateTime()
        );
    }

    private static BigDecimal nonNullScore(BigDecimal score) {
        return score == null ? BigDecimal.ZERO : score;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
