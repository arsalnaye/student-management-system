package com.sms.ui.theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.HashSet;
import java.util.Set;

/**
 * The single source of truth for colour, type and spacing.
 *
 * <p>Every panel pulls its values from here, which is what makes the app look
 * like one product instead of a pile of default Swing widgets. Change a constant
 * here and the whole UI follows.</p>
 */
public final class Theme {

    // -- Palette ---------------------------------------------------------

    /** App background - a very light blue-grey so white cards float above it. */
    public static final Color BACKGROUND = new Color(0xF1, 0xF4, 0xF9);
    /** Card / input background. */
    public static final Color SURFACE = Color.WHITE;
    /** Subtle fill for table headers and hovered rows. */
    public static final Color SURFACE_ALT = new Color(0xF7, 0xF9, 0xFC);
    /** Hairline borders. */
    public static final Color BORDER = new Color(0xDD, 0xE3, 0xED);
    public static final Color BORDER_STRONG = new Color(0xC3, 0xCC, 0xDA);

    public static final Color PRIMARY = new Color(0x2F, 0x5B, 0xEA);
    public static final Color PRIMARY_DARK = new Color(0x22, 0x45, 0xB8);
    public static final Color PRIMARY_SOFT = new Color(0xE6, 0xEC, 0xFD);

    public static final Color SUCCESS = new Color(0x16, 0x9B, 0x62);
    public static final Color SUCCESS_DARK = new Color(0x10, 0x7A, 0x4C);
    public static final Color DANGER = new Color(0xD9, 0x3A, 0x3A);
    public static final Color DANGER_DARK = new Color(0xB0, 0x2A, 0x2A);
    public static final Color WARNING = new Color(0xE0, 0x8A, 0x1E);

    public static final Color TEXT = new Color(0x18, 0x21, 0x33);
    public static final Color TEXT_MUTED = new Color(0x64, 0x70, 0x85);
    public static final Color TEXT_ON_ACCENT = Color.WHITE;

    /** Header gradient endpoints. */
    public static final Color HEADER_FROM = new Color(0x1E, 0x3A, 0x8A);
    public static final Color HEADER_TO = new Color(0x3B, 0x6F, 0xE0);

    /** Alternate table row. */
    public static final Color ROW_ALT = new Color(0xF8, 0xFA, 0xFD);
    public static final Color ROW_SELECTED = new Color(0xDC, 0xE6, 0xFF);

    // -- Type ------------------------------------------------------------

    private static final String FAMILY = pickFamily("Segoe UI", "Inter", "Roboto", "SansSerif");

    public static final Font FONT_DISPLAY = new Font(FAMILY, Font.BOLD, 20);
    public static final Font FONT_TITLE = new Font(FAMILY, Font.BOLD, 15);
    public static final Font FONT_SUBTITLE = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font(FAMILY, Font.BOLD, 11);
    public static final Font FONT_BODY = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FAMILY, Font.PLAIN, 11);
    public static final Font FONT_METRIC = new Font(FAMILY, Font.BOLD, 22);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    // -- Spacing ---------------------------------------------------------

    public static final int GAP_XS = 4;
    public static final int GAP_S = 8;
    public static final int GAP_M = 12;
    public static final int GAP_L = 18;
    public static final int RADIUS = 12;

    private Theme() {
    }

    /**
     * Installs the platform look and feel plus a handful of global defaults.
     * Call once, on the Event Dispatch Thread, before building any window.
     */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Any L&F will do; the app styles its own components anyway.
        }
        enableTextAntialiasing();

        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("OptionPane.buttonFont", FONT_BODY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("TextArea.font", FONT_BODY);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("Spinner.font", FONT_BODY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("TableHeader.font", FONT_LABEL);
        UIManager.put("MenuBar.font", FONT_BODY);
        UIManager.put("Menu.font", FONT_BODY);
        UIManager.put("MenuItem.font", FONT_BODY);
        UIManager.put("RadioButton.font", FONT_BODY);
        UIManager.put("ToolTip.font", FONT_SMALL);
        UIManager.put("Table.selectionBackground", ROW_SELECTED);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("ScrollBar.width", Integer.valueOf(12));
    }

    /** Padding-only border, in pixels. */
    public static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /** Hairline box with inner padding - the default look for inputs. */
    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1, true),
                padding(6, 9, 6, 9));
    }

    /** The same box, tinted red, used to flag an invalid field. */
    public static Border errorBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DANGER, 1, true),
                padding(6, 9, 6, 9));
    }

    /** The same box, tinted blue, used while a field has focus. */
    public static Border focusBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1, true),
                padding(6, 9, 6, 9));
    }

    /** Applies the shared input styling to a text field, combo box or spinner. */
    public static void styleInput(JComponent input) {
        input.setFont(FONT_BODY);
        input.setForeground(TEXT);
        input.setBackground(SURFACE);
        input.setBorder(inputBorder());
    }

    private static void enableTextAntialiasing() {
        // Honour the desktop's font smoothing setting when the toolkit exposes it.
        Object hint = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (hint != null) {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        }
    }

    /** @return the first installed family from {@code candidates}, else {@code "SansSerif"}. */
    private static String pickFamily(String... candidates) {
        Set<String> installed = new HashSet<String>();
        try {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String name : names) {
                installed.add(name);
            }
        } catch (RuntimeException headless) {
            return "SansSerif";
        }
        for (String candidate : candidates) {
            if (installed.contains(candidate)) {
                return candidate;
            }
        }
        return "SansSerif";
    }
}
