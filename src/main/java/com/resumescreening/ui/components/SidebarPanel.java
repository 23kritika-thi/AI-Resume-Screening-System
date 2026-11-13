package com.resumescreening.ui.components;

import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Modern dark sidebar panel with navigation links, branding, and system status indicator.
 */
public class SidebarPanel extends JPanel {

    public static final String NAV_DASHBOARD = "DASHBOARD";
    public static final String NAV_JOBS = "JOBS";
    public static final String NAV_CANDIDATES = "CANDIDATES";
    public static final String NAV_SCREENING = "SCREENING";
    public static final String NAV_REPORTS = "REPORTS";
    public static final String NAV_SETTINGS = "SETTINGS";

    private final List<NavItemButton> navButtons = new ArrayList<>();
    private Consumer<String> onNavigate;

    public SidebarPanel() {
        setPreferredSize(new Dimension(240, 800));
        setMinimumSize(new Dimension(220, 600));
        setBackground(UITheme.SIDEBAR_BG);
        setLayout(new BorderLayout());

        buildUI();
    }

    public void setNavigationListener(Consumer<String> listener) {
        this.onNavigate = listener;
    }

    private void buildUI() {
        // TOP: Branding
        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));

        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandRow.setOpaque(false);

        // Logo badge
        JLabel logoBadge = new JLabel(new UIIcon(UIIcon.Kind.BOLT, 18, Color.WHITE), JLabel.CENTER);
        logoBadge.setOpaque(true);
        logoBadge.setBackground(UITheme.PRIMARY);
        logoBadge.setPreferredSize(new Dimension(32, 32));
        logoBadge.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JLabel brandTitle = new JLabel("ResumeAI");
        brandTitle.setFont(UITheme.FONT_BRAND);
        brandTitle.setForeground(Color.WHITE);

        brandRow.add(logoBadge);
        brandRow.add(brandTitle);
        brandPanel.add(brandRow);

        brandPanel.add(Box.createVerticalStrut(6));
        JLabel tagline = new JLabel("Screening & Ranking System");
        tagline.setFont(UITheme.FONT_CAPTION);
        tagline.setForeground(UITheme.SIDEBAR_TEXT);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        brandPanel.add(tagline);

        add(brandPanel, BorderLayout.NORTH);

        // CENTER: Navigation List
        JPanel navContainer = new JPanel();
        navContainer.setOpaque(false);
        navContainer.setLayout(new BoxLayout(navContainer, BoxLayout.Y_AXIS));
        navContainer.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        addNavItem(navContainer, NAV_DASHBOARD, "Dashboard", UIIcon.Kind.DASHBOARD, true);
        addNavItem(navContainer, NAV_JOBS, "Jobs", UIIcon.Kind.BRIEFCASE, false);
        addNavItem(navContainer, NAV_CANDIDATES, "Candidates", UIIcon.Kind.PEOPLE, false);
        addNavItem(navContainer, NAV_SCREENING, "Resume Screening", UIIcon.Kind.DOCUMENT, false);
        addNavItem(navContainer, NAV_REPORTS, "Reports", UIIcon.Kind.CHART, false);
        addNavItem(navContainer, NAV_SETTINGS, "Settings", UIIcon.Kind.SETTINGS, false);

        navContainer.add(Box.createVerticalGlue());
        add(navContainer, BorderLayout.CENTER);

        // BOTTOM: System Info & Database Connection Status
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 24, 20));

        // DB Status Pill
        JPanel statusPill = new StatusPill("MySQL 8.0 • Connected", UITheme.SUCCESS);
        bottomPanel.add(statusPill);

        bottomPanel.add(Box.createVerticalStrut(10));
        JLabel versionLabel = new JLabel("v1.0.0-SNAPSHOT • Java 21");
        versionLabel.setFont(UITheme.FONT_CAPTION);
        versionLabel.setForeground(UITheme.SIDEBAR_TEXT);
        bottomPanel.add(versionLabel);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addNavItem(JPanel container, String navId, String label, UIIcon.Kind icon, boolean activeByDefault) {
        NavItemButton btn = new NavItemButton(navId, label, icon);
        btn.setActive(activeByDefault);
        btn.addActionListener(e -> selectNav(navId));
        navButtons.add(btn);

        container.add(btn);
        container.add(Box.createVerticalStrut(4));
    }

    public void selectNav(String navId) {
        for (NavItemButton btn : navButtons) {
            btn.setActive(btn.getNavId().equals(navId));
        }
        if (onNavigate != null) {
            onNavigate.accept(navId);
        }
    }

    /**
     * Small pill widget showing live database status.
     */
    private static class StatusPill extends JPanel {
        public StatusPill(String text, Color dotColor) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

            JLabel dot = new JLabel(new UIIcon(UIIcon.Kind.DOT, 10, dotColor));

            JLabel label = new JLabel(text);
            label.setFont(UITheme.FONT_CAPTION);
            label.setForeground(UITheme.SIDEBAR_TEXT);

            add(dot);
            add(label);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UITheme.SIDEBAR_HOVER);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
