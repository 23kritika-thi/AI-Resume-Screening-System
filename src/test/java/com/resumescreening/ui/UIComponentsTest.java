package com.resumescreening.ui;

import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.HeaderPanel;
import com.resumescreening.ui.components.NavItemButton;
import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.components.SecondaryButton;
import com.resumescreening.ui.components.SidebarPanel;
import com.resumescreening.ui.components.StatCard;
import com.resumescreening.ui.components.UIIcon;
import com.resumescreening.ui.theme.UITheme;
import com.resumescreening.ui.views.DashboardView;
import com.resumescreening.ui.views.AuthenticationView;
import com.resumescreening.ui.views.PlaceholderView;
import com.resumescreening.ui.views.JobManagementView;
import com.resumescreening.ui.views.ResumeWorkspaceView;
import com.resumescreening.ui.views.CandidateDirectoryView;
import com.resumescreening.ui.views.SettingsView;
import com.resumescreening.models.Candidate;
import com.resumescreening.models.DashboardMetrics;
import com.resumescreening.models.Job;
import com.resumescreening.models.Recruiter;
import com.resumescreening.models.Resume;
import com.resumescreening.models.ResumeEntry;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.models.ScreeningResult;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UIComponentsTest {

    @BeforeAll
    static void initTheme() {
        UITheme.setupTheme();
    }

    @Test
    @DisplayName("Theme design tokens and fonts should be defined")
    void testThemeConstants() {
        assertNotNull(UITheme.PRIMARY);
        assertNotNull(UITheme.SIDEBAR_BG);
        assertNotNull(UITheme.BG_CANVAS);
        assertNotNull(UITheme.FONT_TITLE);
        assertNotNull(UITheme.FONT_STAT_VALUE);
        assertEquals(new Color(37, 99, 235), UITheme.PRIMARY);
    }

    @Test
    @DisplayName("StatCard should instantiate and hold metrics cleanly")
    void testStatCardCreation() {
        StatCard card = new StatCard("Total Jobs", "12", "+2 this week", UIIcon.Kind.BRIEFCASE, UITheme.PRIMARY, UITheme.PRIMARY_LIGHT);
        assertNotNull(card);
        assertEquals(3, card.getComponentCount()); // North (top), Center (val), South (trend)
        card.setSize(card.getPreferredSize());
        card.doLayout();
        JLabel valueLabel = (JLabel) ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertTrue(valueLabel.getHeight() >= valueLabel.getPreferredSize().height);
    }

    @Test
    @DisplayName("ContentCard should set header and body component")
    void testContentCard() {
        ContentCard card = new ContentCard("Active Jobs", "Overview of open listings");
        assertNotNull(card);
        card.setBodyComponent(new JLabel("Test Body"));
        assertNotNull(card.getContentPanel());
        assertEquals(1, card.getContentPanel().getComponentCount());
    }

    @Test
    @DisplayName("Buttons should initialize with proper styling attributes")
    void testButtons() {
        PrimaryButton primary = new PrimaryButton("Save");
        assertEquals("Save", primary.getText());
        assertTrue(primary.isCursorSet());

        SecondaryButton secondary = new SecondaryButton("Cancel");
        assertEquals("Cancel", secondary.getText());
    }

    @Test
    @DisplayName("NavItemButton should toggle active state correctly")
    void testNavItemButtonState() {
        NavItemButton btn = new NavItemButton("JOBS", "Jobs", UIIcon.Kind.BRIEFCASE);
        assertEquals("JOBS", btn.getNavId());
        assertFalse(btn.isActive());

        btn.setActive(true);
        assertTrue(btn.isActive());
        assertEquals(UITheme.SIDEBAR_TEXT_ACTIVE, btn.getForeground());

        btn.setActive(false);
        assertFalse(btn.isActive());
        assertEquals(UITheme.SIDEBAR_TEXT, btn.getForeground());
    }

    @Test
    @DisplayName("SidebarPanel should register navigation callback and support all 6 tabs")
    void testSidebarNavigation() {
        SidebarPanel sidebar = new SidebarPanel();
        assertNotNull(sidebar);

        AtomicReference<String> navigatedTo = new AtomicReference<>();
        sidebar.setNavigationListener(navigatedTo::set);

        sidebar.selectNav(SidebarPanel.NAV_JOBS);
        assertEquals(SidebarPanel.NAV_JOBS, navigatedTo.get());

        sidebar.selectNav(SidebarPanel.NAV_SCREENING);
        assertEquals(SidebarPanel.NAV_SCREENING, navigatedTo.get());

        sidebar.selectNav(SidebarPanel.NAV_REPORTS);
        assertEquals(SidebarPanel.NAV_REPORTS, navigatedTo.get());

        sidebar.selectNav(SidebarPanel.NAV_SETTINGS);
        assertEquals(SidebarPanel.NAV_SETTINGS, navigatedTo.get());
    }

    @Test
    @DisplayName("HeaderPanel should update page titles dynamically")
    void testHeaderPanel() {
        HeaderPanel header = new HeaderPanel();
        assertNotNull(header);
        header.setPageTitle("Job Openings", "Manage open listings");
        AtomicReference<Boolean> signedOut = new AtomicReference<>(false);
        header.addLogoutListener(event -> signedOut.set(true));
        JButton logoutButton = findButton(header, "Sign out");
        assertNotNull(logoutButton);
        logoutButton.doClick();
        assertTrue(signedOut.get());
    }

    @Test
    @DisplayName("DashboardView should construct metric cards and pipeline tables")
    void testDashboardView() {
        DashboardView view = new DashboardView();
        assertNotNull(view);
        assertEquals(1, view.getComponentCount()); // Contains JScrollPane

        Job demoJob = new Job(7, 42, "[DEMO] Java Developer", "Engineering", "Build services",
            "Java, Spring Boot", 2, "OPEN", null, null);
        Candidate demoCandidate = new Candidate(84, "Morgan Lee", "morgan@example.test",
            "202-555-0101", null, null);
        Resume demoResume = new Resume(95, 84, "DEMO-morgan-lee.pdf", "storage/demo.pdf",
            "Java Spring Boot", "demo-hash", null);
        ScreeningResult demoResult = new ScreeningResult();
        demoResult.setJobId(42);
        demoResult.setCandidateId(84);
        demoResult.setResumeId(95);
        demoResult.setMatchScore(new BigDecimal("88.50"));

        view.setDashboardData(
            new DashboardMetrics(8, 18, 18, 32),
            List.of(demoJob),
            Map.of(42, 4),
            List.of(new ScreeningEntry(demoCandidate, demoResume, demoResult))
        );

        assertEquals("8", view.getMetricValueAt(0));
        assertEquals("18", view.getMetricValueAt(1));
        assertEquals("18", view.getMetricValueAt(2));
        assertEquals("32", view.getMetricValueAt(3));
        assertEquals(1, view.getJobRowCount());
        assertEquals("[DEMO] Java Developer", view.getJobTitleAt(0));
        assertEquals(List.of("Morgan Lee"), view.getRecentCandidateNames());
    }

    @Test
    @DisplayName("PlaceholderView should construct informative module screens")
    void testPlaceholderView() {
        PlaceholderView placeholder = new PlaceholderView("Jobs", "Manage jobs", UIIcon.Kind.BRIEFCASE, "Phase 4");
        assertNotNull(placeholder);
    }

    @Test
    @DisplayName("Settings profile controls validate, save, reset, and invoke connection check")
    void testSettingsViewControls() {
        Recruiter recruiter = new Recruiter(5, "recruiter-test", null, "Current Recruiter",
            "recruiter@example.test", null, null);
        SettingsView settings = new SettingsView(recruiter, new com.resumescreening.database.DatabaseConfig());
        AtomicReference<String> savedName = new AtomicReference<>();
        AtomicReference<Boolean> databaseTested = new AtomicReference<>(false);
        settings.setProfileSaveHandler(savedName::set);
        settings.setDatabaseTestHandler(() -> databaseTested.set(true));

        settings.getFullNameField().setText("New Recruiter Name");
        settings.getSaveButton().doClick();
        assertEquals("New Recruiter Name", savedName.get());
        settings.showProfileSaved(savedName.get());

        settings.getFullNameField().setText(" ");
        settings.getSaveButton().doClick();
        assertTrue(settings.getStatusMessage().contains("between 2 and 100"));
        assertEquals("New Recruiter Name", savedName.get());

        settings.getFullNameField().setText("Unsaved Name");
        settings.getResetButton().doClick();
        assertEquals("New Recruiter Name", settings.getFullNameField().getText());
        assertTrue(settings.getStatusMessage().contains("discarded"));
        assertEquals("New Recruiter Name", savedName.get(),
            "Reset must not submit or persist profile changes.");

        com.resumescreening.database.DatabaseConfig config = new com.resumescreening.database.DatabaseConfig();
        config.setPassword("settings-test-secret");
        settings.setDatabaseConfig(config);
        settings.getTestConnectionButton().doClick();
        assertTrue(databaseTested.get());
        assertTrue(settings.getDatabaseTargetText().contains(config.getHost()));
        assertTrue(settings.getDatabaseTargetText().contains(config.getDatabaseName()));
        assertTrue(settings.getDatabaseTargetText().contains(config.getUsername()));
        assertFalse(settings.getDatabaseTargetText().contains(config.getPassword()));
        assertEquals("Connection not checked.", new SettingsView(recruiter,
            config).getDatabaseStatusText());
        settings.showDatabaseTestResult(true);
        assertTrue(settings.getStatusMessage().contains("Connected"));
        assertEquals("Connected to MySQL.", settings.getDatabaseStatusText());
        settings.showDatabaseTestResult(false);
        assertTrue(settings.getStatusMessage().contains("Not connected"));
        assertTrue(settings.getDatabaseStatusText().startsWith("Not connected."));
        settings.showDatabaseTestFailure("Database connection test was interrupted.");
        assertEquals("Database connection test was interrupted.", settings.getDatabaseStatusText());
    }

    @Test
    @DisplayName("Phase 5 screens expose resume-only upload and job criteria controls")
    void testPhaseFiveViews() {
        ResumeWorkspaceView resumes = new ResumeWorkspaceView();
        assertNotNull(findLabel(resumes, "Upload your resume in PDF format only."));
        assertEquals(5, resumes.getResumesTable().getColumnCount());
        assertEquals(7, resumes.getScreeningTable().getColumnCount());
        AtomicReference<Integer> selectedJob = new AtomicReference<>();
        resumes.setJobSelectionHandler(selectedJob::set);
        resumes.setJobs(List.of(
            new Job(10, 1, "Existing Job", "Engineering", "Build services", "Java", 1,
                "OPEN", null, null),
            new Job(20, 1, "[DEMO] Java Developer", "Engineering", "Build services", "Java", 1,
                "OPEN", null, null)
        ));
        resumes.selectJob(20);
        assertEquals(20, selectedJob.get());
        assertEquals(20, resumes.getSelectedJobId());
        Candidate rankedDemoCandidate = new Candidate(84, "Morgan Lee", "morgan@example.test",
            "202-555-0101", null, null);
        Resume rankedDemoResume = new Resume(95, 84, "DEMO-morgan-lee.pdf", "storage/demo.pdf",
            "Java Spring Boot", "demo-hash", null);
        ScreeningResult rankedDemoResult = new ScreeningResult();
        rankedDemoResult.setJobId(20);
        rankedDemoResult.setCandidateId(84);
        rankedDemoResult.setResumeId(95);
        rankedDemoResult.setMatchScore(new BigDecimal("88.50"));
        rankedDemoResult.setRanking(1);
        resumes.setScreeningResults(List.of(
            new ScreeningEntry(rankedDemoCandidate, rankedDemoResume, rankedDemoResult)
        ));
        assertEquals(1, resumes.getScreeningTable().getRowCount());
        assertEquals("Morgan Lee", resumes.getScreeningTable().getValueAt(0, 1));
        assertEquals(1, resumes.getScreeningTable().getValueAt(0, 0));

        JobManagementView jobs = new JobManagementView();
        assertEquals(6, jobs.getJobsTable().getColumnCount());
        assertEquals(JTable.AUTO_RESIZE_OFF, jobs.getJobsTable().getAutoResizeMode());
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED,
            jobs.getJobsScrollPane().getHorizontalScrollBarPolicy());
        jobs.setJobs(List.of(
            new Job(1, "Backend Engineer", "Engineering", "Build services", "Java", 2),
            new Job(1, "Data Engineer", "Platform", "Build pipelines", "SQL", 1)
        ));
        assertEquals(2, jobs.getJobsTable().getRowCount());

        CandidateDirectoryView candidates = new CandidateDirectoryView();
        Candidate candidate = new Candidate(1, "Jane Doe", "jane@example.test", "555-0123",
            "https://example.test/jane", null);
        candidates.setCandidates(List.of(candidate));
        assertEquals(1, candidates.getCandidatesTable().getRowCount());
        assertEquals("jane@example.test", candidates.getCandidatesTable().getValueAt(0, 1));
        assertFalse(candidates.getDeleteButton().isEnabled());

        Candidate existing = new Candidate(2, "Alex Smith", "alex@example.test", null, null, null);
        String extractedText = "Alex Smith\nEmail: alex@example.test\nSkills\nJava\nEducation\n"
            + "Bachelor of Science\nWork Experience\n"
            + "Backend engineer with Java and Spring Boot experience.\n".repeat(80);
        Resume existingResume = new Resume(1, 2, "alex.pdf", "storage/alex.pdf", extractedText, "hash", null);
        resumes.setResumes(List.of(new ResumeEntry(existing, existingResume)));
        resumes.getResumesTable().setRowSelectionInterval(0, 0);
        assertEquals("alex@example.test", resumes.getResumesTable().getValueAt(0, 1));
        assertTrue(resumes.getResumeDetailsTextArea().getText().length() > 2000);
        assertTrue(resumes.getResumeDetailsTextArea().getLineWrap());
        assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            resumes.getResumeDetailsScrollPane().getVerticalScrollBarPolicy());
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
            resumes.getResumeDetailsScrollPane().getHorizontalScrollBarPolicy());
        JScrollPane detailScroll = resumes.getResumeDetailsScrollPane();
        detailScroll.setSize(360, 120);
        layoutTree(detailScroll);
        JScrollBar verticalBar = detailScroll.getVerticalScrollBar();
        assertTrue(verticalBar.getMaximum() > verticalBar.getVisibleAmount());
        verticalBar.setValue(verticalBar.getMaximum());
        assertTrue(verticalBar.getValue() > 0);
        assertNotSame(candidates.getCandidatesTable(), resumes.getResumesTable());
    }

    @Test
    @DisplayName("Candidate deletion is confirmed and cancellation leaves the candidate unchanged")
    void candidateDeletionCanBeCancelled() {
        CandidateDirectoryView candidates = new CandidateDirectoryView() {
            @Override
            protected boolean confirmDeletion(Candidate candidate) {
                return false;
            }
        };
        candidates.setCandidates(List.of(
            new Candidate(42, "Candidate To Keep", "keep@example.test", null, null, null)
        ));
        AtomicReference<Integer> deletedId = new AtomicReference<>();
        candidates.setDeleteHandler(deletedId::set);

        candidates.getCandidatesTable().setRowSelectionInterval(0, 0);
        assertTrue(candidates.getDeleteButton().isEnabled());
        candidates.getDeleteButton().doClick();

        assertEquals(1, candidates.getCandidatesTable().getRowCount());
        assertNull(deletedId.get());
    }

    @Test
    @DisplayName("Confirmed candidate deletion targets the selected record")
    void candidateDeletionTargetsSelectedCandidate() {
        CandidateDirectoryView candidates = new CandidateDirectoryView() {
            @Override
            protected boolean confirmDeletion(Candidate candidate) {
                return true;
            }
        };
        candidates.setCandidates(List.of(
            new Candidate(41, "First Candidate", "first@example.test", null, null, null),
            new Candidate(42, "Second Candidate", "second@example.test", null, null, null)
        ));
        AtomicReference<Integer> deletedId = new AtomicReference<>();
        candidates.setDeleteHandler(deletedId::set);

        candidates.getCandidatesTable().setRowSelectionInterval(1, 1);
        candidates.getDeleteButton().doClick();

        assertEquals(42, deletedId.get());
    }

    @Test
    @DisplayName("Login should expose a Forgot Password link that opens recovery")
    void testForgotPasswordLink() {
        AuthenticationView view = new AuthenticationView();
        JButton forgotPassword = findButton(view, "Forgot Password?");

        assertNotNull(forgotPassword);
        forgotPassword.doClick();
        view.showRecoveryEmailMessage("Email configuration is unavailable.", true);
        view.showRecoveryCodeStep("If an account exists, a code has been requested.");
        view.showRecoveryPasswordStep("Code verified.");
        view.showRecoveryStepMessage("New passwords do not match.");
    }

    @Test
    @DisplayName("Vector icons should paint at the requested size")
    void testVectorIconRendering() {
        UIIcon icon = new UIIcon(UIIcon.Kind.DASHBOARD, 24, UITheme.PRIMARY);
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics graphics = image.getGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();

        assertEquals(24, icon.getIconWidth());
        assertEquals(24, icon.getIconHeight());
        assertTrue(hasPaintedPixel(image));
    }

    @Test
    @DisplayName("Dashboard content should track its viewport width")
    void testDashboardTracksViewportWidth() {
        DashboardView view = new DashboardView();
        JScrollPane scrollPane = (JScrollPane) view.getComponent(0);
        scrollPane.setSize(700, 500);
        scrollPane.doLayout();
        scrollPane.getViewport().doLayout();

        assertEquals(scrollPane.getViewport().getWidth(), scrollPane.getViewport().getView().getWidth());
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scrollPane.getHorizontalScrollBarPolicy());
    }

    @Test
    @DisplayName("Dashboard metric cards should reflow as the viewport changes width")
    void testDashboardMetricsReflow() {
        DashboardView view = new DashboardView();
        JScrollPane scrollPane = (JScrollPane) view.getComponent(0);

        scrollPane.setSize(700, 500);
        layoutTree(scrollPane);
        Container metrics = findMetricsPanel(scrollPane.getViewport().getView());
        assertNotNull(metrics);
        Component[] cards = metrics.getComponents();
        Point secondCardPosition = cards[1].getLocation();
        Point thirdCardPosition = cards[2].getLocation();

        assertTrue(secondCardPosition.x > cards[0].getX());
        assertEquals(cards[0].getX(), thirdCardPosition.x);
        assertTrue(cards[0].getWidth() >= 220);

        scrollPane.setSize(1200, 700);
        layoutTree(scrollPane);
        metrics = findMetricsPanel(scrollPane.getViewport().getView());
        assertNotNull(metrics);
        cards = metrics.getComponents();

        assertTrue(cards[3].getX() > cards[2].getX());
        assertTrue(cards[3].getX() + cards[3].getWidth() <= metrics.getWidth());
    }

    private boolean hasPaintedPixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                layoutTree((Container) component);
            }
        }
    }

    private Container findMetricsPanel(Component component) {
        if (component instanceof Container) {
            Container container = (Container) component;
            Component[] children = container.getComponents();
            if (children.length == 4 && children[0] instanceof StatCard) {
                return container;
            }
            for (Component child : children) {
                Container metrics = findMetricsPanel(child);
                if (metrics != null) {
                    return metrics;
                }
            }
        }
        return null;
    }

    private JButton findButton(Component component, String text) {
        if (component instanceof JButton && text.equals(((JButton) component).getText())) {
            return (JButton) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                JButton button = findButton(child, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private JLabel findLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel label = findLabel(child, text);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    @Test
    @DisplayName("MainFrame should construct successfully when not headless")
    void testMainFrameInstantiation() {
        if (!GraphicsEnvironment.isHeadless()) {
            MainFrame frame = new MainFrame();
            assertNotNull(frame);
            assertFalse(frame.isAuthenticated());
            assertNull(frame.getHeaderPanel());
            assertNull(frame.getSidebarPanel());
            assertEquals(1, frame.getContentPane().getComponentCount());
            assertEquals("AI-Based Resume Screening and Candidate Ranking System", frame.getTitle());
            frame.dispose();
        }
    }
}
