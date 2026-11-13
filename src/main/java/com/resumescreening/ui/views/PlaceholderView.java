package com.resumescreening.ui.views;

import com.resumescreening.ui.components.ContentCard;
import com.resumescreening.ui.components.UIIcon;
import com.resumescreening.ui.theme.UITheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Clean placeholder view for navigation modules configured in future development phases.
 */
public class PlaceholderView extends JPanel {

    public PlaceholderView(String moduleName, String description, UIIcon.Kind iconKind, String nextPhaseDetails) {
        setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        ContentCard card = new ContentCard(moduleName, "Module Overview");

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        // Icon
        JLabel icon = new JLabel(new UIIcon(iconKind, 48, UITheme.PRIMARY));
        icon.setPreferredSize(new Dimension(48, 48));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        // Title
        JLabel title = new JLabel(moduleName);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setAlignmentX(CENTER_ALIGNMENT);

        // Description
        JLabel desc = new JLabel("<html><center>" + description + "</center></html>");
        desc.setFont(UITheme.FONT_BODY);
        desc.setForeground(UITheme.TEXT_SECONDARY);
        desc.setAlignmentX(CENTER_ALIGNMENT);
        desc.setMaximumSize(new Dimension(500, 60));

        // Note
        JLabel note = new JLabel("<html><center><b>Planned Implementation:</b> " + nextPhaseDetails + "</center></html>");
        note.setFont(UITheme.FONT_CAPTION);
        note.setForeground(UITheme.PRIMARY);
        note.setAlignmentX(CENTER_ALIGNMENT);

        body.add(icon);
        body.add(Box.createVerticalStrut(16));
        body.add(title);
        body.add(Box.createVerticalStrut(8));
        body.add(desc);
        body.add(Box.createVerticalStrut(14));
        body.add(note);

        card.setBodyComponent(body);
        add(card, BorderLayout.CENTER);
    }
}
