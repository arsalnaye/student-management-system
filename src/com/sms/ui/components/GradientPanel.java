package com.sms.ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** A panel filled with a left-to-right linear gradient; used for the app banner. */
public class GradientPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Color from;
    private final Color to;

    public GradientPanel(LayoutManager layout, Color from, Color to) {
        super(layout);
        this.from = from;
        this.to = to;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, from, getWidth(), getHeight(), to));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
