package com.resumescreening.ui.components;

import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Modern card container panel with clean borders, header, and content area.
 */
public class ContentCard extends JPanel {

    private final JPanel headerPanel;
    private final JPanel contentPanel;

    public ContentCard(String title, String subtitle) {
        this(title, subtitle, null);
    }

    public ContentCard(String title, String subtitle, Component actionComponent) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));

        // Header
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JPanel titlesBox = new JPanel();
        titlesBox.setOpaque(false);
        titlesBox.setLayout(new BoxLayout(titlesBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_HEADER);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titlesBox.add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            titlesBox.add(Box.createVerticalStrut(4));
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(UITheme.FONT_CAPTION);
            subtitleLabel.setForeground(UITheme.TEXT_MUTED);
            titlesBox.add(subtitleLabel);
        }

        headerPanel.add(titlesBox, BorderLayout.WEST);

        if (actionComponent != null) {
            headerPanel.add(actionComponent, BorderLayout.EAST);
        }

        add(headerPanel, BorderLayout.NORTH);

        // Body content
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void setBodyComponent(Component comp) {
        contentPanel.removeAll();
        if (comp != null) {
            contentPanel.add(comp, BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public JPanel getContentPanel() {
        return contentPanel;
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

        // Border
        g2.setColor(UITheme.CARD_BORDER);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);

        g2.dispose();
        super.paintComponent(g);
    }
}
