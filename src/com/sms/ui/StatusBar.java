package com.sms.ui;

import com.sms.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The strip along the bottom of the window: a status dot, the last message, the
 * data file in use and a live clock.
 *
 * <p>The clock is driven by a {@link javax.swing.Timer}, which fires on the
 * Event Dispatch Thread - the only safe way to touch Swing state on a schedule.</p>
 */
public class StatusBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss");

    /** Severity of a status message, mapped to the colour of the leading dot. */
    public enum Level {
        INFO(Theme.TEXT_MUTED),
        SUCCESS(Theme.SUCCESS),
        WARNING(Theme.WARNING),
        ERROR(Theme.DANGER);

        private final Color color;

        Level(Color color) {
            this.color = color;
        }
    }

    private final Dot dot = new Dot();
    private final JLabel messageLabel = new JLabel("Ready");
    private final JLabel contextLabel = new JLabel();
    private final JLabel clockLabel = new JLabel();

    public StatusBar() {
        super(new BorderLayout(Theme.GAP_M, 0));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                Theme.padding(6, Theme.GAP_M, 6, Theme.GAP_M)));

        messageLabel.setFont(Theme.FONT_SMALL);
        messageLabel.setForeground(Theme.TEXT);

        contextLabel.setFont(Theme.FONT_SMALL);
        contextLabel.setForeground(Theme.TEXT_MUTED);

        clockLabel.setFont(Theme.FONT_SMALL);
        clockLabel.setForeground(Theme.TEXT_MUTED);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.GAP_S, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(messageLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP_M, 0));
        right.setOpaque(false);
        right.add(contextLabel);
        right.add(clockLabel);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        startClock();
    }

    /** Shows {@code message} with the colour that matches {@code level}. */
    public void setMessage(String message, Level level) {
        messageLabel.setText(message);
        dot.setColor(level.color);
        dot.repaint();
    }

    /** Sets the small grey text on the right (the data file being used). */
    public void setContext(String context) {
        contextLabel.setText(context);
        contextLabel.setToolTipText(context);
    }

    private void startClock() {
        ActionListener tick = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clockLabel.setText(CLOCK.format(LocalDateTime.now()));
            }
        };
        tick.actionPerformed(null);
        Timer timer = new Timer(1000, tick);
        timer.setInitialDelay(0);
        timer.start();
    }

    /** A small filled circle used as the status indicator. */
    private static class Dot extends JPanel {

        private static final long serialVersionUID = 1L;

        private Color color = Theme.TEXT_MUTED;

        Dot() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 10));
        }

        void setColor(Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 1, 8, 8);
            } finally {
                g2.dispose();
            }
        }
    }
}
