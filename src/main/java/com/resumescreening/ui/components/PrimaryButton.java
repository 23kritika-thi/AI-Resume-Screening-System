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

/**
 * Modern primary button with smooth hover state and rounded corners.
 */
public class PrimaryButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;

    public PrimaryButton(String text) {
        super(text);
        setFont(UITheme.FONT_BODY_BOLD);
        setForeground(java.awt.Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));

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

        if (!isEnabled()) {
            g2.setColor(UITheme.TEXT_MUTED);
        } else if (isPressed) {
            g2.setColor(UITheme.PRIMARY_HOVER.darker());
        } else if (isHovered) {
            g2.setColor(UITheme.PRIMARY_HOVER);
        } else {
            g2.setColor(UITheme.PRIMARY);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();

        super.paintComponent(g);
    }
}
