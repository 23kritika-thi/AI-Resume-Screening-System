package com.resumescreening.ui.components;

import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Reusable metric statistic card for dashboard overviews.
 */
public class StatCard extends JPanel {

    private final String title;
    private JLabel valueLabel;
    private final String subtitle;
    private final UIIcon.Kind iconKind;
    private final Color accentColor;
    private final Color badgeBgColor;

    public StatCard(String title, String value, String subtitle, UIIcon.Kind iconKind, Color accentColor, Color badgeBgColor) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconKind = iconKind;
        this.accentColor = accentColor;
        this.badgeBgColor = badgeBgColor;
        valueLabel = new JLabel(value);

        setOpaque(false);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setPreferredSize(new Dimension(220, 150));
        setMinimumSize(new Dimension(180, 140));

        buildUI();
    }

    private void buildUI() {
        // Top row: Title and Icon badge
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SUBHEADER);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);
        topPanel.add(titleLabel, BorderLayout.WEST);

        // Icon badge
        IconBadge badge = new IconBadge(iconKind, accentColor, badgeBgColor);
        topPanel.add(badge, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center: Large Metric Value
        valueLabel.setFont(UITheme.FONT_STAT_VALUE);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        add(valueLabel, BorderLayout.CENTER);

        // Bottom: Trend / Subtitle
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPanel.setOpaque(false);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(UITheme.FONT_CAPTION);
        subLabel.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(subLabel);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public String getValue() {
        return valueLabel.getText();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Card background
        g2.setColor(UITheme.CARD_BG);
        g2.fillRoundRect(0, 0, width - 1, height - 1, 14, 14);

        // Subtle border
        g2.setColor(UITheme.CARD_BORDER);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Circular badge with a vector icon.
     */
    private static class IconBadge extends JPanel {
        private final Color bgColor;

        public IconBadge(UIIcon.Kind iconKind, Color iconColor, Color bgColor) {
            this.bgColor = bgColor;

            setOpaque(false);
            setPreferredSize(new Dimension(36, 36));
            setLayout(new BorderLayout());

            JLabel label = new JLabel(new UIIcon(iconKind, 18, iconColor));
            add(label, BorderLayout.CENTER);
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
