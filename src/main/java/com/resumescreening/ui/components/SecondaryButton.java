package com.resumescreening.ui.components;

import com.resumescreening.ui.theme.UITheme;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * Modern secondary button with white background, subtle border, and hover elevation.
 */
public class SecondaryButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;

    public SecondaryButton(String text) {
        super(text);
        setFont(UITheme.FONT_BODY_BOLD);
        setForeground(UITheme.TEXT_PRIMARY);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        if (isPressed) {
            g2.setColor(UITheme.PRIMARY_LIGHT);
        } else if (isHovered) {
            g2.setColor(new Color(241, 245, 249)); // Slate-100
        } else {
            g2.setColor(Color.WHITE);
        }
        g2.fillRoundRect(0, 0, width - 1, height - 1, 8, 8);

        // Border
        g2.setColor(isHovered ? UITheme.PRIMARY : UITheme.CARD_BORDER);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);

        g2.dispose();
        super.paintComponent(g);
    }
}
