package com.resumescreening.ui.views;

import com.resumescreening.models.JobAnalytics;
import com.resumescreening.models.ReportsSnapshot;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.services.ReportExportService;
import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.components.SecondaryButton;
import com.resumescreening.ui.components.StatCard;
import com.resumescreening.ui.components.UIIcon;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Recruiter-scoped reporting and analytics, separate from candidate management and screening. */
public class ReportsAnalyticsView extends JPanel {

    private final JLabel status = new JLabel(" ");
    private final JButton refreshButton = new SecondaryButton("Refresh");
    private final JButton csvButton = new SecondaryButton("Export CSV");
    private final JButton pdfButton = new PrimaryButton("Export PDF");
    private final JButton loadDemoButton = new SecondaryButton("Load Demo Data");
    private final JButton removeDemoButton = new SecondaryButton("Remove Demo Data");
    private final List<StatCard> metricCards = new ArrayList<>();
    private final DefaultTableModel jobsModel = new DefaultTableModel(
        new Object[]{"Job", "Department", "Status", "Applicants", "Average score"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel screeningsModel = new DefaultTableModel(
        new Object[]{"Job", "Rank", "Candidate", "Score", "Required /50", "Preferred /20",
            "Experience /15", "Text /15", "Screened"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable jobsTable = new JTable(jobsModel);
    private final JTable screeningsTable = new JTable(screeningsModel);
    private final DistributionChart applicantsChart = new DistributionChart("Applicants by job");
    private final DistributionChart scoresChart = new DistributionChart("Screening score distribution");
    private ReportsSnapshot snapshot;
    private Runnable refreshHandler;
    private Runnable loadDemoHandler;
    private Runnable removeDemoHandler;
    private Consumer<Path> csvExportHandler;
    private Consumer<Path> pdfExportHandler;

    public ReportsAnalyticsView() {
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG_CANVAS);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        JLabel intro = new JLabel("Recruiter-scoped hiring and screening insights");
        intro.setFont(UITheme.FONT_BODY);
        intro.setForeground(UITheme.TEXT_SECONDARY);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(csvButton);
        actions.add(pdfButton);
        toolbar.add(intro, BorderLayout.CENTER);
        toolbar.add(actions, BorderLayout.EAST);
        addSection(content, toolbar);

        JPanel demoActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        demoActions.setOpaque(false);
        JLabel demoLabel = new JLabel("Fictional demo dataset (tagged and removable):");
        demoLabel.setForeground(UITheme.TEXT_SECONDARY);
        demoActions.add(demoLabel);
        demoActions.add(loadDemoButton);
        demoActions.add(removeDemoButton);
        addSection(content, demoActions);

        JPanel metrics = new JPanel(new GridLayout(2, 2, 12, 12));
        metrics.setOpaque(false);
        metricCards.add(new StatCard("Total Jobs", "—", "Recruiter-owned jobs",
            UIIcon.Kind.BRIEFCASE, UITheme.PRIMARY, UITheme.PRIMARY_LIGHT));
        metricCards.add(new StatCard("Total Candidates", "—", "Candidate profiles with resumes",
            UIIcon.Kind.PEOPLE, UITheme.INFO, UITheme.INFO_LIGHT));
        metricCards.add(new StatCard("Uploaded Resumes", "—", "Validated resume files",
            UIIcon.Kind.DOCUMENT, UITheme.PURPLE, UITheme.PURPLE_LIGHT));
        metricCards.add(new StatCard("Screened Candidates", "—", "Unique candidates with results",
            UIIcon.Kind.CHART, UITheme.SUCCESS, UITheme.SUCCESS_LIGHT));
        metricCards.forEach(metrics::add);
        addSection(content, metrics);

        JPanel charts = new JPanel(new GridLayout(0, 1, 0, 16));
        charts.setOpaque(false);
        ContentCard applicantsCard = new ContentCard(
            "Candidate distribution across jobs", "Distinct applicants with a stored screening result"
        );
        applicantsCard.setBodyComponent(applicantsChart);
        ContentCard scoreCard = new ContentCard(
            "Screening score distribution", "Latest stored score per resume and job"
        );
        scoreCard.setBodyComponent(scoresChart);
        charts.add(applicantsCard);
        charts.add(scoreCard);
        addSection(content, charts);

        jobsTable.setFillsViewportHeight(true);
        jobsTable.setAutoCreateRowSorter(true);
        jobsTable.setRowHeight(26);
        ContentCard jobCard = new ContentCard(
            "Job-wise applicant report", "Applicant totals count distinct candidates; average uses latest resume scores"
        );
        jobCard.setBodyComponent(new JScrollPane(jobsTable));
        jobCard.setPreferredSize(new Dimension(0, 280));
        addSection(content, jobCard);

        screeningsTable.setFillsViewportHeight(true);
        screeningsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        screeningsTable.setRowHeight(26);
        int[] widths = {190, 60, 170, 80, 95, 100, 105, 75, 150};
        for (int i = 0; i < widths.length; i++) {
            screeningsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        screeningsTable.setRowSorter(new TableRowSorter<>(screeningsModel));
        JScrollPane screeningScroll = new JScrollPane(
            screeningsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        ContentCard screeningCard = new ContentCard(
            "Screening results and ranking summary", "Latest screening per resume, with component score breakdown"
        );
        screeningCard.setBodyComponent(screeningScroll);
        screeningCard.setPreferredSize(new Dimension(0, 330));
        addSection(content, screeningCard);

        JScrollPane pageScroll = new JScrollPane(content);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(pageScroll, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4, 24, 8, 24));
        status.setForeground(UITheme.TEXT_SECONDARY);
        add(status, BorderLayout.SOUTH);

        refreshButton.addActionListener(event -> refresh());
        csvButton.addActionListener(event -> chooseExport(".csv"));
        pdfButton.addActionListener(event -> chooseExport(".pdf"));
        loadDemoButton.addActionListener(event -> confirmDemoLoad());
        removeDemoButton.addActionListener(event -> confirmDemoRemoval());
    }

    public void setRefreshHandler(Runnable handler) {
        refreshHandler = handler;
    }

    public void setDemoDataHandlers(Runnable loadHandler, Runnable removeHandler) {
        loadDemoHandler = loadHandler;
        removeDemoHandler = removeHandler;
    }

    public void setExportHandlers(Consumer<Path> csvHandler, Consumer<Path> pdfHandler) {
        csvExportHandler = csvHandler;
        pdfExportHandler = pdfHandler;
    }

    public void setLoading(boolean loading) {
        if (loading) {
            status.setForeground(UITheme.TEXT_SECONDARY);
            status.setText("Loading reports from the database...");
        }
        refreshButton.setEnabled(!loading);
        csvButton.setEnabled(!loading);
        pdfButton.setEnabled(!loading);
        loadDemoButton.setEnabled(!loading);
        removeDemoButton.setEnabled(!loading);
    }

    public void setReports(ReportsSnapshot value) {
        snapshot = value;
        jobsModel.setRowCount(0);
        screeningsModel.setRowCount(0);
        if (value == null) {
            clearCharts();
            status.setForeground(UITheme.TEXT_SECONDARY);
            status.setText("No report data is available.");
            return;
        }

        for (int i = 0; i < metricCards.size(); i++) {
            metricCards.get(i).setValue(metricValue(value, i));
        }
        Map<String, Integer> applicantsByJob = new LinkedHashMap<>();
        Map<String, Integer> scoreBuckets = new LinkedHashMap<>();
        scoreBuckets.put("0-19", 0);
        scoreBuckets.put("20-39", 0);
        scoreBuckets.put("40-59", 0);
        scoreBuckets.put("60-79", 0);
        scoreBuckets.put("80-100", 0);

        for (JobAnalytics job : value.jobs()) {
            String title = safe(job.job().getTitle());
            applicantsByJob.put(title + " (#" + job.job().getId() + ")", job.applicantCount());
            jobsModel.addRow(new Object[]{
                title, safe(job.job().getDepartment()), safe(job.job().getStatus()),
                job.applicantCount(), job.averageScore().toPlainString()
            });
            for (ScreeningEntry entry : job.screenings()) {
                var result = entry.result();
                screeningsModel.addRow(new Object[]{
                    title, result.getRanking(), safe(entry.candidate().getFullName()),
                    score(result.getMatchScore()), score(result.getRequiredSkillsScore()),
                    score(result.getPreferredSkillsScore()), score(result.getExperienceScore()),
                    score(result.getTextSimilarityScore()), result.getScreenedAt()
                });
                if (result.getMatchScore() != null) {
                    int valueScore = result.getMatchScore().intValue();
                    String bucket = valueScore < 20 ? "0-19" : valueScore < 40 ? "20-39"
                        : valueScore < 60 ? "40-59" : valueScore < 80 ? "60-79" : "80-100";
                    scoreBuckets.computeIfPresent(bucket, (key, count) -> count + 1);
                }
            }
        }
        applicantsChart.setValues(applicantsByJob);
        scoresChart.setValues(scoreBuckets);
        status.setForeground(UITheme.TEXT_SECONDARY);
        status.setText(value.jobs().isEmpty()
            ? "No jobs are available for this recruiter yet."
            : value.screenings().isEmpty()
                ? "No screening results yet. Screen resumes against a job to populate analytics."
                : value.jobs().size() + " job(s), " + value.screenings().size()
                    + " latest resume/job screening result(s) loaded.");
    }

    public void showError(String message) {
        status.setForeground(UITheme.ERROR);
        status.setText(message);
    }

    public void showMessage(String message) {
        status.setForeground(UITheme.SUCCESS);
        status.setText(message);
    }

    public ReportsSnapshot getSnapshot() {
        return snapshot;
    }

    public JTable getJobsTable() {
        return jobsTable;
    }

    public JTable getScreeningsTable() {
        return screeningsTable;
    }

    public JButton getLoadDemoButton() {
        return loadDemoButton;
    }

    public JButton getRemoveDemoButton() {
        return removeDemoButton;
    }

    private void confirmDemoLoad() {
        int answer = JOptionPane.showConfirmDialog(
            this,
            "Create 8 clearly tagged demo jobs and 18 fictional candidate resumes? "
                + "Existing records will not be overwritten. Loading is safe to repeat.",
            "Load fictional demo data",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (answer == JOptionPane.YES_OPTION && loadDemoHandler != null) {
            loadDemoHandler.run();
        }
    }

    private void confirmDemoRemoval() {
        int answer = JOptionPane.showConfirmDialog(
            this,
            "Remove this recruiter's [DEMO] jobs and demo-only candidates/resumes? "
                + "Candidates still used by non-demo screenings will be retained.",
            "Remove demo data",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (answer == JOptionPane.YES_OPTION && removeDemoHandler != null) {
            removeDemoHandler.run();
        }
    }

    public String getStatusMessage() {
        return status.getText();
    }

    public List<String> getMetricValues() {
        return metricCards.stream().map(StatCard::getValue).toList();
    }

    public DistributionChart getApplicantsChart() {
        return applicantsChart;
    }

    public DistributionChart getScoresChart() {
        return scoresChart;
    }

    private void refresh() {
        if (refreshHandler == null) {
            showError("Report refresh is unavailable.");
            return;
        }
        refreshHandler.run();
    }

    private void chooseExport(String extension) {
        if (snapshot == null) {
            showError("Load report data before exporting.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export " + extension.substring(1).toUpperCase(java.util.Locale.ROOT) + " report");
        chooser.setFileFilter(new FileNameExtensionFilter(
            extension.substring(1).toUpperCase(java.util.Locale.ROOT) + " report (*" + extension + ")",
            extension.substring(1)
        ));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destination = chooser.getSelectedFile().toPath();
        if (!destination.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            destination = destination.resolveSibling(destination.getFileName() + extension);
        }
        Consumer<Path> handler = extension.equals(".csv") ? csvExportHandler : pdfExportHandler;
        if (handler == null) {
            showError("Report export is unavailable.");
            return;
        }
        handler.accept(destination);
    }

    private void clearCharts() {
        applicantsChart.setValues(Map.of());
        scoresChart.setValues(Map.of());
    }

    private void addSection(JPanel content, javax.swing.JComponent component) {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            Math.max(component.getPreferredSize().height, component.getMinimumSize().height)));
        component.setAlignmentX(LEFT_ALIGNMENT);
        content.add(component);
        content.add(javax.swing.Box.createVerticalStrut(16));
    }

    private String metricValue(ReportsSnapshot value, int index) {
        return switch (index) {
            case 0 -> Integer.toString(value.metrics().jobs());
            case 1 -> Integer.toString(value.metrics().candidates());
            case 2 -> Integer.toString(value.metrics().resumes());
            default -> Integer.toString(value.screenedCandidateCount());
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String score(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    /** Small accessible bar chart drawn with Java2D to avoid an additional chart dependency. */
    public static final class DistributionChart extends JPanel {
        private final String title;
        private Map<String, Integer> values = Map.of();

        private DistributionChart(String title) {
            this.title = title;
            setOpaque(false);
            setPreferredSize(new Dimension(420, 230));
            getAccessibleContext().setAccessibleName(title);
        }

        public void setValues(Map<String, Integer> newValues) {
            values = new LinkedHashMap<>(newValues);
            repaint();
        }

        public Map<String, Integer> getValues() {
            return Map.copyOf(values);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int left = 38;
                int right = Math.max(left + 1, getWidth() - 12);
                int top = 14;
                int baseline = getHeight() - 40;
                g.setColor(UITheme.CARD_BORDER);
                g.setStroke(new BasicStroke(1));
                g.drawLine(left, top, left, baseline);
                g.drawLine(left, baseline, right, baseline);
                if (values.isEmpty() || values.values().stream().mapToInt(Integer::intValue).sum() == 0) {
                    g.setFont(UITheme.FONT_BODY);
                    g.setColor(UITheme.TEXT_MUTED);
                    String empty = "No data available";
                    FontMetrics metrics = g.getFontMetrics();
                    g.drawString(empty, Math.max(left + 8, (getWidth() - metrics.stringWidth(empty)) / 2),
                        Math.max(top + 24, (top + baseline) / 2));
                    return;
                }
                int max = Math.max(1, values.values().stream().mapToInt(Integer::intValue).max().orElse(0));
                int count = values.size();
                int slot = Math.max(1, (right - left) / count);
                int barWidth = Math.max(12, Math.min(42, slot - 14));
                int availableHeight = Math.max(1, baseline - top - 24);
                int index = 0;
                for (Map.Entry<String, Integer> item : values.entrySet()) {
                    int barHeight = (int) Math.round((double) item.getValue() / max * availableHeight);
                    int x = left + index * slot + (slot - barWidth) / 2;
                    int y = baseline - barHeight;
                    g.setColor(UITheme.PRIMARY);
                    g.fillRoundRect(x, y, barWidth, Math.max(2, barHeight), 8, 8);
                    g.setColor(UITheme.TEXT_SECONDARY);
                    g.setFont(UITheme.FONT_CAPTION);
                    String countLabel = Integer.toString(item.getValue());
                    FontMetrics metrics = g.getFontMetrics();
                    g.drawString(countLabel, x + (barWidth - metrics.stringWidth(countLabel)) / 2,
                        Math.max(top + 10, y - 4));
                    String category = item.getKey();
                    if (metrics.stringWidth(category) > slot - 2 && category.length() > 5) {
                        category = category.substring(0, Math.min(5, category.length())) + "...";
                    }
                    g.drawString(category, x + (barWidth - metrics.stringWidth(category)) / 2,
                        baseline + 16);
                    index++;
                }
            } finally {
                g.dispose();
            }
        }
    }
}
