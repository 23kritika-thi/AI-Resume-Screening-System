package com.resumescreening.ui;

import com.resumescreening.models.Recruiter;
import com.resumescreening.models.Job;
import com.resumescreening.models.Candidate;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.services.DatabaseService;
import com.resumescreening.services.DemoDataService;
import com.resumescreening.services.PasswordRecoveryService;
import com.resumescreening.services.RecruiterAuthService;
import com.resumescreening.services.RecruiterSession;
import com.resumescreening.services.RecoveryCodeSender;
import com.resumescreening.services.ResumeScreeningService;
import com.resumescreening.services.ResumeUploadService;
import com.resumescreening.services.PdfResumeParser;
import com.resumescreening.services.ReportExportService;
import com.resumescreening.services.SmtpRecoveryCodeSender;
import com.resumescreening.ui.components.HeaderPanel;
import com.resumescreening.ui.components.SidebarPanel;
import com.resumescreening.ui.theme.UITheme;
import com.resumescreening.ui.views.AuthenticationView;
import com.resumescreening.ui.views.DashboardView;
import com.resumescreening.ui.views.JobManagementView;
import com.resumescreening.ui.views.ResumeWorkspaceView;
import com.resumescreening.ui.views.CandidateDirectoryView;
import com.resumescreening.ui.views.ReportsAnalyticsView;
import com.resumescreening.ui.views.SettingsView;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.sql.SQLException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application window. Recruiter authentication is required before app views are created.
 */
public class MainFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private static final String LOGIN_CARD = "LOGIN";
    private static final String APPLICATION_CARD = "APPLICATION";

    private final RecruiterAuthService authService;
    private final DatabaseService databaseService;
    private final ResumeUploadService resumeUploadService;
    private final ResumeScreeningService resumeScreeningService;
    private final DemoDataService demoDataService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final RecruiterSession recruiterSession = new RecruiterSession();
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootCards = new JPanel(rootLayout);
    private final AuthenticationView authenticationView = new AuthenticationView();
    private HeaderPanel headerPanel;
    private SidebarPanel sidebarPanel;
    private JPanel applicationPanel;
    private CardLayout contentLayout;
    private JPanel contentCards;
    private JobManagementView jobManagementView;
    private CandidateDirectoryView candidateDirectoryView;
    private ResumeWorkspaceView resumeWorkspaceView;
    private ReportsAnalyticsView reportsAnalyticsView;
    private SettingsView settingsView;
    private DashboardView dashboardView;

    public MainFrame() {
        this(new DatabaseService());
    }

    public MainFrame(DatabaseService databaseService) {
        this(databaseService, true);
    }

    private MainFrame(DatabaseService databaseService, boolean phaseFiveSchemaReady) {
        super("AI-Based Resume Screening and Candidate Ranking System");
        this.databaseService = databaseService;
        authService = new RecruiterAuthService(databaseService);
        resumeUploadService = new ResumeUploadService(databaseService);
        resumeScreeningService = new ResumeScreeningService(databaseService);
        demoDataService = new DemoDataService(databaseService, resumeUploadService, resumeScreeningService);
        passwordRecoveryService = new PasswordRecoveryService(
            databaseService,
            new SmtpRecoveryCodeSender()
        );

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1280, 800));
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        authenticationView.setLoginHandler(this::handleLogin);
        authenticationView.setRegistrationHandler(this::handleRegistration);
        authenticationView.setRecoveryHandler(new AuthenticationView.RecoveryHandler() {
            @Override
            public void requestCode(String email) {
                handleRecoveryRequest(email);
            }

            @Override
            public void verifyCode(String email, String code) {
                handleRecoveryCodeVerification(email, code);
            }

            @Override
            public void resetPassword(String email, String newPassword, String confirmation) {
                handlePasswordReset(email, newPassword, confirmation);
            }
        });
        if (!phaseFiveSchemaReady) {
            authenticationView.showLoginMessage(
                "Phase 5 database upgrade could not be applied. Run the database setup command and retry."
            );
        }
        rootCards.add(authenticationView, LOGIN_CARD);
        setContentPane(rootCards);
        rootLayout.show(rootCards, LOGIN_CARD);
        pack();
    }

    private void handleLogin(String email, String password) {
        try {
            showDashboard(authService.login(email, password));
        } catch (RecruiterAuthService.ValidationException | RecruiterAuthService.AuthenticationException e) {
            authenticationView.showLoginMessage(e.getMessage());
        } catch (SQLException e) {
            logger.error("Recruiter login failed because the account database is unavailable.", e);
            authenticationView.showLoginMessage(
                "Unable to access the account database. Check your connection and try again."
            );
        }
    }

    private void handleRegistration(String fullName, String email, String password, String confirmation) {
        try {
            authService.register(fullName, email, password, confirmation);
            authenticationView.showLogin(email.trim(), "Account created. Sign in with your new account.");
        } catch (RecruiterAuthService.ValidationException | RecruiterAuthService.DuplicateEmailException e) {
            authenticationView.showRegistrationMessage(e.getMessage());
        } catch (SQLException e) {
            logger.error("Recruiter registration failed because the account database is unavailable.", e);
            authenticationView.showRegistrationMessage(
                "Unable to access the account database. Check your connection and try again."
            );
        }
    }

    private void handleRecoveryRequest(String email) {
        try {
            passwordRecoveryService.requestCode(email);
            authenticationView.showRecoveryCodeStep(recoveryRequestMessage());
        } catch (PasswordRecoveryService.RecoveryValidationException e) {
            authenticationView.showRecoveryEmailMessage(e.getMessage(), true);
        } catch (RecoveryCodeSender.EmailConfigurationException e) {
            logger.warn("Password recovery is unavailable because SMTP is not configured.");
            authenticationView.showRecoveryEmailMessage(e.getMessage(), true);
        } catch (RecoveryCodeSender.EmailDeliveryException e) {
            logger.warn("Password recovery email delivery failed.");
            authenticationView.showRecoveryCodeStep(recoveryRequestMessage());
        } catch (SQLException e) {
            logger.error("Password recovery could not access the recruiter account database.", e);
            authenticationView.showRecoveryEmailMessage(
                "Unable to process the recovery request right now. Please try again later.",
                true
            );
        }
    }

    private void handleRecoveryCodeVerification(String email, String code) {
        try {
            if (passwordRecoveryService.verifyCode(email, code)) {
                authenticationView.showRecoveryPasswordStep("Code verified. Choose a new password.");
            } else {
                authenticationView.showRecoveryStepMessage(
                    "That code is invalid, expired, or already used. Request a new code and try again."
                );
            }
        } catch (PasswordRecoveryService.RecoveryValidationException e) {
            authenticationView.showRecoveryStepMessage(e.getMessage());
        }
    }

    private void handlePasswordReset(String email, String newPassword, String confirmation) {
        try {
            passwordRecoveryService.resetPassword(email, newPassword, confirmation);
            authenticationView.showLogin(
                email,
                "Password reset successfully. Sign in with your new password."
            );
        } catch (PasswordRecoveryService.RecoveryValidationException e) {
            authenticationView.showRecoveryStepMessage(e.getMessage());
        } catch (SQLException e) {
            logger.error("Password recovery could not update the recruiter account.", e);
            authenticationView.showRecoveryStepMessage(
                "Unable to update your password right now. Please try again."
            );
        }
    }

    private static String recoveryRequestMessage() {
        return "If this email belongs to an account, a verification code will be sent. "
            + "Check your inbox shortly; delivery may take a few minutes.";
    }

    private void showDashboard(Recruiter recruiter) {
        if (recruiter == null) {
            throw new IllegalArgumentException("A verified recruiter account is required to open the dashboard.");
        }
        recruiterSession.login(recruiter);
        buildApplication();
        rootLayout.show(rootCards, APPLICATION_CARD);
        rootCards.revalidate();
        rootCards.repaint();
    }

    private void buildApplication() {
        if (applicationPanel != null) {
            rootCards.remove(applicationPanel);
        }

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UITheme.BG_CANVAS);

        sidebarPanel = new SidebarPanel();
        sidebarPanel.setNavigationListener(this::handleNavigation);
        rootPanel.add(sidebarPanel, BorderLayout.WEST);

        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(UITheme.BG_CANVAS);

        headerPanel = new HeaderPanel();
        headerPanel.setRecruiterName(recruiterSession.getCurrentRecruiter().getFullName());
        headerPanel.addLogoutListener(event -> logout());
        rightArea.add(headerPanel, BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentCards = new JPanel(contentLayout);
        contentCards.setBackground(UITheme.BG_CANVAS);
        initViews();
        rightArea.add(contentCards, BorderLayout.CENTER);

        rootPanel.add(rightArea, BorderLayout.CENTER);
        applicationPanel = rootPanel;
        rootCards.add(applicationPanel, APPLICATION_CARD);
        contentLayout.show(contentCards, SidebarPanel.NAV_DASHBOARD);
        headerPanel.setPageTitle(
            "Dashboard",
            "Overview of active hiring pipeline and candidate metrics"
        );
    }

    private void initViews() {
        int recruiterId = recruiterSession.getCurrentRecruiter().getId();
        dashboardView = new DashboardView(
            databaseService, recruiterId, recruiterSession.getCurrentRecruiter().getFullName(), this::handleNavigation
        );
        contentCards.add(dashboardView, SidebarPanel.NAV_DASHBOARD);

        jobManagementView = new JobManagementView();
        jobManagementView.setCreateHandler(job -> createJob(job, recruiterId));
        contentCards.add(jobManagementView, SidebarPanel.NAV_JOBS);

        candidateDirectoryView = new CandidateDirectoryView();
        candidateDirectoryView.setDeleteHandler(this::deleteCandidate);
        contentCards.add(candidateDirectoryView, SidebarPanel.NAV_CANDIDATES);

        resumeWorkspaceView = new ResumeWorkspaceView();
        resumeWorkspaceView.setUploadHandler(this::uploadResume);
        resumeWorkspaceView.setScreenHandler(this::screenResumes);
        resumeWorkspaceView.setJobSelectionHandler(this::loadScreeningResults);
        contentCards.add(resumeWorkspaceView, SidebarPanel.NAV_SCREENING);
        refreshPhaseFiveData();

        reportsAnalyticsView = new ReportsAnalyticsView();
        reportsAnalyticsView.setRefreshHandler(this::loadReports);
        reportsAnalyticsView.setDemoDataHandlers(this::loadDemoData, this::removeDemoData);
        reportsAnalyticsView.setExportHandlers(
            path -> exportReports(path, false),
            path -> exportReports(path, true)
        );
        contentCards.add(reportsAnalyticsView, SidebarPanel.NAV_REPORTS);
        settingsView = new SettingsView(
            recruiterSession.getCurrentRecruiter(), databaseService.getDatabaseConnection().getConfig()
        );
        settingsView.setProfileSaveHandler(this::updateRecruiterProfile);
        settingsView.setDatabaseTestHandler(this::testDatabaseConnection);
        contentCards.add(settingsView, SidebarPanel.NAV_SETTINGS);
    }

    private void handleNavigation(String navId) {
        if (!recruiterSession.isAuthenticated()) {
            rootLayout.show(rootCards, LOGIN_CARD);
            return;
        }
        contentLayout.show(contentCards, navId);

        switch (navId) {
            case SidebarPanel.NAV_DASHBOARD:
                headerPanel.setPageTitle("Dashboard", "Overview of active hiring pipeline and candidate metrics");
                refreshDashboard();
                break;
            case SidebarPanel.NAV_JOBS:
                headerPanel.setPageTitle("Job Openings", "Manage open positions, departments, and required qualifications");
                break;
            case SidebarPanel.NAV_CANDIDATES:
                headerPanel.setPageTitle("Candidates", "Stored candidate records and available contact details");
                break;
            case SidebarPanel.NAV_SCREENING:
                headerPanel.setPageTitle("Resume Screening", "Transparent weighted scores and recruiter decision support");
                refreshPhaseFiveData();
                break;
            case SidebarPanel.NAV_REPORTS:
                headerPanel.setPageTitle("Reports & Analytics", "Database-backed job, candidate, and screening insights");
                loadReports();
                break;
            case SidebarPanel.NAV_SETTINGS:
                headerPanel.setPageTitle("Settings", "Update your recruiter profile and check database connectivity");
                testDatabaseConnection();
                break;
            default:
                headerPanel.setPageTitle("ResumeAI", null);
                break;
        }
    }

    private void updateRecruiterProfile(String fullName) {
        Recruiter currentRecruiter = recruiterSession.getCurrentRecruiter();
        if (settingsView == null || currentRecruiter == null) {
            return;
        }
        settingsView.setProfileSaving(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return authService.updateFullName(currentRecruiter.getId(), fullName);
            }

            @Override
            protected void done() {
                try {
                    String savedName = get();
                    currentRecruiter.setFullName(savedName);
                    settingsView.showProfileSaved(savedName);
                    headerPanel.setRecruiterName(savedName);
                    dashboardView.setRecruiterName(savedName);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    settingsView.showStatus("Profile update was interrupted.", true);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RecruiterAuthService.ValidationException validation) {
                        settingsView.showStatus(validation.getMessage(), true);
                    } else {
                        logger.error("Recruiter profile update failed.", cause);
                        settingsView.showStatus(
                            "Unable to update your profile. Check database connectivity and try again.", true
                        );
                    }
                } finally {
                    settingsView.setProfileSaving(false);
                }
            }
        }.execute();
    }

    private void testDatabaseConnection() {
        if (settingsView == null || !settingsView.getTestConnectionButton().isEnabled()) {
            return;
        }
        settingsView.setDatabaseTesting(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return databaseService.isConnected();
            }

            @Override
            protected void done() {
                try {
                    settingsView.showDatabaseTestResult(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    settingsView.showDatabaseTestFailure("Database connection test was interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Database connection test failed unexpectedly.", e.getCause());
                    settingsView.showDatabaseTestFailure(
                        "The database connection test failed unexpectedly. See the application log."
                    );
                } finally {
                    settingsView.setDatabaseTesting(false);
                }
            }
        }.execute();
    }

    private void loadReports() {
        loadReports(null);
    }

    private void loadReports(String successMessage) {
        if (reportsAnalyticsView == null || !recruiterSession.isAuthenticated()) {
            return;
        }
        reportsAnalyticsView.setLoading(true);
        int recruiterId = recruiterSession.getCurrentRecruiter().getId();
        new SwingWorker<com.resumescreening.models.ReportsSnapshot, Void>() {
            @Override
            protected com.resumescreening.models.ReportsSnapshot doInBackground() throws Exception {
                return databaseService.getReportsSnapshot(recruiterId);
            }

            @Override
            protected void done() {
                try {
                    reportsAnalyticsView.setReports(get());
                    if (successMessage != null) {
                        reportsAnalyticsView.showMessage(successMessage);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reportsAnalyticsView.setReports(null);
                    reportsAnalyticsView.showError("Loading reports was interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Reports and analytics could not be loaded.", e.getCause());
                    reportsAnalyticsView.setReports(null);
                    reportsAnalyticsView.showError(
                        "Unable to load reports from the database. Check connectivity and retry."
                    );
                } finally {
                    reportsAnalyticsView.setLoading(false);
                }
            }
        }.execute();
    }

    private void loadDemoData() {
        if (reportsAnalyticsView == null || !recruiterSession.isAuthenticated()) {
            return;
        }
        reportsAnalyticsView.setLoading(true);
        int recruiterId = recruiterSession.getCurrentRecruiter().getId();
        new SwingWorker<DemoLoadOutcome, Void>() {
            @Override
            protected DemoLoadOutcome doInBackground() throws Exception {
                DemoDataService.LoadResult result = demoDataService.load(recruiterId);
                Integer preferredJobId = databaseService.getJobsForRecruiter(recruiterId).stream()
                    .filter(job -> job.getTitle().startsWith("[DEMO] "))
                    .map(Job::getId)
                    .findFirst()
                    .orElse(null);
                return new DemoLoadOutcome(result, preferredJobId);
            }

            @Override
            protected void done() {
                try {
                    DemoLoadOutcome outcome = get();
                    refreshPhaseFiveData(outcome.preferredJobId());
                    refreshDashboard();
                    loadReports("Demo data ready: " + outcome.result().jobsCreated() + " jobs, "
                        + outcome.result().resumesCreated() + " fictional candidate/resume profiles, and "
                        + outcome.result().screeningsCreated()
                        + " screenings created; existing demo records reused safely.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reportsAnalyticsView.setLoading(false);
                    reportsAnalyticsView.showError("Loading demo data was interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Fictional demo data could not be loaded.", e.getCause());
                    reportsAnalyticsView.setLoading(false);
                    reportsAnalyticsView.showError(
                        "Unable to load demo data. Existing records were not overwritten; check the application log."
                    );
                }
            }
        }.execute();
    }

    private void removeDemoData() {
        if (reportsAnalyticsView == null || !recruiterSession.isAuthenticated()) {
            return;
        }
        reportsAnalyticsView.setLoading(true);
        int recruiterId = recruiterSession.getCurrentRecruiter().getId();
        new SwingWorker<DemoDataService.RemoveResult, Void>() {
            @Override
            protected DemoDataService.RemoveResult doInBackground() throws Exception {
                return demoDataService.remove(recruiterId);
            }

            @Override
            protected void done() {
                try {
                    DemoDataService.RemoveResult result = get();
                    refreshPhaseFiveData();
                    refreshDashboard();
                    loadReports("Demo cleanup complete: " + result.jobsDeleted() + " jobs and "
                        + result.candidatesDeleted() + " demo-only candidates removed; "
                        + result.candidatesRetained() + " candidate(s) retained for non-demo screenings.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reportsAnalyticsView.setLoading(false);
                    reportsAnalyticsView.showError("Removing demo data was interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Fictional demo data cleanup failed.", e.getCause());
                    reportsAnalyticsView.setLoading(false);
                    reportsAnalyticsView.showError(
                        "Unable to remove demo data. Check the application log."
                    );
                }
            }
        }.execute();
    }

    private void exportReports(Path destination, boolean pdf) {
        if (reportsAnalyticsView == null || reportsAnalyticsView.getSnapshot() == null) {
            return;
        }
        reportsAnalyticsView.setLoading(true);
        var snapshot = reportsAnalyticsView.getSnapshot();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ReportExportService exporter = new ReportExportService();
                if (pdf) {
                    exporter.exportPdf(destination, snapshot);
                } else {
                    exporter.exportCsv(destination, snapshot);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    reportsAnalyticsView.showMessage(
                        (pdf ? "PDF" : "CSV") + " report exported successfully to " + destination
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reportsAnalyticsView.showError("Report export was interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Could not export report to {}.", destination, e.getCause());
                    reportsAnalyticsView.showError(
                        "Unable to export the report. Check the destination path and available disk space."
                    );
                } finally {
                    reportsAnalyticsView.setLoading(false);
                }
            }
        }.execute();
    }

    private void createJob(Job job, int recruiterId) {
            job.setRecruiterId(recruiterId);
            jobManagementView.showMessage("Saving job...", false);
            new SwingWorker<Job, Void>() {
                @Override
                protected Job doInBackground() throws Exception {
                    return databaseService.createJob(job);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        jobManagementView.showMessage("Job saved successfully.", false);
                        refreshPhaseFiveData();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        jobManagementView.showMessage("Job saving was interrupted.", true);
                    } catch (ExecutionException e) {
                        logger.error("Could not save recruiter job.", e.getCause());
                        jobManagementView.showMessage("Unable to save the job. Check the database connection and required fields.", true);
                    }
                }
            }.execute();
        }

    private void uploadResume(ResumeWorkspaceView.UploadRequest request) {
            resumeWorkspaceView.showStatus("Validating and extracting resume text...", false);
            new SwingWorker<UploadOutcome, Void>() {
                @Override
                protected UploadOutcome doInBackground() throws Exception {
                    resumeUploadService.upload(request.file(), request.candidate());
                    List<ScreeningEntry> screenings = List.of();
                    String screeningError = null;
                    if (request.jobId() != null) {
                        try {
                            screenings = resumeScreeningService.screen(
                                recruiterSession.getCurrentRecruiter().getId(), request.jobId()
                            );
                        } catch (SQLException e) {
                            logger.error("Resume was stored, but automatic screening failed.", e);
                            screeningError = " The resume was uploaded, but automatic screening could not be saved.";
                        }
                    }
                    return new UploadOutcome(screenings, screeningError);
                }

                @Override
                protected void done() {
                    try {
                        UploadOutcome outcome = get();
                        resumeWorkspaceView.setScreeningResults(outcome.screenings());
                        resumeWorkspaceView.showStatus(
                            outcome.screeningError() == null
                                ? "Resume uploaded and validated successfully."
                                : "Resume uploaded successfully." + outcome.screeningError(),
                            outcome.screeningError() != null
                        );
                        refreshPhaseFiveData();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        resumeWorkspaceView.showStatus("Resume upload was interrupted.", true);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof PdfResumeParser.ResumeValidationException validation) {
                            resumeWorkspaceView.showStatus(validation.getMessage(), true);
                        } else if (cause instanceof ResumeUploadService.DuplicateResumeException duplicate) {
                            resumeWorkspaceView.showStatus(duplicate.getMessage(), true);
                        } else if (cause instanceof IllegalArgumentException validation) {
                            resumeWorkspaceView.showStatus(validation.getMessage(), true);
                        } else {
                            logger.error("Resume upload failed.", cause);
                            resumeWorkspaceView.showStatus(
                                "Unable to save the resume. Check database and storage access, then try again.", true
                            );
                        }
                    }
                }
            }.execute();
        }

    private void screenResumes(int jobId) {
            resumeWorkspaceView.showStatus("Screening stored resumes against the selected job...", false);
            new SwingWorker<List<ScreeningEntry>, Void>() {
                @Override
                protected List<ScreeningEntry> doInBackground() throws Exception {
                    return resumeScreeningService.screen(recruiterSession.getCurrentRecruiter().getId(), jobId);
                }

                @Override
                protected void done() {
                    try {
                        List<ScreeningEntry> results = get();
                        resumeWorkspaceView.setScreeningResults(results);
                        resumeWorkspaceView.showStatus(
                            results.isEmpty() ? "No resumes are available to screen." : "Screening results saved.", false
                        );
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        resumeWorkspaceView.showStatus("Screening was interrupted.", true);
                    } catch (ExecutionException e) {
                        logger.error("Resume screening failed.", e.getCause());
                        resumeWorkspaceView.showStatus("Unable to screen resumes. Check database connectivity and retry.", true);
                    }
                }
            }.execute();
        }

    private void refreshPhaseFiveData() {
        refreshPhaseFiveData(null);
    }

    private void refreshPhaseFiveData(Integer preferredJobId) {
            Integer selectedJobId = resumeWorkspaceView.getSelectedJobId();
            new SwingWorker<PhaseFiveData, Void>() {
                @Override
                protected PhaseFiveData doInBackground() throws Exception {
                    List<Job> jobs = databaseService.getJobsForRecruiter(recruiterSession.getCurrentRecruiter().getId());
                    Integer jobId = jobs.stream().anyMatch(job ->
                        job.getId() == (preferredJobId == null ? -1 : preferredJobId))
                        ? preferredJobId
                        : jobs.stream().anyMatch(job -> job.getId() == (selectedJobId == null ? -1 : selectedJobId))
                            ? selectedJobId : jobs.isEmpty() ? null : jobs.get(0).getId();
                    List<ScreeningEntry> results = jobId == null
                        ? List.of() : databaseService.getLatestScreeningResults(jobId);
                    return new PhaseFiveData(
                        jobs, databaseService.getResumes(), databaseService.getCandidates(), results, jobId
                    );
                }

                @Override
                protected void done() {
                    try {
                        PhaseFiveData data = get();
                        jobManagementView.setJobs(data.jobs());
                        resumeWorkspaceView.setJobs(data.jobs(), data.selectedJobId());
                        resumeWorkspaceView.setResumes(data.resumes());
                        resumeWorkspaceView.setScreeningResults(data.screenings());
                        candidateDirectoryView.setCandidates(data.candidates());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        resumeWorkspaceView.showStatus("Loading saved resumes was interrupted.", true);
                    } catch (ExecutionException e) {
                        logger.error("Phase 5 data could not be loaded.", e.getCause());
                        resumeWorkspaceView.showStatus("Unable to load jobs or resumes from the database.", true);
                        jobManagementView.showMessage("Unable to load saved jobs from the database.", true);
                    }
                }
            }.execute();
        }

    private void loadScreeningResults(int jobId) {
        new SwingWorker<List<ScreeningEntry>, Void>() {
            @Override
            protected List<ScreeningEntry> doInBackground() throws Exception {
                return databaseService.getLatestScreeningResults(jobId);
            }

            @Override
            protected void done() {
                try {
                    List<ScreeningEntry> results = get();
                    if (Integer.valueOf(jobId).equals(resumeWorkspaceView.getSelectedJobId())) {
                        resumeWorkspaceView.setScreeningResults(results);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    resumeWorkspaceView.showStatus("Loading this job's screening results was interrupted.", true);
                } catch (ExecutionException e) {
                    logger.error("Selected job screening results could not be loaded.", e.getCause());
                    resumeWorkspaceView.showStatus(
                        "Unable to load rankings for the selected job. Check database connectivity and retry.", true
                    );
                }
            }
        }.execute();
    }

    private void refreshDashboard() {
        if (dashboardView == null || !recruiterSession.isAuthenticated()) {
            return;
        }
        int recruiterId = recruiterSession.getCurrentRecruiter().getId();
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                List<Job> jobs = databaseService.getJobsForRecruiter(recruiterId);
                Map<Integer, Integer> screenedCounts = new LinkedHashMap<>();
                for (Job job : jobs) {
                    screenedCounts.put(job.getId(), databaseService.getScreenedResumeCountForJob(job.getId()));
                }
                return new DashboardData(
                    databaseService.getDashboardMetrics(recruiterId), jobs, screenedCounts,
                    databaseService.getRecentScreeningsForRecruiter(recruiterId, 5)
                );
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    dashboardView.setDashboardData(
                        data.metrics(), data.jobs(), data.screenedCounts(), data.recentScreenings()
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Dashboard data refresh was interrupted.");
                } catch (ExecutionException e) {
                    logger.error("Dashboard could not refresh current database data.", e.getCause());
                }
            }
        }.execute();
    }

    private void deleteCandidate(int candidateId) {
        candidateDirectoryView.setDeleteInProgress(true);
        new SwingWorker<CandidateDeletionOutcome, Void>() {
            @Override
            protected CandidateDeletionOutcome doInBackground() throws Exception {
                List<String> unreferencedFiles = databaseService.deleteCandidate(candidateId);
                ResumeUploadService.FileCleanupResult cleanup =
                    resumeUploadService.removeOrphanedFiles(unreferencedFiles);
                return new CandidateDeletionOutcome(cleanup.deleted(), cleanup.retained());
            }

            @Override
            protected void done() {
                try {
                    CandidateDeletionOutcome outcome = get();
                    String message = "Candidate and associated records deleted. "
                        + outcome.filesDeleted() + " unreferenced resume file(s) removed.";
                    if (outcome.filesRetained() > 0) {
                        message += " " + outcome.filesRetained()
                            + " file(s) were retained because they are outside managed storage or could not be removed.";
                    }
                    candidateDirectoryView.showMessage(message, outcome.filesRetained() > 0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    candidateDirectoryView.showMessage("Candidate deletion was interrupted.", true);
                } catch (ExecutionException e) {
                    logger.error("Candidate deletion failed.", e.getCause());
                    candidateDirectoryView.showMessage(
                        "Unable to delete the candidate. Check database connectivity and retry.", true
                    );
                } finally {
                    candidateDirectoryView.setDeleteInProgress(false);
                    refreshPhaseFiveData();
                }
            }
        }.execute();
    }

    private record PhaseFiveData(List<Job> jobs, List<com.resumescreening.models.ResumeEntry> resumes,
                                 List<Candidate> candidates,
                                 List<ScreeningEntry> screenings, Integer selectedJobId) {
    }

    private record UploadOutcome(List<ScreeningEntry> screenings, String screeningError) {
    }

    private record DemoLoadOutcome(DemoDataService.LoadResult result, Integer preferredJobId) {
    }

    private record DashboardData(com.resumescreening.models.DashboardMetrics metrics, List<Job> jobs,
                                  Map<Integer, Integer> screenedCounts,
                                  List<ScreeningEntry> recentScreenings) {
    }

    private record CandidateDeletionOutcome(int filesDeleted, int filesRetained) {
    }

    private void logout() {
        recruiterSession.logout();
        if (applicationPanel != null) {
            rootCards.remove(applicationPanel);
            applicationPanel = null;
            headerPanel = null;
            sidebarPanel = null;
            contentCards = null;
            contentLayout = null;
        }
        authenticationView.showLogin(null, "You have been signed out.");
        rootLayout.show(rootCards, LOGIN_CARD);
        rootCards.revalidate();
        rootCards.repaint();
    }

    public static void launchApp() {
        UITheme.setupTheme();
        SwingUtilities.invokeLater(() -> {
            DatabaseService databaseService = new DatabaseService();
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return databaseService.applyPhaseFiveMigrations();
                }

                @Override
                protected void done() {
                    boolean ready;
                    try {
                        ready = get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ready = false;
                    } catch (ExecutionException e) {
                        logger.error("Phase 5 database migration could not be applied.", e.getCause());
                        ready = false;
                    }
                    MainFrame frame = new MainFrame(databaseService, ready);
                    frame.setVisible(true);
                }
            }.execute();
        });
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public SidebarPanel getSidebarPanel() {
        return sidebarPanel;
    }

    public boolean isAuthenticated() {
        return recruiterSession.isAuthenticated();
    }
}
