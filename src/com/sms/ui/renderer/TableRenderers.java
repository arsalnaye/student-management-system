package com.sms.ui.renderer;

import com.sms.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Component;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Cell renderers used by the student grid.
 *
 * <p>Renderers are stateless flyweights - one instance paints every cell in a
 * column - so they are created once and reused, exactly as Swing intends.</p>
 */
public final class TableRenderers {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private TableRenderers() {
    }

    /** Plain text with zebra striping and the given horizontal alignment. */
    public static DefaultTableCellRenderer text(final int alignment) {
        return new StripedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void decorate(JTable table, Object value, int row, int column) {
                setHorizontalAlignment(alignment);
            }
        };
    }

    /** Bold, dark text - used for the student's name. */
    public static DefaultTableCellRenderer emphasis() {
        return new StripedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void decorate(JTable table, Object value, int row, int column) {
                setHorizontalAlignment(JLabel.LEFT);
                setFont(Theme.FONT_BODY_BOLD);
            }
        };
    }

    /** Monospaced, centred - keeps roll numbers visually aligned. */
    public static DefaultTableCellRenderer code() {
        return new StripedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void decorate(JTable table, Object value, int row, int column) {
                setHorizontalAlignment(JLabel.CENTER);
                setFont(Theme.FONT_MONO);
            }
        };
    }

    /** Department short code with the full name as a tooltip. */
    public static DefaultTableCellRenderer department() {
        return new StripedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void decorate(JTable table, Object value, int row, int column) {
                setHorizontalAlignment(JLabel.CENTER);
                if (value instanceof com.sms.model.Department) {
                    com.sms.model.Department d = (com.sms.model.Department) value;
                    setText(d.code());
                    setToolTipText(d.label());
                }
            }
        };
    }

    /** CGPA to two decimals, coloured green / amber / red by band. */
    public static DefaultTableCellRenderer cgpa() {
        return new StripedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void decorate(JTable table, Object value, int row, int column) {
                setHorizontalAlignment(JLabel.CENTER);
                setFont(Theme.FONT_BODY_BOLD);
                if (value instanceof Number) {
                    double score = ((Number) value).doubleValue();
                    setText(String.format(Locale.ROOT, "%.2f", Double.valueOf(score)));
                    if (!isSelectedRow()) {
                        if (score >= 7.5) {
                            setForeground(Theme.SUCCESS);
                        } else if (score >= 6.0) {
                            setForeground(Theme.WARNING);
                        } else {
                            setForeground(Theme.DANGER);
                        }
                    }
                }
            }
        };
    }

    /** {@code LocalDate} formatted as {@code 05 Mar 2004}. */
    public static DefaultTableCellRenderer date() {
        return new StripedRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void decorate(JTable table, Object value, int row, int column) {
                setHorizontalAlignment(JLabel.CENTER);
                if (value instanceof LocalDate) {
                    setText(DISPLAY_DATE.format((LocalDate) value));
                } else if (value == null) {
                    setText("-");
                }
            }
        };
    }

    /** Applies the app's flat header styling to a table header. */
    public static void styleHeader(JTableHeader header) {
        header.setDefaultRenderer(new HeaderRenderer(header.getDefaultRenderer()));
        header.setPreferredSize(new java.awt.Dimension(header.getPreferredSize().width, 36));
        header.setReorderingAllowed(false);
        header.setBackground(Theme.SURFACE_ALT);
        header.setForeground(Theme.TEXT_MUTED);
    }

    /**
     * Base renderer: zebra background, selection colours and padding.
     * Subclasses only override {@link #decorate}.
     */
    private abstract static class StripedRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        private boolean selectedRow;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            this.selectedRow = isSelected;

            setFont(Theme.FONT_BODY);
            setForeground(Theme.TEXT);
            setBackground(isSelected ? Theme.ROW_SELECTED : (row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT));
            setBorder(BorderFactory.createEmptyBorder(0, Theme.GAP_S, 0, Theme.GAP_S));
            setToolTipText(value == null ? null : String.valueOf(value));

            decorate(table, value, row, column);
            return this;
        }

        /** @return {@code true} while painting a selected row. */
        protected boolean isSelectedRow() {
            return selectedRow;
        }

        /** Hook for per-column styling; called after the shared defaults. */
        protected abstract void decorate(JTable table, Object value, int row, int column);
    }

    /** Wraps the look-and-feel's header renderer and restyles the label. */
    private static class HeaderRenderer implements javax.swing.table.TableCellRenderer {

        private final javax.swing.table.TableCellRenderer delegate;

        HeaderRenderer(javax.swing.table.TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setFont(Theme.FONT_LABEL.deriveFont(Font.BOLD, 11f));
                label.setForeground(Theme.TEXT_MUTED);
                label.setBackground(Theme.SURFACE_ALT);
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, Theme.GAP_S, 0, Theme.GAP_S)));
                label.setOpaque(true);
            }
            return c;
        }
    }
}
