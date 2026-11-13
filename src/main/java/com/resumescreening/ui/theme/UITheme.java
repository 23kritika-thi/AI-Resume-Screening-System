package com.resumescreening.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Central design tokens, color palette, typography, and FlatLaf theme initialization.
 */
public final class UITheme {

    private UITheme() {
    }

    // ==========================================
    // Color Palette
    // ==========================================
    // Primary Brand Colors
    public static final Color PRIMARY = new Color(37, 99, 235);        // #2563EB Blue-600
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);  // #1D4ED8 Blue-700
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255);// #EFF6FF Blue-50

    // Sidebar & Navigation
    public static final Color SIDEBAR_BG = new Color(15, 23, 42);      // #0F172A Slate-900
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);   // #1E293B Slate-800
    public static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235); // #2563EB Blue-600
    public static final Color SIDEBAR_TEXT = new Color(148, 163, 184); // #94A3B8 Slate-400
    public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;
    public static final Color SIDEBAR_BORDER = new Color(30, 41, 59);

    // Canvas & Cards
    public static final Color BG_CANVAS = new Color(248, 250, 252);    // #F8FAFC Slate-50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(226, 232, 240);  // #E2E8F0 Slate-200
    public static final Color CARD_BORDER_HOVER = new Color(203, 213, 225); // #CBD5E1

    // Typography Colors
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);    // #0F172A Slate-900
    public static final Color TEXT_SECONDARY = new Color(71, 85, 105); // #475569 Slate-600
    public static final Color TEXT_MUTED = new Color(148, 163, 184);   // #94A3B8 Slate-400

    // Semantic Status Colors
    public static final Color SUCCESS = new Color(16, 185, 129);       // #10B981 Emerald-500
    public static final Color ERROR = new Color(185, 28, 28);          // #B91C1C Red-700
    public static final Color SUCCESS_LIGHT = new Color(236, 253, 245);
    public static final Color WARNING = new Color(245, 158, 11);       // #F59E0B Amber-500
    public static final Color WARNING_LIGHT = new Color(254, 243, 199);
    public static final Color INFO = new Color(6, 182, 212);           // #06B6D4 Cyan-500
    public static final Color INFO_LIGHT = new Color(236, 254, 255);
    public static final Color PURPLE = new Color(139, 92, 246);        // #8B5CF6 Violet-500
    public static final Color PURPLE_LIGHT = new Color(245, 243, 255);

    // ==========================================
    // Typography
    // ==========================================
    private static final String FONT_FAMILY = "Segoe UI";

    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_SUBHEADER = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_CAPTION = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_STAT_VALUE = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font FONT_BRAND = new Font(FONT_FAMILY, Font.BOLD, 17);

    // ==========================================
    // Reusable Borders
    // ==========================================
    public static final Border BORDER_CARD = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(CARD_BORDER, 1),
        BorderFactory.createEmptyBorder(18, 18, 18, 18)
    );

    public static final Border BORDER_PADDING_16 = BorderFactory.createEmptyBorder(16, 16, 16, 16);
    public static final Border BORDER_PADDING_20 = BorderFactory.createEmptyBorder(20, 20, 20, 20);

    /**
     * Initializes FlatLaf Look and Feel with custom UI defaults.
     */
    public static void setupTheme() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("TableHeader.font", FONT_BODY_BOLD);
            UIManager.put("Table.rowHeight", 36);
            UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Table.gridColor", CARD_BORDER);
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf look and feel: " + e.getMessage());
        }
    }
}
