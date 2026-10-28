package com.sms.ui.components;

import com.sms.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * A white, rounded surface with a hairline border - the container every section
 * of the window sits inside.
 *
 * <p>The panel is non-opaque and paints its own rounded background so the app
 * background shows through at the corners.</p>
 */
public class CardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(Theme.padding(Theme.GAP_M, Theme.GAP_M, Theme.GAP_M, Theme.GAP_M));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.SURFACE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
