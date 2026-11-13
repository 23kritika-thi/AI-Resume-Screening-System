package com.resumescreening.ui.views;

import com.resumescreening.models.Job;
import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Recruiter-owned job descriptions and skill criteria. */
public class JobManagementView extends JPanel {

    private final JTextField title = new JTextField();
    private final JTextField department = new JTextField();
    private final JTextField requiredSkills = new JTextField();
    private final JTextField preferredSkills = new JTextField();
    private final JSpinner experience = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
    private final JTextArea description = new JTextArea(5, 40);
    private final JLabel message = new JLabel(" ");
    private final DefaultTableModel jobsModel = new DefaultTableModel(
        new Object[]{"Job", "Department", "Required skills", "Preferred skills", "Min. experience", "Description"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable jobsTable = new JTable(jobsModel);
    private Consumer<Job> onCreate;

    public JobManagementView() {
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JPanel form = new JPanel(new BorderLayout(12, 12));
        form.setOpaque(false);
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, "Job title", title, 0, 0);
        addField(fields, "Department", department, 1, 0);
        addField(fields, "Required skills (comma separated)", requiredSkills, 0, 1);
        addField(fields, "Preferred skills (comma separated)", preferredSkills, 1, 1);
        addField(fields, "Minimum relevant experience (years)", experience, 0, 2);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setPreferredSize(new Dimension(300, 64));
        addField(fields, "Job description", descriptionScroll, 1, 2);
        form.add(fields, BorderLayout.CENTER);

        JPanel formFooter = new JPanel(new BorderLayout());
        formFooter.setOpaque(false);
        message.setForeground(UITheme.TEXT_SECONDARY);
        PrimaryButton save = new PrimaryButton("Save job");
        save.addActionListener(event -> createJob());
        formFooter.add(message, BorderLayout.CENTER);
        formFooter.add(save, BorderLayout.EAST);
        form.add(formFooter, BorderLayout.SOUTH);

        jobsTable.setFillsViewportHeight(true);
        jobsTable.setAutoCreateRowSorter(true);
        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jobsTable.setRowHeight(26);
        jobsTable.getTableHeader().setPreferredSize(new Dimension(0, 30));
        int[] widths = {180, 120, 210, 190, 110, 260};
        for (int i = 0; i < widths.length; i++) {
            jobsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        jobsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column
                );
                label.setToolTipText(value == null ? null : value.toString());
                return label;
            }
        });
        JScrollPane tableScroll = new JScrollPane(
            jobsTable,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setMinimumSize(new Dimension(0, 140));
        ContentCard jobsCard = new ContentCard("Your job descriptions", "Saved requirements used for transparent resume matching");
        jobsCard.setBodyComponent(tableScroll);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, form, jobsCard);
        split.setResizeWeight(0.38);
        split.setDividerLocation(285);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    public void setCreateHandler(Consumer<Job> handler) {
        onCreate = handler;
    }

    public void setJobs(List<Job> jobs) {
        jobsModel.setRowCount(0);
        for (Job job : jobs) {
            jobsModel.addRow(new Object[]{
                job.getTitle(), job.getDepartment(), job.getRequiredSkills(),
                job.getPreferredSkills(), job.getMinExperienceYears(), job.getDescription()
            });
        }
    }

    public JTable getJobsTable() {
        return jobsTable;
    }

    public JScrollPane getJobsScrollPane() {
        return (JScrollPane) jobsTable.getParent().getParent();
    }

    public void showMessage(String text, boolean error) {
        message.setText(text);
        message.setForeground(error ? UITheme.ERROR : UITheme.SUCCESS);
    }

    private void createJob() {
        String cleanTitle = title.getText().strip();
        String cleanDescription = description.getText().strip();
        if (cleanTitle.isEmpty() || cleanDescription.isEmpty()) {
            showMessage("Enter a job title and description.", true);
            return;
        }
        Job job = new Job(
            0, cleanTitle, blankToNull(department.getText()), cleanDescription,
            blankToNull(requiredSkills.getText()), (Integer) experience.getValue()
        );
        job.setPreferredSkills(blankToNull(preferredSkills.getText()));
        if (onCreate == null) {
            showMessage("Job saving is unavailable.", true);
            return;
        }
        onCreate.accept(job);
    }

    private void addField(JPanel fields, String label, java.awt.Component input, int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(3, 5, 5, 8);
        fields.add(field(label, input), constraints);
    }

    private JPanel field(String label, java.awt.Component input) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setFont(UITheme.FONT_CAPTION);
        caption.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
