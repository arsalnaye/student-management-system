package com.sms.ui.components;

import com.sms.ui.theme.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A flat, rounded button with hover and pressed states.
 *
 * <p>Swing's stock button follows the OS theme, which clashes with the rest of
 * this UI, so the background is painted here and the label is left to the
 * standard UI delegate (which keeps mnemonics, icons and accessibility intact).</p>
 */
public class ActionButton extends JButton {

    private static final long serialVersionUID = 1L;

    /** Visual weight of a button, mapped to a colour pair. */
    public enum Style {
        /** Filled blue - the one action the user most likely wants. */
        PRIMARY(Theme.PRIMARY, Theme.PRIMARY_DARK, Theme.TEXT_ON_ACCENT),
        /** Filled green - confirms a change to an existing record. */
        SUCCESS(Theme.SUCCESS, Theme.SUCCESS_DARK, Theme.TEXT_ON_ACCENT),
        /** Filled red - destructive. */
        DANGER(Theme.DANGER, Theme.DANGER_DARK, Theme.TEXT_ON_ACCENT),
        /** Outlined - secondary actions such as Clear or Reset. */
        GHOST(Theme.SURFACE, Theme.SURFACE_ALT, Theme.TEXT);

        private final Color fill;
        private final Color fillActive;
        private final Color text;

        Style(Color fill, Color fillActive, Color text) {
            this.fill = fill;
            this.fillActive = fillActive;
            this.text = text;
        }
    }

    private final Style style;

    public ActionButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(Theme.FONT_BODY_BOLD);
        setForeground(style.text);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(Theme.padding(9, 16, 9, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Convenience factory so call sites read as one line. */
    public static ActionButton of(String text, Style style, String tooltip) {
        ActionButton button = new ActionButton(text, style);
        button.setToolTipText(tooltip);
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean active = getModel().isPressed() || getModel().isRollover();

            if (!isEnabled()) {
                g2.setColor(Theme.BORDER);
            } else {
                g2.setColor(active ? style.fillActive : style.fill);
            }
            g2.fillRoundRect(0, 0, w, h, Theme.RADIUS, Theme.RADIUS);

            if (style == Style.GHOST) {
                g2.setColor(isEnabled() ? Theme.BORDER_STRONG : Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
            }
            setForeground(isEnabled() ? style.text : Theme.TEXT_MUTED);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = Math.max(size.height, 36);
        return size;
    }
}
