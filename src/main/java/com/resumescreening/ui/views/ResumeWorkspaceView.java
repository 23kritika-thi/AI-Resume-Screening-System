package com.resumescreening.ui.views;

import com.resumescreening.models.Candidate;
import com.resumescreening.models.Job;
import com.resumescreening.models.ResumeEntry;
import com.resumescreening.models.ResumeProfile;
import com.resumescreening.models.ScreeningEntry;
import com.resumescreening.services.ResumeProfileExtractor;
import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.components.SecondaryButton;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Resume upload, candidate information, and ranked job-screening results. */
public class ResumeWorkspaceView extends JPanel {

    private final JTextField candidateName = new JTextField(18);
    private final JTextField candidateEmail = new JTextField(18);
    private final JTextField candidatePhone = new JTextField(14);
    private final JComboBox<Job> jobSelector = new JComboBox<>();
    private final JLabel selectedFile = new JLabel("No file selected");
    private final JLabel status = new JLabel(" ");
    private final JTextArea details = new JTextArea();
    private final JScrollPane detailsScroll = new JScrollPane(details);
    private final DefaultTableModel resumeModel = new DefaultTableModel(
        new Object[]{"Candidate", "Email", "Phone", "Resume file", "Uploaded"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel screeningModel = new DefaultTableModel(
        new Object[]{"Rank", "Candidate", "Score", "Required /50", "Preferred /20", "Experience /15", "Text /15"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resumesTable = new JTable(resumeModel);
    private final JTable screeningTable = new JTable(screeningModel);
    private final ResumeProfileExtractor profileExtractor = new ResumeProfileExtractor();
    private Path chosenFile;
    private UploadHandler uploadHandler;
    private java.util.function.IntConsumer screenHandler;
    private java.util.function.IntConsumer jobSelectionHandler;
    private List<ResumeEntry> currentResumes = List.of();
    private List<ScreeningEntry> currentResults = List.of();
    private boolean replacingJobs;

    public ResumeWorkspaceView() {
        jobSelector.addActionListener(event -> {
            if (!replacingJobs && jobSelectionHandler != null && getSelectedJobId() != null) {
                jobSelectionHandler.accept(getSelectedJobId());
            }
        });
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JPanel uploadForm = new JPanel(new BorderLayout(10, 10));
        uploadForm.setOpaque(false);
        JPanel inputs = new JPanel(new GridLayout(0, 3, 10, 8));
        inputs.setOpaque(false);
        inputs.add(field("Candidate name", candidateName));
        inputs.add(field("Email (optional)", candidateEmail));
        inputs.add(field("Phone (optional)", candidatePhone));
        JPanel jobField = field("Associate with job (optional)", jobSelector);
        inputs.add(jobField);

        JLabel instruction = new JLabel("Upload your resume in PDF format only.");
        instruction.setFont(UITheme.FONT_BODY_BOLD);
        instruction.setForeground(UITheme.TEXT_PRIMARY);
        SecondaryButton choose = new SecondaryButton("Choose resume");
        choose.addActionListener(event -> chooseResume());
        PrimaryButton upload = new PrimaryButton("Upload resume");
        upload.addActionListener(event -> submitUpload());
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fileRow.setOpaque(false);
        fileRow.add(instruction);
        fileRow.add(choose);
        fileRow.add(selectedFile);
        fileRow.add(upload);
        status.setForeground(UITheme.TEXT_SECONDARY);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(inputs, BorderLayout.NORTH);
        JPanel lower = new JPanel(new BorderLayout(0, 5));
        lower.setOpaque(false);
        lower.add(fileRow, BorderLayout.NORTH);
        lower.add(status, BorderLayout.SOUTH);
        header.add(lower, BorderLayout.SOUTH);
        uploadForm.add(header, BorderLayout.CENTER);

        resumesTable.setFillsViewportHeight(true);
        resumesTable.setAutoCreateRowSorter(true);
        resumesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resumesTable.setRowHeight(26);
        int[] resumeWidths = {170, 230, 150, 260, 170};
        for (int i = 0; i < resumeWidths.length; i++) {
            resumesTable.getColumnModel().getColumn(i).setPreferredWidth(resumeWidths[i]);
        }
        resumesTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedResume();
            }
        });
        screeningTable.setFillsViewportHeight(true);
        TableRowSorter<DefaultTableModel> screeningSorter = new TableRowSorter<>(screeningModel);
        screeningTable.setRowSorter(screeningSorter);
        screeningTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedScreening();
            }
        });
        JScrollPane resumesScroll = new JScrollPane(
            resumesTable,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        resumesScroll.setMinimumSize(new java.awt.Dimension(0, 100));
        ContentCard resumesCard = new ContentCard("Uploaded resumes", "Validated text-based PDF resumes and extracted candidate details");
        resumesCard.setBodyComponent(resumesScroll);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setRows(6);
        details.setColumns(35);
        details.setFont(UITheme.FONT_BODY);
        detailsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        detailsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailsScroll.getVerticalScrollBar().setUnitIncrement(16);
        detailsScroll.setMinimumSize(new java.awt.Dimension(0, 100));
        ContentCard detailCard = new ContentCard("Resume details", "Evidence extracted from selectable PDF text");
        detailCard.setBodyComponent(detailsScroll);

        PrimaryButton screen = new PrimaryButton("Screen resumes for selected job");
        screen.addActionListener(event -> screenSelectedJob());
        JTextField resultsFilter = new JTextField(14);
        resultsFilter.setToolTipText("Filter ranked candidates by name, rank, or score");
        resultsFilter.getDocument().addDocumentListener(new DocumentListener() {
            private void updateFilter() {
                String text = resultsFilter.getText().strip();
                screeningSorter.setRowFilter(text.isEmpty() ? null
                    : javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateFilter();
            }
        });
        JPanel resultsTop = new JPanel(new BorderLayout());
        resultsTop.setOpaque(false);
        JPanel resultsControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        resultsControls.setOpaque(false);
        resultsControls.add(new JLabel("Filter"));
        resultsControls.add(resultsFilter);
        resultsTop.add(resultsControls, BorderLayout.WEST);
        JLabel decisionSupport = new JLabel("Decision support only; recruiters decide.");
        decisionSupport.setForeground(UITheme.TEXT_SECONDARY);
        resultsTop.add(decisionSupport, BorderLayout.CENTER);
        resultsTop.add(screen, BorderLayout.EAST);
        JScrollPane screeningScroll = new JScrollPane(screeningTable);
        JPanel resultsPanel = new JPanel(new BorderLayout(0, 8));
        resultsPanel.setOpaque(false);
        resultsPanel.add(resultsTop, BorderLayout.NORTH);
        resultsPanel.add(screeningScroll, BorderLayout.CENTER);
        ContentCard resultsCard = new ContentCard("Candidate ranking", "Transparent weighted scores; sortable by any score column");
        resultsCard.setBodyComponent(resultsPanel);

        JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resumesCard, detailCard);
        lowerSplit.setResizeWeight(0.6);
        lowerSplit.setMinimumSize(new java.awt.Dimension(0, 180));
        lowerSplit.setBorder(null);
        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lowerSplit, resultsCard);
        workspace.setResizeWeight(0.48);
        workspace.setBorder(null);
        add(uploadForm, BorderLayout.NORTH);
        add(workspace, BorderLayout.CENTER);
    }

    public void setUploadHandler(UploadHandler handler) {
        uploadHandler = handler;
    }

    public void setScreenHandler(java.util.function.IntConsumer handler) {
        screenHandler = handler;
    }

    public void setJobSelectionHandler(java.util.function.IntConsumer handler) {
        jobSelectionHandler = handler;
    }

    public void setJobs(List<Job> jobs) {
        setJobs(jobs, null);
    }

    public void setJobs(List<Job> jobs, Integer preferredJobId) {
        Integer previousSelection = getSelectedJobId();
        DefaultComboBoxModel<Job> model = new DefaultComboBoxModel<>();
        for (Job job : jobs) {
            model.addElement(job);
        }
        replacingJobs = true;
        try {
            jobSelector.setModel(model);
            Integer targetJobId = preferredJobId == null ? previousSelection : preferredJobId;
            if (targetJobId != null) {
                for (int i = 0; i < model.getSize(); i++) {
                    if (model.getElementAt(i).getId() == targetJobId) {
                        jobSelector.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            replacingJobs = false;
        }
        jobSelector.setRenderer((list, job, index, selected, focused) -> {
            JLabel label = new JLabel(job == null ? "Choose a job" : job.getTitle());
            label.setOpaque(selected);
            if (selected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });
    }

    public void selectJob(int jobId) {
        for (int i = 0; i < jobSelector.getItemCount(); i++) {
            Job job = jobSelector.getItemAt(i);
            if (job.getId() == jobId) {
                jobSelector.setSelectedIndex(i);
                return;
            }
        }
    }

    public void setResumes(List<ResumeEntry> entries) {
        currentResumes = List.copyOf(entries);
        resumesTable.clearSelection();
        details.setText("");
        resumeModel.setRowCount(0);
        for (ResumeEntry entry : currentResumes) {
            resumeModel.addRow(new Object[]{
                entry.candidate().getFullName(), entry.candidate().getEmail(), entry.candidate().getPhone(),
                entry.resume().getFileName(), entry.resume().getUploadedAt()
            });
        }
    }

    public void setScreeningResults(List<ScreeningEntry> entries) {
        currentResults = List.copyOf(entries);
        screeningTable.clearSelection();
        details.setText("");
        screeningModel.setRowCount(0);
        for (ScreeningEntry entry : currentResults) {
            var result = entry.result();
            screeningModel.addRow(new Object[]{
                result.getRanking(), entry.candidate().getFullName(), result.getMatchScore(),
                result.getRequiredSkillsScore(), result.getPreferredSkillsScore(),
                result.getExperienceScore(), result.getTextSimilarityScore()
            });
        }
    }

    public void showStatus(String message, boolean error) {
        status.setText(message);
        status.setForeground(error ? UITheme.ERROR : UITheme.SUCCESS);
    }

    public JTable getResumesTable() {
        return resumesTable;
    }

    public JTable getScreeningTable() {
        return screeningTable;
    }

    public JScrollPane getResumeDetailsScrollPane() {
        return detailsScroll;
    }

    public JTextArea getResumeDetailsTextArea() {
        return details;
    }

    public Integer getSelectedJobId() {
        Job selected = (Job) jobSelector.getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    private void chooseResume() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a PDF resume");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF resumes (*.pdf)", "pdf"));
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            chosenFile = chooser.getSelectedFile().toPath();
            selectedFile.setText(chosenFile.getFileName().toString());
            showStatus("Selected. The file's content will be validated before storage.", false);
        }
    }

    private void submitUpload() {
        if (chosenFile == null) {
            showStatus("Choose a resume file first.", true);
            return;
        }
        if (uploadHandler == null) {
            showStatus("Resume upload is unavailable.", true);
            return;
        }
        Candidate candidate = new Candidate(
            candidateName.getText().strip(), blankToNull(candidateEmail.getText()),
            blankToNull(candidatePhone.getText()), null
        );
        Job selectedJob = (Job) jobSelector.getSelectedItem();
        uploadHandler.upload(new UploadRequest(chosenFile, candidate, selectedJob == null ? null : selectedJob.getId()));
    }

    private void screenSelectedJob() {
        Job selectedJob = (Job) jobSelector.getSelectedItem();
        if (selectedJob == null) {
            showStatus("Create or select a job before screening resumes.", true);
        } else if (screenHandler == null) {
            showStatus("Resume screening is unavailable.", true);
        } else {
            screenHandler.accept(selectedJob.getId());
        }
    }

    private void showSelectedResume() {
        int viewRow = resumesTable.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int row = resumesTable.convertRowIndexToModel(viewRow);
        ResumeEntry entry = currentResumes.get(row);
        ResumeProfile profile = profileExtractor.extract(entry.resume().getExtractedText());
        details.setText(formatProfile(profile, entry.resume().getExtractedText()));
        details.setCaretPosition(0);
    }

    private void showSelectedScreening() {
        int viewRow = screeningTable.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        ScreeningEntry entry = currentResults.get(screeningTable.convertRowIndexToModel(viewRow));
        ResumeProfile profile = profileExtractor.extract(entry.resume().getExtractedText());
        String explanation = "Score: " + entry.result().getMatchScore() + "/100\n"
            + "Required skills: " + entry.result().getRequiredSkillsScore() + "/50 points\n"
            + "Preferred skills: " + entry.result().getPreferredSkillsScore() + "/20 points\n"
            + "Relevant experience: " + entry.result().getExperienceScore() + "/15 points "
            + "(based on explicit years-of-experience text)\n"
            + "Job-description text similarity: " + entry.result().getTextSimilarityScore() + "/15 points\n"
            + "Matched skills: " + emptyLabel(entry.result().getMatchedSkills()) + "\n"
            + "Missing skills: " + emptyLabel(entry.result().getMissingSkills()) + "\n\n"
            + formatProfile(profile, entry.resume().getExtractedText());
        details.setText(explanation);
        details.setCaretPosition(0);
    }

    private String formatProfile(ResumeProfile profile, String sourceText) {
        return "Candidate: " + emptyLabel(profile.candidateName()) + "\n"
            + "Email: " + emptyLabel(profile.email()) + "\n"
            + "Phone: " + emptyLabel(profile.phone()) + "\n"
            + "Estimated experience mentioned: " + profile.experienceYears() + " year(s)\n"
            + "Programming languages: " + emptyLabel(String.join(", ", profile.categorizedSkills().getOrDefault("Programming Languages", List.of()))) + "\n"
            + "Frameworks & technologies: " + emptyLabel(String.join(", ", profile.categorizedSkills().getOrDefault("Frameworks & Technologies", List.of()))) + "\n\n"
            + "Education\n" + section(profile.education()) + "\n"
            + "Work experience\n" + section(profile.workExperience()) + "\n"
            + "Internships\n" + section(profile.internships()) + "\n"
            + "Projects\n" + section(profile.projects()) + "\n"
            + "Certifications\n" + section(profile.certifications()) + "\n"
            + "Extracted text:\n" + sourceText;
    }

    private JPanel field(String label, java.awt.Component input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setFont(UITheme.FONT_CAPTION);
        caption.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private String section(List<String> lines) {
        return lines.isEmpty() ? "Not found in extracted text\n" : String.join("\n", lines) + "\n";
    }

    private String emptyLabel(String value) {
        return value == null || value.isBlank() ? "Not found in extracted text" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public record UploadRequest(Path file, Candidate candidate, Integer jobId) {
    }

    @FunctionalInterface
    public interface UploadHandler {
        void upload(UploadRequest request);
    }
}
