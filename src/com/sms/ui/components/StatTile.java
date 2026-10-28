package com.sms.ui.components;

import com.sms.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * One dashboard metric: a caption, a big number and a coloured accent bar.
 *
 * <p>Used in the header strip to show total students, average CGPA and so on.
 * Only {@link #setValue(String)} and {@link #setCaption(String)} change at
 * runtime, so repaints stay cheap.</p>
 */
public class StatTile extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Color accent;
    private final JLabel titleLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final JLabel captionLabel = new JLabel();

    /**
     * @param title   short metric name, e.g. {@code "Total students"}
     * @param accent  colour of the 3px bar down the left edge
     */
    public StatTile(String title, Color accent) {
        this.accent = accent;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(Theme.GAP_S, Theme.GAP_M + 6, Theme.GAP_S, Theme.GAP_M));

        titleLabel.setText(title.toUpperCase());
        titleLabel.setFont(Theme.FONT_LABEL);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setText("-");
        valueLabel.setFont(Theme.FONT_METRIC);
        valueLabel.setForeground(Theme.TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        captionLabel.setText(" ");
        captionLabel.setFont(Theme.FONT_SMALL);
        captionLabel.setForeground(Theme.TEXT_MUTED);
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(titleLabel);
        add(valueLabel);
        add(captionLabel);
    }

    /** Sets the large number. */
    public void setValue(String value) {
        valueLabel.setText(value);
    }

    /** Sets the small grey line under the number. */
    public void setCaption(String caption) {
        captionLabel.setText(caption == null || caption.isEmpty() ? " " : caption);
        setToolTipText(caption);
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
            g2.setColor(accent);
            g2.fillRoundRect(0, 6, 4, h - 13, 4, 4);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = Math.max(size.width, 170);
        size.height = Math.max(size.height, 78);
        return size;
    }
}
