package com.resumescreening.ui.views;

import com.resumescreening.models.Candidate;
import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.SecondaryButton;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Displays saved candidate records independently of the resume-screening workflow. */
public class CandidateDirectoryView extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel(
        new Object[]{"Candidate", "Email", "Phone", "LinkedIn", "Created"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextArea details = new JTextArea();
    private final JLabel status = new JLabel(" ");
    private final JButton deleteButton = new SecondaryButton("Delete selected");
    private List<Candidate> currentCandidates = List.of();
    private IntConsumer deleteHandler;

    public CandidateDirectoryView() {
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(26);
        int[] widths = {190, 220, 140, 230, 160};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedCandidate();
                deleteButton.setEnabled(table.getSelectedRow() >= 0);
            }
        });
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField filter = new JTextField(22);
        filter.setToolTipText("Filter candidates by name, email, phone, or LinkedIn");
        filter.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String query = filter.getText().strip();
                sorter.setRowFilter(query.isEmpty() ? null
                    : javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                update();
            }
        });
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Filter candidates"), BorderLayout.WEST);
        toolbar.add(filter, BorderLayout.CENTER);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(event -> deleteSelectedCandidate());
        toolbar.add(deleteButton, BorderLayout.EAST);

        JScrollPane tableScroll = new JScrollPane(
            table,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        tableScroll.setPreferredSize(new Dimension(760, 300));
        ContentCard candidatesCard = new ContentCard(
            "Stored candidates", "Candidate details saved with validated resume uploads"
        );
        candidatesCard.setBodyComponent(tableScroll);

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(UITheme.FONT_BODY);
        JScrollPane detailsScroll = new JScrollPane(details);
        detailsScroll.setPreferredSize(new Dimension(760, 110));
        ContentCard detailsCard = new ContentCard("Candidate details", "Select a row to inspect the available saved contact details");
        detailsCard.setBodyComponent(detailsScroll);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(candidatesCard, BorderLayout.CENTER);
        center.add(detailsCard, BorderLayout.SOUTH);
        add(toolbar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        status.setForeground(UITheme.TEXT_SECONDARY);
        add(status, BorderLayout.SOUTH);
    }

    public void setCandidates(List<Candidate> candidates) {
        currentCandidates = List.copyOf(candidates);
        model.setRowCount(0);
        for (Candidate candidate : currentCandidates) {
            model.addRow(new Object[]{
                candidate.getFullName(), candidate.getEmail(), candidate.getPhone(),
                candidate.getLinkedinUrl(), candidate.getCreatedAt()
            });
        }
        table.clearSelection();
        details.setText("");
        deleteButton.setEnabled(false);
    }

    public JTable getCandidatesTable() {
        return table;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public void setDeleteHandler(IntConsumer handler) {
        deleteHandler = handler;
    }

    public void setDeleteInProgress(boolean inProgress) {
        deleteButton.setEnabled(!inProgress && table.getSelectedRow() >= 0);
    }

    public void showMessage(String message, boolean error) {
        status.setText(message);
        status.setForeground(error ? UITheme.ERROR : UITheme.SUCCESS);
    }

    public void deleteSelectedCandidate() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            showMessage("Select a candidate before deleting.", true);
            return;
        }
        Candidate candidate = currentCandidates.get(table.convertRowIndexToModel(selected));
        if (!confirmDeletion(candidate)) {
            showMessage("Deletion cancelled. The candidate was not changed.", false);
            return;
        }
        if (deleteHandler == null) {
            showMessage("Candidate deletion is unavailable.", true);
            return;
        }
        deleteButton.setEnabled(false);
        showMessage("Deleting candidate and associated records...", false);
        deleteHandler.accept(candidate.getId());
    }

    protected boolean confirmDeletion(Candidate candidate) {
        return JOptionPane.showConfirmDialog(
            this,
            "Delete " + candidate.getFullName() + " and their associated resumes and screening results?",
            "Confirm candidate deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        ) == JOptionPane.YES_OPTION;
    }

    private void showSelectedCandidate() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            details.setText("");
            return;
        }
        int modelRow = table.convertRowIndexToModel(selected);
        StringBuilder text = new StringBuilder();
        for (int column = 0; column < model.getColumnCount(); column++) {
            text.append(model.getColumnName(column)).append(": ")
                .append(model.getValueAt(modelRow, column) == null ? "Not available" : model.getValueAt(modelRow, column))
                .append('\n');
        }
        details.setText(text.toString());
    }
}
