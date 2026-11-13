package com.resumescreening.ui.views;

import com.resumescreening.ui.components.PrimaryButton;
import com.resumescreening.ui.components.UIIcon;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Login and registration forms for recruiter accounts.
 */
public class AuthenticationView extends JPanel {

    private static final String LOGIN = "LOGIN";
    private static final String REGISTER = "REGISTER";
    private static final String RECOVERY = "RECOVERY";
    private static final String RECOVERY_EMAIL = "RECOVERY_EMAIL";
    private static final String RECOVERY_CODE = "RECOVERY_CODE";
    private static final String RECOVERY_PASSWORD = "RECOVERY_PASSWORD";

    private final CardLayout formLayout = new CardLayout();
    private final JPanel formCards = new JPanel(formLayout);
    private final JTextField loginEmail = new JTextField();
    private final JPasswordField loginPassword = new JPasswordField();
    private final JTextField registerName = new JTextField();
    private final JTextField registerEmail = new JTextField();
    private final JPasswordField registerPassword = new JPasswordField();
    private final JPasswordField registerConfirmation = new JPasswordField();
    private final JTextField recoveryEmail = new JTextField();
    private final JLabel recoveryEmailDisplay = new JLabel();
    private final JTextField recoveryCode = new JTextField();
    private final JPasswordField newPassword = new JPasswordField();
    private final JPasswordField newPasswordConfirmation = new JPasswordField();
    private final JLabel loginMessage = createMessageLabel();
    private final JLabel registerMessage = createMessageLabel();
    private final JLabel recoveryEmailMessage = createMessageLabel();
    private final JLabel recoveryCodeMessage = createMessageLabel();
    private final JLabel recoveryPasswordMessage = createMessageLabel();
    private BiConsumer<String, String> loginHandler;
    private RegistrationHandler registrationHandler;
    private RecoveryHandler recoveryHandler;
    private final CardLayout recoveryLayout = new CardLayout();
    private JPanel recoveryStages;
    private String activeRecoveryStage = RECOVERY_EMAIL;

    public AuthenticationView() {
        setLayout(new GridBagLayout());
        setBackground(UITheme.BG_CANVAS);

        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.CARD_BORDER),
            BorderFactory.createEmptyBorder(32, 38, 32, 38)
        ));
        card.setPreferredSize(new Dimension(460, 590));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel(new UIIcon(UIIcon.Kind.BOLT, 22, UITheme.PRIMARY));
        JLabel brandName = new JLabel("ResumeAI");
        brandName.setFont(UITheme.FONT_BRAND);
        brandName.setForeground(UITheme.TEXT_PRIMARY);
        brand.add(logo);
        brand.add(brandName);
        card.add(brand, BorderLayout.NORTH);

        formCards.setOpaque(false);
        formCards.add(buildLoginForm(), LOGIN);
        formCards.add(buildRegistrationForm(), REGISTER);
        formCards.add(buildRecoveryForm(), RECOVERY);
        card.add(formCards, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        add(card, constraints);
    }

    public void setLoginHandler(BiConsumer<String, String> handler) {
        loginHandler = handler;
    }

    public void setRegistrationHandler(RegistrationHandler handler) {
        registrationHandler = handler;
    }

    public void setRecoveryHandler(RecoveryHandler handler) {
        recoveryHandler = handler;
    }

    public void showLogin(String email, String message) {
        formLayout.show(formCards, LOGIN);
        if (email != null) {
            loginEmail.setText(email);
        }
        recoveryCode.setText("");
        newPassword.setText("");
        newPasswordConfirmation.setText("");
        setMessage(loginMessage, message, UITheme.SUCCESS);
    }

    public void showRegistration() {
        formLayout.show(formCards, REGISTER);
        setMessage(registerMessage, null, UITheme.TEXT_MUTED);
    }

    public void showLoginMessage(String message) {
        setMessage(loginMessage, message, UITheme.WARNING);
    }

    public void showRegistrationMessage(String message) {
        setMessage(registerMessage, message, UITheme.WARNING);
    }

    public void showRecovery() {
        formLayout.show(formCards, RECOVERY);
        recoveryLayout.show(recoveryStages, RECOVERY_EMAIL);
        activeRecoveryStage = RECOVERY_EMAIL;
        recoveryEmail.setEnabled(true);
        recoveryEmail.setText(loginEmail.getText());
        recoveryCode.setText("");
        newPassword.setText("");
        newPasswordConfirmation.setText("");
        setMessage(recoveryEmailMessage, null, UITheme.TEXT_MUTED);
        setMessage(recoveryCodeMessage, null, UITheme.TEXT_MUTED);
        setMessage(recoveryPasswordMessage, null, UITheme.TEXT_MUTED);
    }

    public void showRecoveryEmailMessage(String message, boolean isError) {
        recoveryLayout.show(recoveryStages, RECOVERY_EMAIL);
        activeRecoveryStage = RECOVERY_EMAIL;
        recoveryEmail.setEnabled(true);
        setMessage(recoveryEmailMessage, message, isError ? UITheme.WARNING : UITheme.SUCCESS);
    }

    public void showRecoveryCodeStep(String message) {
        recoveryLayout.show(recoveryStages, RECOVERY_CODE);
        activeRecoveryStage = RECOVERY_CODE;
        recoveryEmail.setEnabled(false);
        recoveryEmailDisplay.setText(recoveryEmail.getText());
        recoveryCode.setText("");
        setMessage(recoveryCodeMessage, message, UITheme.TEXT_SECONDARY);
    }

    public void showRecoveryPasswordStep(String message) {
        recoveryLayout.show(recoveryStages, RECOVERY_PASSWORD);
        activeRecoveryStage = RECOVERY_PASSWORD;
        recoveryEmail.setEnabled(false);
        setMessage(recoveryPasswordMessage, message, UITheme.SUCCESS);
    }

    public void showRecoveryStepMessage(String message) {
        JLabel activeMessage = switch (activeRecoveryStage) {
            case RECOVERY_CODE -> recoveryCodeMessage;
            case RECOVERY_PASSWORD -> recoveryPasswordMessage;
            default -> recoveryEmailMessage;
        };
        setMessage(activeMessage, message, UITheme.WARNING);
    }

    private JPanel buildLoginForm() {
        JPanel form = createFormPanel();
        addHeading(form, "Welcome back", "Sign in to your recruiter account");
        addField(form, "Email address", loginEmail);
        addField(form, "Password", loginPassword);

        JPanel forgotPasswordRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotPasswordRow.setOpaque(false);
        JButton forgotPassword = createLinkButton("Forgot Password?");
        forgotPassword.addActionListener(event -> showRecovery());
        forgotPasswordRow.add(forgotPassword);
        forgotPasswordRow.setAlignmentX(LEFT_ALIGNMENT);
        form.add(forgotPasswordRow);
        form.add(loginMessage);
        form.add(Box.createVerticalStrut(12));

        PrimaryButton signIn = new PrimaryButton("Sign in");
        signIn.setAlignmentX(LEFT_ALIGNMENT);
        signIn.addActionListener(event -> submitLogin());
        form.add(signIn);

        JPanel registerLink = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        registerLink.setOpaque(false);
        JLabel prompt = new JLabel("New to ResumeAI? ");
        prompt.setFont(UITheme.FONT_BODY);
        prompt.setForeground(UITheme.TEXT_SECONDARY);
        JButton link = createLinkButton("Create an account");
        link.addActionListener(event -> showRegistration());
        registerLink.add(prompt);
        registerLink.add(link);
        form.add(registerLink);
        return form;
    }

    private JPanel buildRecoveryForm() {
        recoveryStages = new JPanel(recoveryLayout);
        recoveryStages.setOpaque(false);
        recoveryStages.add(buildRecoveryEmailStage(), RECOVERY_EMAIL);
        recoveryStages.add(buildRecoveryCodeStage(), RECOVERY_CODE);
        recoveryStages.add(buildRecoveryPasswordStage(), RECOVERY_PASSWORD);
        return recoveryStages;
    }

    private JPanel buildRecoveryEmailStage() {
        JPanel form = createFormPanel();
        addHeading(form, "Forgot password?", "Enter your registered email address.");
        addField(form, "Email address", recoveryEmail);
        form.add(recoveryEmailMessage);
        form.add(Box.createVerticalStrut(12));

        PrimaryButton sendCode = new PrimaryButton("Send verification code");
        sendCode.setAlignmentX(LEFT_ALIGNMENT);
        sendCode.addActionListener(event -> submitRecoveryRequest());
        form.add(sendCode);
        addBackToLoginLink(form);
        return form;
    }

    private JPanel buildRecoveryCodeStage() {
        JPanel form = createFormPanel();
        addHeading(form, "Check your email", "Enter the 8-digit code. It expires in 10 minutes.");
        JLabel emailLabel = new JLabel("Recovery email");
        emailLabel.setFont(UITheme.FONT_BODY_BOLD);
        emailLabel.setForeground(UITheme.TEXT_SECONDARY);
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);
        recoveryEmailDisplay.setFont(UITheme.FONT_BODY);
        recoveryEmailDisplay.setForeground(UITheme.TEXT_PRIMARY);
        recoveryEmailDisplay.setAlignmentX(LEFT_ALIGNMENT);
        form.add(emailLabel);
        form.add(Box.createVerticalStrut(5));
        form.add(recoveryEmailDisplay);
        form.add(Box.createVerticalStrut(12));
        addField(form, "Verification code", recoveryCode);
        form.add(recoveryCodeMessage);
        form.add(Box.createVerticalStrut(12));

        PrimaryButton verifyCode = new PrimaryButton("Verify code");
        verifyCode.setAlignmentX(LEFT_ALIGNMENT);
        verifyCode.addActionListener(event -> submitCodeVerification());
        form.add(verifyCode);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        links.setOpaque(false);
        JButton resend = createLinkButton("Send another code");
        resend.addActionListener(event -> submitRecoveryRequest());
        links.add(resend);
        form.add(links);
        addBackToLoginLink(form);
        return form;
    }

    private JPanel buildRecoveryPasswordStage() {
        JPanel form = createFormPanel();
        addHeading(form, "Choose a new password", "Use a password between 8 and 128 characters.");
        addField(form, "New password", newPassword);
        addField(form, "Confirm new password", newPasswordConfirmation);
        form.add(recoveryPasswordMessage);
        form.add(Box.createVerticalStrut(12));

        PrimaryButton resetPassword = new PrimaryButton("Reset password");
        resetPassword.setAlignmentX(LEFT_ALIGNMENT);
        resetPassword.addActionListener(event -> submitPasswordReset());
        form.add(resetPassword);
        addBackToLoginLink(form);
        return form;
    }

    private void addBackToLoginLink(JPanel form) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        row.setOpaque(false);
        JButton back = createLinkButton("Back to sign in");
        back.addActionListener(event -> showLogin(null, null));
        row.add(back);
        form.add(row);
    }

    private JPanel buildRegistrationForm() {
        JPanel form = createFormPanel();
        addHeading(form, "Create your account", "Register as a recruiter to get started");
        addField(form, "Full name", registerName);
        addField(form, "Email address", registerEmail);
        addField(form, "Password (at least 8 characters)", registerPassword);
        addField(form, "Confirm password", registerConfirmation);
        form.add(registerMessage);
        form.add(Box.createVerticalStrut(8));

        PrimaryButton createAccount = new PrimaryButton("Create account");
        createAccount.setAlignmentX(LEFT_ALIGNMENT);
        createAccount.addActionListener(event -> submitRegistration());
        form.add(createAccount);

        JPanel loginLink = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        loginLink.setOpaque(false);
        JLabel prompt = new JLabel("Already registered? ");
        prompt.setFont(UITheme.FONT_BODY);
        prompt.setForeground(UITheme.TEXT_SECONDARY);
        JButton link = createLinkButton("Back to sign in");
        link.addActionListener(event -> showLogin(null, null));
        loginLink.add(prompt);
        loginLink.add(link);
        form.add(loginLink);
        return form;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        return form;
    }

    private void addHeading(JPanel form, String title, String subtitle) {
        JLabel heading = new JLabel(title);
        heading.setFont(UITheme.FONT_TITLE);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        JLabel description = new JLabel(subtitle);
        description.setFont(UITheme.FONT_BODY);
        description.setForeground(UITheme.TEXT_SECONDARY);
        description.setAlignmentX(LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(4));
        form.add(description);
        form.add(Box.createVerticalStrut(18));
    }

    private void addField(JPanel form, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        field.setFont(UITheme.FONT_BODY);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setPreferredSize(new Dimension(380, 38));
        field.setAlignmentX(LEFT_ALIGNMENT);
        form.add(label);
        form.add(Box.createVerticalStrut(5));
        form.add(field);
        form.add(Box.createVerticalStrut(12));
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UITheme.FONT_BODY_BOLD);
        button.setForeground(UITheme.PRIMARY);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        return button;
    }

    private void submitLogin() {
        if (loginHandler == null) {
            throw new IllegalStateException("Login handler has not been configured.");
        }
        char[] password = loginPassword.getPassword();
        try {
            loginHandler.accept(loginEmail.getText(), new String(password));
        } finally {
            Arrays.fill(password, '\0');
            loginPassword.setText("");
        }
    }

    private void submitRegistration() {
        if (registrationHandler == null) {
            throw new IllegalStateException("Registration handler has not been configured.");
        }
        char[] password = registerPassword.getPassword();
        char[] confirmation = registerConfirmation.getPassword();
        try {
            registrationHandler.register(
                registerName.getText(),
                registerEmail.getText(),
                new String(password),
                new String(confirmation)
            );
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            registerPassword.setText("");
            registerConfirmation.setText("");
        }
    }

    private void submitRecoveryRequest() {
        if (recoveryHandler == null) {
            throw new IllegalStateException("Password recovery handler has not been configured.");
        }
        recoveryHandler.requestCode(recoveryEmail.getText());
    }

    private void submitCodeVerification() {
        if (recoveryHandler == null) {
            throw new IllegalStateException("Password recovery handler has not been configured.");
        }
        recoveryHandler.verifyCode(recoveryEmail.getText(), recoveryCode.getText().trim());
    }

    private void submitPasswordReset() {
        if (recoveryHandler == null) {
            throw new IllegalStateException("Password recovery handler has not been configured.");
        }
        char[] password = newPassword.getPassword();
        char[] confirmation = newPasswordConfirmation.getPassword();
        try {
            recoveryHandler.resetPassword(
                recoveryEmail.getText(),
                new String(password),
                new String(confirmation)
            );
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            newPassword.setText("");
            newPasswordConfirmation.setText("");
        }
    }

    private static JLabel createMessageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(UITheme.FONT_BODY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private static void setMessage(JLabel label, String message, Color color) {
        if (message == null || message.isBlank()) {
            label.setText(" ");
        } else {
            String escaped = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            label.setText("<html><body style='width: 350px'>" + escaped + "</body></html>");
        }
        label.setForeground(color);
    }

    @FunctionalInterface
    public interface RegistrationHandler {
        void register(String fullName, String email, String password, String confirmation);
    }

    public interface RecoveryHandler {
        void requestCode(String email);

        void verifyCode(String email, String code);

        void resetPassword(String email, String newPassword, String confirmation);
    }
}
