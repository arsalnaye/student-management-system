package com.sms.ui.components;

import com.sms.ui.theme.Theme;

import javax.swing.JTextField;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A text field that paints a grey placeholder while it is empty and unfocused.
 *
 * <p>Placeholders are drawn rather than inserted as text, so {@link #getText()}
 * never returns the hint - a classic source of bugs in hand-rolled versions.</p>
 */
public class HintTextField extends JTextField {

    private static final long serialVersionUID = 1L;

    private final String hint;

    public HintTextField(String hint) {
        this.hint = hint == null ? "" : hint;
        Theme.styleInput(this);
        setColumns(14);

        // Gaining focus hides the hint, losing it shows the hint again - but
        // Swing only repaints the one-pixel caret region on a focus change, so
        // without this the old hint stays painted underneath the caret.
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (hint.isEmpty() || !getText().isEmpty() || isFocusOwner()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(getFont());
            // Match where the real text lands: the border insets plus the
            // editor margin. Without the margin the hint sits 2px to the left
            // and the text visibly jumps when the user starts typing.
            Insets insets = getInsets();
            Insets margin = getMargin();
            int x = insets.left + (margin == null ? 0 : margin.left);
            int baseline = insets.top + g2.getFontMetrics().getAscent();
            g2.drawString(hint, x, baseline);
        } finally {
            g2.dispose();
        }
    }
}
