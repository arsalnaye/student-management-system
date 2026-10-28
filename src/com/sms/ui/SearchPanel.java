package com.sms.ui;

import com.sms.model.Department;
import com.sms.model.SearchQuery;
import com.sms.ui.components.ActionButton;
import com.sms.ui.components.CardPanel;
import com.sms.ui.components.HintTextField;
import com.sms.ui.theme.Theme;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The filter bar above the grid.
 *
 * <p>Every control feeds the same {@link #fireQueryChanged()} method, so search
 * is live: results narrow as the user types, with no Search button to press.
 * The panel owns no data - it only publishes a {@link SearchQuery}.</p>
 */
public class SearchPanel extends CardPanel {

    private static final long serialVersionUID = 1L;

    private static final int CGPA_SCALE = 10; // slider units per CGPA point

    private final HintTextField keywordField = new HintTextField("Search name, roll no, email or phone...");
    private final JComboBox<Department> departmentCombo = new JComboBox<Department>();
    private final JComboBox<Integer> semesterCombo = new JComboBox<Integer>();
    private final JSlider cgpaSlider = new JSlider(0, 10 * CGPA_SCALE, 0);
    private final JLabel cgpaValueLabel = new JLabel("Any");
    private final ActionButton resetButton =
            ActionButton.of("Reset", ActionButton.Style.GHOST, "Clear every filter (Esc)");

    private Consumer<SearchQuery> queryListener;
    private boolean suppressEvents;

    public SearchPanel() {
        super(new GridBagLayout());
        buildControls();
        layoutControls();
        wireEvents();
    }

    // ------------------------------------------------------------- building

    private void buildControls() {
        keywordField.setColumns(24);

        DefaultComboBoxModel<Department> departments = new DefaultComboBoxModel<Department>();
        departments.addElement(null); // null == "All departments"
        for (Department d : Department.values()) {
            departments.addElement(d);
        }
        departmentCombo.setModel(departments);
        departmentCombo.setRenderer(departmentRenderer("All departments"));
        Theme.styleInput(departmentCombo);

        DefaultComboBoxModel<Integer> semesters = new DefaultComboBoxModel<Integer>();
        semesters.addElement(Integer.valueOf(SearchQuery.ANY_SEMESTER));
        for (int i = 1; i <= 8; i++) {
            semesters.addElement(Integer.valueOf(i));
        }
        semesterCombo.setModel(semesters);
        semesterCombo.setRenderer(semesterRenderer());
        Theme.styleInput(semesterCombo);
        semesterCombo.setPreferredSize(new Dimension(110, 32));

        cgpaSlider.setOpaque(false);
        cgpaSlider.setMajorTickSpacing(2 * CGPA_SCALE);
        cgpaSlider.setPaintTicks(true);
        cgpaSlider.setToolTipText("Show only students at or above this CGPA");
        // A slider's own minimum size is tiny, so GridBagLayout squashes it to a
        // stub as soon as the bar is tight. Pin the minimum to the preferred.
        Dimension sliderSize = new Dimension(150, 34);
        cgpaSlider.setPreferredSize(sliderSize);
        cgpaSlider.setMinimumSize(sliderSize);

        cgpaValueLabel.setFont(Theme.FONT_BODY_BOLD);
        cgpaValueLabel.setForeground(Theme.PRIMARY);
        cgpaValueLabel.setPreferredSize(new Dimension(42, 20));
    }

    private void layoutControls() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, Theme.GAP_S);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridy = 0;

        gc.gridx = 0;
        add(caption("Find"), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_L);
        add(keywordField, gc);

        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_S);

        gc.gridx = 2;
        add(caption("Department"), gc);
        gc.gridx = 3;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_L);
        add(departmentCombo, gc);

        gc.gridx = 4;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_S);
        add(caption("Semester"), gc);
        gc.gridx = 5;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_L);
        add(semesterCombo, gc);

        gc.gridx = 6;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_XS);
        add(caption("Min CGPA"), gc);
        gc.gridx = 7;
        add(cgpaSlider, gc);
        gc.gridx = 8;
        gc.insets = new Insets(0, 0, 0, Theme.GAP_L);
        add(cgpaValueLabel, gc);

        gc.gridx = 9;
        gc.insets = new Insets(0, 0, 0, 0);
        add(resetButton, gc);
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(Theme.FONT_LABEL);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    // --------------------------------------------------------------- events

    private void wireEvents() {
        keywordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireQueryChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireQueryChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fireQueryChanged();
            }
        });

        ActionListener onChoice = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireQueryChanged();
            }
        };
        departmentCombo.addActionListener(onChoice);
        semesterCombo.addActionListener(onChoice);

        cgpaSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                cgpaValueLabel.setText(cgpaSlider.getValue() == 0
                        ? "Any"
                        : String.format(Locale.ROOT, "%.1f", Double.valueOf(minCgpa())));
                if (!cgpaSlider.getValueIsAdjusting()) {
                    fireQueryChanged();
                }
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
    }

    /** Registers the (single) listener that receives every query change. */
    public void setQueryListener(Consumer<SearchQuery> listener) {
        this.queryListener = listener;
    }

    /** @return the filters currently shown in the bar. */
    public SearchQuery getQuery() {
        Integer semester = (Integer) semesterCombo.getSelectedItem();
        return new SearchQuery(
                keywordField.getText(),
                (Department) departmentCombo.getSelectedItem(),
                semester == null ? SearchQuery.ANY_SEMESTER : semester.intValue(),
                minCgpa());
    }

    /** Clears every filter and publishes the resulting (empty) query once. */
    public void reset() {
        suppressEvents = true;
        try {
            keywordField.setText("");
            departmentCombo.setSelectedIndex(0);
            semesterCombo.setSelectedIndex(0);
            cgpaSlider.setValue(0);
        } finally {
            suppressEvents = false;
        }
        fireQueryChanged();
    }

    /** Moves keyboard focus to the search box and selects its content. */
    public void focusSearchField() {
        keywordField.requestFocusInWindow();
        keywordField.selectAll();
    }

    private double minCgpa() {
        return cgpaSlider.getValue() / (double) CGPA_SCALE;
    }

    private void fireQueryChanged() {
        if (!suppressEvents && queryListener != null) {
            queryListener.accept(getQuery());
        }
    }

    // ------------------------------------------------------------ renderers

    /**
     * Renders a {@code null} entry as {@code placeholder} instead of a blank row.
     * Extending {@link DefaultListCellRenderer} keeps the look and feel's own
     * selection colours and focus handling.
     */
    private static ListCellRenderer<Object> departmentRenderer(final String placeholder) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? placeholder : ((Department) value).label());
                setFont(Theme.FONT_BODY);
                return this;
            }
        };
    }

    /** Renders semester {@code 0} as {@code "All semesters"}. */
    private static ListCellRenderer<Object> semesterRenderer() {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                boolean any = value == null || ((Integer) value).intValue() == SearchQuery.ANY_SEMESTER;
                setText(any ? "All semesters" : "Semester " + value);
                setFont(Theme.FONT_BODY);
                return this;
            }
        };
    }

    /** Keeps the bar from stretching vertically inside a BorderLayout.NORTH slot. */
    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}
