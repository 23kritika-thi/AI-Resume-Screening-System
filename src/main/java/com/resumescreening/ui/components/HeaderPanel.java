package com.resumescreening.ui.components;

import com.resumescreening.ui.theme.UITheme;
import com.resumescreening.ui.components.SecondaryButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Top application header displaying current page title, application context,
 * and recruiter profile area.
 */
public class HeaderPanel extends JPanel {

    private final JLabel pageTitleLabel;
    private final JLabel subtitleLabel;
    private final JLabel nameLabel;
    private final AvatarBadge avatar;
    private final JButton logoutButton;

    public HeaderPanel() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 72));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.CARD_BORDER),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        // LEFT: Page Title & Subtitle
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        pageTitleLabel = new JLabel("Dashboard");
        pageTitleLabel.setFont(UITheme.FONT_TITLE);
        pageTitleLabel.setForeground(UITheme.TEXT_PRIMARY);

        subtitleLabel = new JLabel("AI-Based Resume Screening and Candidate Ranking System");
        subtitleLabel.setFont(UITheme.FONT_CAPTION);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);

        leftPanel.add(pageTitleLabel);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(subtitleLabel);

        add(leftPanel, BorderLayout.WEST);

        // RIGHT: Recruiter Profile Area
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        // Profile Avatar
        avatar = new AvatarBadge("R", UITheme.PRIMARY);
        rightPanel.add(avatar);

        // Recruiter Name & Title
        JPanel profileText = new JPanel();
        profileText.setOpaque(false);
        profileText.setLayout(new BoxLayout(profileText, BoxLayout.Y_AXIS));

        nameLabel = new JLabel("Recruiter");
        nameLabel.setFont(UITheme.FONT_BODY_BOLD);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel roleLabel = new JLabel("Recruiter • Online");
        roleLabel.setFont(UITheme.FONT_CAPTION);
        roleLabel.setForeground(UITheme.SUCCESS);

        profileText.add(nameLabel);
        profileText.add(Box.createVerticalStrut(2));
        profileText.add(roleLabel);

        rightPanel.add(profileText);

        logoutButton = new SecondaryButton("Sign out");
        rightPanel.add(logoutButton);

        add(rightPanel, BorderLayout.EAST);
    }

    public void setPageTitle(String title, String subtitle) {
        pageTitleLabel.setText(title);
        subtitleLabel.setText(subtitle != null ? subtitle : "AI-Based Resume Screening and Candidate Ranking System");
    }

    public void setRecruiterName(String fullName) {
        String name = fullName == null || fullName.isBlank() ? "Recruiter" : fullName.trim();
        nameLabel.setText(name);
        avatar.setInitials(getInitials(name));
    }

    public void addLogoutListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    private static String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
            .toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Circular avatar badge displaying recruiter initials.
     */
    private static class AvatarBadge extends JPanel {
        private final JLabel initialsLabel;
        private final Color bgColor;

        public AvatarBadge(String initials, Color bgColor) {
            this.bgColor = bgColor;
            setOpaque(false);
            setPreferredSize(new Dimension(42, 42));
            setLayout(new BorderLayout());

            initialsLabel = new JLabel(initials, JLabel.CENTER);
            initialsLabel.setFont(UITheme.FONT_BODY_BOLD);
            initialsLabel.setForeground(Color.WHITE);
            add(initialsLabel, BorderLayout.CENTER);
        }

        public void setInitials(String initials) {
            initialsLabel.setText(initials);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
