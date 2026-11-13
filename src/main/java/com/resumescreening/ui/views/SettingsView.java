package com.resumescreening.ui.views;

import com.resumescreening.database.DatabaseConfig;
import com.resumescreening.models.Recruiter;
import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.components.SecondaryButton;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Persistent recruiter profile settings and a live database connectivity check. */
public class SettingsView extends JPanel {

    private final JTextField fullNameField = new JTextField(28);
    private final JLabel emailValue = new JLabel(" ");
    private final JLabel databaseTarget = new JLabel(" ");
    private final JLabel databaseStatus = new JLabel("Connection not checked.");
    private final JLabel status = new JLabel(" ");
    private final JButton saveButton = new PrimaryButton("Save profile");
    private final JButton resetButton = new SecondaryButton("Reset");
    private final JButton testConnectionButton = new SecondaryButton("Test database connection");
    private String savedName = "";
    private Consumer<String> profileSaveHandler;
    private Runnable databaseTestHandler;

    public SettingsView(Recruiter recruiter, DatabaseConfig databaseConfig) {
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        JPanel content = new JPanel(new GridLayout(0, 1, 0, 16));
        content.setOpaque(false);
        JLabel introduction = new JLabel("Manage your recruiter profile and verify application connectivity.");
        introduction.setFont(UITheme.FONT_BODY);
        introduction.setForeground(UITheme.TEXT_SECONDARY);
        content.add(introduction);

        JPanel profileForm = new JPanel(new GridLayout(0, 2, 14, 14));
        profileForm.setOpaque(false);
        profileForm.add(label("Full name"));
        profileForm.add(fullNameField);
        profileForm.add(label("Account email"));
        emailValue.setFont(UITheme.FONT_BODY);
        emailValue.setForeground(UITheme.TEXT_PRIMARY);
        emailValue.setName("accountEmail");
        profileForm.add(emailValue);

        JPanel profileActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        profileActions.setOpaque(false);
        profileActions.add(resetButton);
        profileActions.add(saveButton);
        ContentCard profileCard = new ContentCard(
            "Recruiter profile", "Your display name is saved to your recruiter account."
        );
        JPanel profileBody = new JPanel(new BorderLayout(0, 16));
        profileBody.setOpaque(false);
        profileBody.add(profileForm, BorderLayout.CENTER);
        profileBody.add(profileActions, BorderLayout.SOUTH);
        profileCard.setBodyComponent(profileBody);
        content.add(profileCard);

        JPanel databaseBody = new JPanel(new BorderLayout(12, 12));
        databaseBody.setOpaque(false);
        JPanel databaseDetails = new JPanel();
        databaseDetails.setOpaque(false);
        databaseDetails.setLayout(new javax.swing.BoxLayout(databaseDetails, javax.swing.BoxLayout.Y_AXIS));
        databaseTarget.setAlignmentX(LEFT_ALIGNMENT);
        databaseStatus.setAlignmentX(LEFT_ALIGNMENT);
        databaseStatus.setFont(UITheme.FONT_BODY_BOLD);
        databaseStatus.setForeground(UITheme.TEXT_SECONDARY);
        databaseDetails.add(databaseTarget);
        databaseDetails.add(javax.swing.Box.createVerticalStrut(8));
        databaseDetails.add(databaseStatus);
        databaseBody.add(databaseDetails, BorderLayout.CENTER);
        databaseBody.add(testConnectionButton, BorderLayout.EAST);
        ContentCard databaseCard = new ContentCard(
            "Database connection", "Connection settings are read-only here; credentials stay in environment or local configuration."
        );
        databaseCard.setBodyComponent(databaseBody);
        content.add(databaseCard);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        status.setFont(UITheme.FONT_BODY);
        status.setForeground(UITheme.TEXT_SECONDARY);
        statusPanel.add(status, BorderLayout.CENTER);
        content.add(statusPanel);
        add(content, BorderLayout.NORTH);

        setRecruiter(recruiter);
        setDatabaseConfig(databaseConfig);
        resetButton.addActionListener(event -> resetProfile());
        saveButton.addActionListener(event -> saveProfile());
        testConnectionButton.addActionListener(event -> testDatabaseConnection());
    }

    public void setProfileSaveHandler(Consumer<String> handler) {
        profileSaveHandler = handler;
    }

    public void setDatabaseTestHandler(Runnable handler) {
        databaseTestHandler = handler;
    }

    public void setRecruiter(Recruiter recruiter) {
        Objects.requireNonNull(recruiter, "recruiter");
        savedName = recruiter.getFullName() == null ? "" : recruiter.getFullName().trim();
        fullNameField.setText(savedName);
        emailValue.setText(recruiter.getEmail() == null ? "—" : recruiter.getEmail());
    }

    public void setDatabaseConfig(DatabaseConfig config) {
        Objects.requireNonNull(config, "config");
        databaseTarget.setText(String.format(
            "<html><b>Host:</b> %s:%d &nbsp;&nbsp; <b>Database:</b> %s &nbsp;&nbsp; <b>User:</b> %s</html>",
            escapeHtml(config.getHost()), config.getPort(), escapeHtml(config.getDatabaseName()),
            escapeHtml(config.getUsername())
        ));
    }

    public void setProfileSaving(boolean saving) {
        saveButton.setEnabled(!saving);
        resetButton.setEnabled(!saving);
        fullNameField.setEnabled(!saving);
        if (saving) {
            showStatus("Saving profile...", false);
        }
    }

    public void setDatabaseTesting(boolean testing) {
        testConnectionButton.setEnabled(!testing);
        if (testing) {
            databaseStatus.setForeground(UITheme.TEXT_SECONDARY);
            databaseStatus.setText("Checking database connection...");
            showStatus("Testing database connection...", false);
        }
    }

    public void showProfileSaved(String name) {
        savedName = name;
        fullNameField.setText(name);
        showStatus("Profile updated successfully.", false);
    }

    public void showDatabaseTestResult(boolean connected) {
        String message = connected
            ? "Connected to MySQL."
            : "Not connected. Check the database configuration and connection, then retry.";
        databaseStatus.setForeground(connected ? UITheme.SUCCESS : UITheme.ERROR);
        databaseStatus.setText(message);
        showStatus(message, !connected);
    }

    public void showDatabaseTestFailure(String message) {
        String displayedMessage = message == null || message.isBlank()
            ? "The database connection test failed unexpectedly."
            : message;
        databaseStatus.setForeground(UITheme.ERROR);
        databaseStatus.setText(displayedMessage);
        showStatus(displayedMessage, true);
    }

    public void showStatus(String message, boolean error) {
        status.setForeground(error ? UITheme.ERROR : UITheme.TEXT_SECONDARY);
        status.setText(message);
    }

    public JTextField getFullNameField() {
        return fullNameField;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }

    public JButton getTestConnectionButton() {
        return testConnectionButton;
    }

    public String getStatusMessage() {
        return status.getText();
    }

    public String getDatabaseTargetText() {
        return databaseTarget.getText();
    }

    public String getDatabaseStatusText() {
        return databaseStatus.getText();
    }

    private void saveProfile() {
        String name = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        if (name.length() < 2 || name.length() > 100) {
            showStatus("Full name must be between 2 and 100 characters.", true);
            return;
        }
        if (profileSaveHandler == null) {
            showStatus("Profile saving is unavailable.", true);
            return;
        }
        profileSaveHandler.accept(name);
    }

    private void resetProfile() {
        fullNameField.setText(savedName);
        showStatus("Unsaved profile changes were discarded.", false);
    }

    private void testDatabaseConnection() {
        if (databaseTestHandler == null) {
            showStatus("Database connection testing is unavailable.", true);
            return;
        }
        databaseTestHandler.run();
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
