package com.resumescreening.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Small vector icons drawn with Java2D so they render consistently without emoji fonts.
 */
public final class UIIcon implements Icon {

    public enum Kind {
        BRIEFCASE,
        PEOPLE,
        DOCUMENT,
        STAR,
        CHART,
        SETTINGS,
        DASHBOARD,
        UPLOAD,
        DOWNLOAD,
        BOLT,
        DOT
    }

    private final Kind kind;
    private final int size;
    private final Color color;

    public UIIcon(Kind kind, int size, Color color) {
        this.kind = kind;
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.translate(x, y);
        g2.scale(size / 24.0, size / 24.0);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (kind) {
            case BRIEFCASE:
                paintBriefcase(g2);
                break;
            case PEOPLE:
                paintPeople(g2);
                break;
            case DOCUMENT:
                paintDocument(g2);
                break;
            case STAR:
                paintStar(g2);
                break;
            case CHART:
                paintChart(g2);
                break;
            case SETTINGS:
                paintSettings(g2);
                break;
            case DASHBOARD:
                paintDashboard(g2);
                break;
            case UPLOAD:
                paintTransfer(g2, true);
                break;
            case DOWNLOAD:
                paintTransfer(g2, false);
                break;
            case BOLT:
                paintBolt(g2);
                break;
            case DOT:
                g2.fill(new Ellipse2D.Double(9, 9, 6, 6));
                break;
            default:
                throw new IllegalStateException("Unsupported icon kind: " + kind);
        }

        g2.dispose();
    }

    private static void paintBriefcase(Graphics2D g2) {
        g2.draw(new RoundRectangle2D.Double(3, 8, 18, 12, 2, 2));
        g2.draw(new RoundRectangle2D.Double(8, 4, 8, 4, 1, 1));
        g2.drawLine(3, 13, 21, 13);
        g2.drawLine(10, 12, 14, 12);
    }

    private static void paintPeople(Graphics2D g2) {
        g2.draw(new Ellipse2D.Double(8, 3, 7, 7));
        g2.draw(new Ellipse2D.Double(16, 6, 5, 5));
        g2.draw(new RoundRectangle2D.Double(4, 12, 15, 9, 5, 5));
        g2.draw(new RoundRectangle2D.Double(15, 13, 7, 7, 4, 4));
    }

    private static void paintDocument(Graphics2D g2) {
        Path2D page = new Path2D.Double();
        page.moveTo(6, 3);
        page.lineTo(15, 3);
        page.lineTo(20, 8);
        page.lineTo(20, 21);
        page.lineTo(6, 21);
        page.closePath();
        g2.draw(page);
        g2.drawLine(15, 3, 15, 8);
        g2.drawLine(15, 8, 20, 8);
        g2.drawLine(9, 12, 17, 12);
        g2.drawLine(9, 16, 17, 16);
    }

    private static void paintStar(Graphics2D g2) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double radius = i % 2 == 0 ? 9 : 4;
            double px = 12 + Math.cos(angle) * radius;
            double py = 12 + Math.sin(angle) * radius;
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.closePath();
        g2.draw(star);
    }

    private static void paintChart(Graphics2D g2) {
        g2.drawLine(4, 20, 21, 20);
        g2.drawLine(5, 20, 5, 5);
        g2.draw(new RoundRectangle2D.Double(8, 12, 3, 8, 1, 1));
        g2.draw(new RoundRectangle2D.Double(14, 8, 3, 12, 1, 1));
        g2.draw(new RoundRectangle2D.Double(19, 4, 3, 16, 1, 1));
    }

    private static void paintSettings(Graphics2D g2) {
        g2.draw(new Ellipse2D.Double(8, 8, 8, 8));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double innerX = 12 + Math.cos(angle) * 6;
            double innerY = 12 + Math.sin(angle) * 6;
            double outerX = 12 + Math.cos(angle) * 9;
            double outerY = 12 + Math.sin(angle) * 9;
            g2.drawLine((int) innerX, (int) innerY, (int) outerX, (int) outerY);
        }
    }

    private static void paintDashboard(Graphics2D g2) {
        g2.draw(new RoundRectangle2D.Double(3, 3, 8, 8, 2, 2));
        g2.draw(new RoundRectangle2D.Double(13, 3, 8, 8, 2, 2));
        g2.draw(new RoundRectangle2D.Double(3, 13, 8, 8, 2, 2));
        g2.draw(new RoundRectangle2D.Double(13, 13, 8, 8, 2, 2));
    }

    private static void paintTransfer(Graphics2D g2, boolean upload) {
        g2.draw(new RoundRectangle2D.Double(3, 15, 18, 5, 1, 1));
        g2.drawLine(7, 15, 7, 18);
        g2.drawLine(17, 15, 17, 18);
        if (upload) {
            g2.drawLine(12, 14, 12, 5);
            g2.drawLine(12, 5, 8, 9);
            g2.drawLine(12, 5, 16, 9);
        } else {
            g2.drawLine(12, 5, 12, 14);
            g2.drawLine(12, 14, 8, 10);
            g2.drawLine(12, 14, 16, 10);
        }
    }

    private static void paintBolt(Graphics2D g2) {
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(13, 2);
        bolt.lineTo(5, 13);
        bolt.lineTo(11, 13);
        bolt.lineTo(10, 22);
        bolt.lineTo(19, 10);
        bolt.lineTo(13, 10);
        bolt.closePath();
        g2.draw(bolt);
    }
}
