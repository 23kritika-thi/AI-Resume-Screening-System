package com.resumescreening.ui.components;

import com.resumescreening.ui.theme.UITheme;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * Custom styled button designed for the sidebar navigation menu.
 */
public class NavItemButton extends JButton {

    private final String navId;
    private final UIIcon.Kind iconKind;
    private boolean active = false;
    private boolean hovered = false;

    public NavItemButton(String navId, String label, UIIcon.Kind iconKind) {
        super(label);
        this.navId = navId;
        this.iconKind = iconKind;

        setFont(UITheme.FONT_BODY_BOLD);
        setForeground(UITheme.SIDEBAR_TEXT);
        setIcon(new UIIcon(iconKind, 18, UITheme.SIDEBAR_TEXT));
        setIconTextGap(12);
        setHorizontalAlignment(SwingConstants.LEFT);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        setPreferredSize(new Dimension(210, 42));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    public String getNavId() {
        return navId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setForeground(active ? UITheme.SIDEBAR_TEXT_ACTIVE : UITheme.SIDEBAR_TEXT);
        setIcon(new UIIcon(iconKind, 18, active ? UITheme.SIDEBAR_TEXT_ACTIVE : UITheme.SIDEBAR_TEXT));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (active) {
            // Active background
            g2.setColor(UITheme.SIDEBAR_ACTIVE);
            g2.fillRoundRect(8, 2, width - 16, height - 4, 8, 8);
        } else if (hovered) {
            // Hovered background
            g2.setColor(UITheme.SIDEBAR_HOVER);
            g2.fillRoundRect(8, 2, width - 16, height - 4, 8, 8);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
