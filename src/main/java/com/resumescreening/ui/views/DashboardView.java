package com.resumescreening.ui.views;

import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.components.SecondaryButton;
import com.resumescreening.ui.components.StatCard;
import com.resumescreening.ui.components.UIIcon;
import com.resumescreening.ui.components.SidebarPanel;
import com.resumescreening.ui.theme.UITheme;
import com.resumescreening.models.DashboardMetrics;
import com.resumescreening.models.Job;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.services.DatabaseService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Main dashboard screen displaying summary statistics, quick action shortcuts,
 * active job openings, and recent candidate screening activities.
 */
public class DashboardView extends JPanel {

    private final DatabaseService databaseService;
    private final int recruiterId;
    private final String recruiterName;
    private JLabel welcomeTitle;
    private final Consumer<String> navigationHandler;
    private final List<StatCard> metricCards = new ArrayList<>();
    private List<String> recentCandidateNames = List.of();
    private final DefaultTableModel jobsModel = new DefaultTableModel(
        new Object[]{"Job Title", "Department", "Resumes screened", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JPanel recentActivityList;

    public DashboardView() {
        this(null, 0, null, null);
    }

    public DashboardView(DatabaseService databaseService, int recruiterId, String recruiterName,
                         Consumer<String> navigationHandler) {
        this.databaseService = databaseService;
        this.recruiterId = recruiterId;
        this.recruiterName = recruiterName;
        this.navigationHandler = navigationHandler;
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        // Scrollable content panel
        JPanel contentPanel = new DashboardContentPanel();
        contentPanel.setBackground(UITheme.BG_CANVAS);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        // 1. Greeting & Welcome Banner
        contentPanel.add(buildWelcomeSection());
        contentPanel.add(Box.createVerticalStrut(20));

        // 2. Metrics / Stat Cards Row
        contentPanel.add(buildMetricsRow());
        contentPanel.add(Box.createVerticalStrut(20));

        // 3. Quick Actions Toolbar
        contentPanel.add(buildQuickActionsBar());
        contentPanel.add(Box.createVerticalStrut(20));

        // 4. Lower Cards Grid (Active Jobs Table + Recent Screenings)
        contentPanel.add(buildLowerGrid());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(UITheme.BG_CANVAS);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildWelcomeSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        welcomeTitle = new JLabel(recruiterName == null || recruiterName.isBlank()
            ? "Welcome to ResumeAI" : "Welcome back, " + recruiterName);
        welcomeTitle.setFont(UITheme.FONT_TITLE);
        welcomeTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Hiring pipeline, resume screening queue, and top candidates.");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        textPanel.add(welcomeTitle);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    public void setRecruiterName(String name) {
        if (welcomeTitle != null) {
            welcomeTitle.setText(name == null || name.isBlank()
                ? "Welcome to ResumeAI" : "Welcome back, " + name.trim());
        }
    }

    private JPanel buildMetricsRow() {
        ResponsiveGridPanel row = new ResponsiveGridPanel(4, 220, 16, 16);

        DashboardMetrics metrics = loadMetrics();
        StatCard jobsCard = new StatCard(
            "Total Jobs",
            metric(metrics, 0),
            "Saved job descriptions",
            UIIcon.Kind.BRIEFCASE,
            UITheme.PRIMARY,
            UITheme.PRIMARY_LIGHT
        );

        // 2. Total Candidates
        StatCard candidatesCard = new StatCard(
            "Candidates with Resumes",
            metric(metrics, 1),
            "Distinct uploaded candidate profiles",
            UIIcon.Kind.PEOPLE,
            UITheme.INFO,
            UITheme.INFO_LIGHT
        );

        // 3. Resumes Screened
        StatCard screenedCard = new StatCard(
            "Uploaded Resumes",
            metric(metrics, 2),
            "Validated PDF resumes",
            UIIcon.Kind.DOCUMENT,
            UITheme.PURPLE,
            UITheme.PURPLE_LIGHT
        );

        // 4. Shortlisted Candidates
        StatCard shortlistedCard = new StatCard(
            "Screening Results",
            metric(metrics, 3),
            "Stored score evaluations",
            UIIcon.Kind.STAR,
            UITheme.SUCCESS,
            UITheme.SUCCESS_LIGHT
        );

        metricCards.add(jobsCard);
        metricCards.add(candidatesCard);
        metricCards.add(screenedCard);
        metricCards.add(shortlistedCard);
        metricCards.forEach(row::add);

        return row;
    }

    private JPanel buildQuickActionsBar() {
        ResponsiveGridPanel bar = new ResponsiveGridPanel(4, 220, 12, 8);

        PrimaryButton postJobBtn = new PrimaryButton("+ Post New Job");
        SecondaryButton uploadResumeBtn = createActionButton("Upload Resumes", UIIcon.Kind.UPLOAD);
        SecondaryButton runScreeningBtn = createActionButton("Run AI Screening", UIIcon.Kind.BOLT);
        SecondaryButton exportReportBtn = createActionButton("Export Ranking Report", UIIcon.Kind.CHART);
        postJobBtn.addActionListener(event -> navigate(SidebarPanel.NAV_JOBS));
        uploadResumeBtn.addActionListener(event -> navigate(SidebarPanel.NAV_CANDIDATES));
        runScreeningBtn.addActionListener(event -> navigate(SidebarPanel.NAV_SCREENING));
        exportReportBtn.addActionListener(event -> navigate(SidebarPanel.NAV_REPORTS));

        bar.add(postJobBtn);
        bar.add(uploadResumeBtn);
        bar.add(runScreeningBtn);
        bar.add(exportReportBtn);

        return bar;
    }

    private JPanel buildLowerGrid() {
        ResponsiveGridPanel grid = new ResponsiveGridPanel(2, 420, 20, 20);

        // Left Card: Active Job Openings Table
        ContentCard jobsCard = new ContentCard("Your Job Openings", "Actual jobs and stored screening counts");
        jobsCard.setBodyComponent(buildJobsTable());
        grid.add(jobsCard);

        // Right Card: Recent Candidate AI Rankings
        ContentCard rankingsCard = new ContentCard("Recent Screening Results", "Latest stored match scores; recruiter review required");
        rankingsCard.setBodyComponent(buildRecentActivityList());
        grid.add(rankingsCard);

        return grid;
    }

    private Component buildJobsTable() {
        JTable table = new JTable(jobsModel);
        for (String[] row : loadJobRows()) {
            jobsModel.addRow(row);
        }
        table.setFont(UITheme.FONT_BODY);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);

        // Centered alignment for count columns and status
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, r, c);
                l.setHorizontalAlignment(JLabel.CENTER);
                l.setFont(UITheme.FONT_CAPTION);
                String status = String.valueOf(val);
                if ("ACTIVE".equals(status)) {
                    l.setForeground(UITheme.SUCCESS);
                } else {
                    l.setForeground(UITheme.WARNING);
                }
                return l;
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));
        tableScroll.setPreferredSize(new Dimension(450, 220));
        return tableScroll;
    }

    private Component buildRecentActivityList() {
        recentActivityList = new JPanel();
        recentActivityList.setOpaque(false);
        recentActivityList.setLayout(new BoxLayout(recentActivityList, BoxLayout.Y_AXIS));

        for (ScreeningEntry entry : loadRecentScreenings()) {
            addCandidateRow(
                recentActivityList, entry.candidate().getFullName(), entry.resume().getFileName(),
                entry.result().getMatchScore() + "%", "Recruiter review", UITheme.PRIMARY
            );
        }
        if (recentActivityList.getComponentCount() == 0) {
            JLabel empty = new JLabel("No screening results have been saved yet.");
            empty.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
            empty.setForeground(UITheme.TEXT_SECONDARY);
            recentActivityList.add(empty);
        }

        JScrollPane scroll = new JScrollPane(recentActivityList);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));
        scroll.setPreferredSize(new Dimension(450, 220));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    public void setDashboardData(DashboardMetrics metrics, List<Job> jobs,
                                 Map<Integer, Integer> screenedCounts,
                                 List<ScreeningEntry> recentScreenings) {
        if (metrics == null || jobs == null || screenedCounts == null || recentScreenings == null) {
            throw new IllegalArgumentException("Complete dashboard data is required.");
        }
        for (int i = 0; i < metricCards.size(); i++) {
            metricCards.get(i).setValue(metric(metrics, i));
        }

        jobsModel.setRowCount(0);
        for (Job job : jobs) {
            jobsModel.addRow(new Object[]{
                job.getTitle(), job.getDepartment() == null ? "" : job.getDepartment(),
                screenedCounts.getOrDefault(job.getId(), 0), job.getStatus()
            });
        }

        recentActivityList.removeAll();
        List<String> candidateNames = new ArrayList<>();
        for (ScreeningEntry entry : recentScreenings) {
            candidateNames.add(entry.candidate().getFullName());
            addCandidateRow(
                recentActivityList, entry.candidate().getFullName(), entry.resume().getFileName(),
                entry.result().getMatchScore() + "%", "Recruiter review", UITheme.PRIMARY
            );
        }
        recentCandidateNames = List.copyOf(candidateNames);
        if (recentActivityList.getComponentCount() == 0) {
            JLabel empty = new JLabel("No screening results have been saved yet.");
            empty.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
            empty.setForeground(UITheme.TEXT_SECONDARY);
            recentActivityList.add(empty);
        }
        recentActivityList.revalidate();
        recentActivityList.repaint();
    }

    public int getJobRowCount() {
        return jobsModel.getRowCount();
    }

    public String getJobTitleAt(int row) {
        return String.valueOf(jobsModel.getValueAt(row, 0));
    }

    public String getMetricValueAt(int index) {
        return metricCards.get(index).getValue();
    }

    public List<String> getRecentCandidateNames() {
        return recentCandidateNames;
    }

    private void addCandidateRow(JPanel container, String name, String role, String score, String recommendation, Color scoreColor) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.CARD_BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // Left info
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UITheme.FONT_BODY_BOLD);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel roleLabel = new JLabel(role);
        roleLabel.setFont(UITheme.FONT_CAPTION);
        roleLabel.setForeground(UITheme.TEXT_SECONDARY);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(roleLabel);

        row.add(info, BorderLayout.WEST);

        // Right score badge
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgePanel.setOpaque(false);

        JLabel scoreLabel = new JLabel(score);
        scoreLabel.setFont(UITheme.FONT_BODY_BOLD);
        scoreLabel.setForeground(scoreColor);

        JLabel recLabel = new JLabel(recommendation);
        recLabel.setFont(UITheme.FONT_CAPTION);
        recLabel.setForeground(UITheme.TEXT_MUTED);
        badgePanel.add(scoreLabel);
        badgePanel.add(recLabel);

        row.add(badgePanel, BorderLayout.EAST);

        container.add(row);
    }

    private SecondaryButton createActionButton(String text, UIIcon.Kind iconKind) {
        SecondaryButton button = new SecondaryButton(text);
        button.setIcon(new UIIcon(iconKind, 16, UITheme.PRIMARY));
        button.setIconTextGap(8);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        return button;
    }

    private DashboardMetrics loadMetrics() {
        if (databaseService == null) {
            return null;
        }
        try {
            return databaseService.getDashboardMetrics(recruiterId);
        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger(DashboardView.class)
                .error("Dashboard metrics could not be loaded from the database.", e);
            return null;
        }
    }

    private String[][] loadJobRows() {
        if (databaseService == null) {
            return new String[0][4];
        }
        try {
            List<Job> jobs = databaseService.getJobsForRecruiter(recruiterId);
            String[][] rows = new String[jobs.size()][4];
            for (int i = 0; i < jobs.size(); i++) {
                Job job = jobs.get(i);
                rows[i] = new String[]{
                    job.getTitle(), job.getDepartment() == null ? "" : job.getDepartment(),
                    Integer.toString(databaseService.getScreenedResumeCountForJob(job.getId())),
                    job.getStatus()
                };
            }
            return rows;
        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger(DashboardView.class)
                .error("Dashboard jobs could not be loaded from the database.", e);
            return new String[][]{{"Unable to load jobs", "", "", ""}};
        }
    }

    private List<ScreeningEntry> loadRecentScreenings() {
        if (databaseService == null) {
            return List.of();
        }
        try {
            return databaseService.getRecentScreeningsForRecruiter(recruiterId, 5);
        } catch (SQLException e) {
            org.slf4j.LoggerFactory.getLogger(DashboardView.class)
                .error("Recent screening results could not be loaded from the database.", e);
            return List.of();
        }
    }

    private String metric(DashboardMetrics metrics, int index) {
        if (metrics == null) {
            return databaseService == null ? "0" : "N/A";
        }
        return Integer.toString(switch (index) {
            case 0 -> metrics.jobs();
            case 1 -> metrics.candidates();
            case 2 -> metrics.resumes();
            default -> metrics.screenings();
        });
    }

    private void navigate(String navId) {
        if (navigationHandler != null) {
            navigationHandler.accept(navId);
        }
    }

    private static class DashboardContentPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class ResponsiveGridPanel extends JPanel {

        private final int maximumColumns;
        private final int minimumCellWidth;
        private final int horizontalGap;

        ResponsiveGridPanel(int maximumColumns, int minimumCellWidth, int horizontalGap, int verticalGap) {
            super(new GridLayout(0, maximumColumns, horizontalGap, verticalGap));
            this.maximumColumns = maximumColumns;
            this.minimumCellWidth = minimumCellWidth;
            this.horizontalGap = horizontalGap;
            setOpaque(false);
            setMinimumSize(new Dimension(0, 0));
        }

        @Override
        public void doLayout() {
            int columns = Math.max(1, Math.min(
                maximumColumns,
                (getWidth() + horizontalGap) / (minimumCellWidth + horizontalGap)
            ));
            GridLayout layout = (GridLayout) getLayout();
            if (layout.getColumns() != columns) {
                layout.setColumns(columns);
                revalidate();
            }
            super.doLayout();
        }
    }
}
