package com.sms.ui;

import com.sms.model.Student;
import com.sms.ui.components.CardPanel;
import com.sms.ui.renderer.TableRenderers;
import com.sms.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * The records grid: a sortable {@link JTable} wrapped in a card, plus an empty
 * state for when a search returns nothing.
 *
 * <p>This is a passive view. It renders what it is given and reports user
 * gestures through listeners; every decision is taken by {@link MainFrame}.</p>
 */
public class StudentTablePanel extends CardPanel {

    private static final long serialVersionUID = 1L;

    private static final String CARD_GRID = "grid";
    private static final String CARD_EMPTY = "empty";

    private final StudentTableModel model = new StudentTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<StudentTableModel> sorter = new TableRowSorter<StudentTableModel>(model);
    private final JLabel countLabel = new JLabel();
    private final JLabel emptyLabel = new JLabel();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);

    private final List<Consumer<Student>> selectionListeners = new ArrayList<Consumer<Student>>();
    private Runnable doubleClickAction;
    private Runnable deleteKeyAction;
    private boolean suppressSelectionEvents;

    public StudentTablePanel() {
        super(new BorderLayout(0, Theme.GAP_S));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        configureTable();
        wireEvents();
    }

    // ------------------------------------------------------------- building

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Student Records");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);

        countLabel.setFont(Theme.FONT_SMALL);
        countLabel.setForeground(Theme.TEXT_MUTED);
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(title, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);
        header.setBorder(Theme.padding(0, 2, Theme.GAP_XS, 2));
        return header;
    }

    private JComponent buildBody() {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setPreferredSize(new Dimension(720, 380));

        body.setOpaque(false);
        body.add(scroll, CARD_GRID);
        body.add(buildEmptyState(), CARD_EMPTY);
        return body;
    }

    private JComponent buildEmptyState() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JLabel icon = new JLabel("[  ]");
        icon.setFont(Theme.FONT_DISPLAY.deriveFont(34f));
        icon.setForeground(Theme.BORDER_STRONG);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyLabel.setText("No students yet");
        emptyLabel.setFont(Theme.FONT_TITLE);
        emptyLabel.setForeground(Theme.TEXT_MUTED);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Fill in the form on the right and press Add Student.");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(javax.swing.Box.createVerticalGlue());
        panel.add(icon);
        panel.add(javax.swing.Box.createVerticalStrut(Theme.GAP_S));
        panel.add(emptyLabel);
        panel.add(javax.swing.Box.createVerticalStrut(Theme.GAP_XS));
        panel.add(hint);
        panel.add(javax.swing.Box.createVerticalGlue());
        return panel;
    }

    private void configureTable() {
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setBackground(Theme.SURFACE);
        table.setRowSorter(sorter);
        sorter.setSortKeys(Arrays.asList(
                new RowSorter.SortKey(StudentTableModel.COL_ID, SortOrder.ASCENDING)));
        TableRenderers.styleHeader(table.getTableHeader());

        setColumn(StudentTableModel.COL_ID, 46, TableRenderers.text(SwingConstants.CENTER));
        setColumn(StudentTableModel.COL_ROLL, 110, TableRenderers.code());
        setColumn(StudentTableModel.COL_NAME, 180, TableRenderers.emphasis());
        // Wide enough that the "Department" header never truncates to "Depart...";
        // the width it takes comes back off the e-mail column, which elides
        // gracefully because the full address is in the cell tooltip.
        setColumn(StudentTableModel.COL_DEPARTMENT, 120, TableRenderers.department());
        setColumn(StudentTableModel.COL_SEMESTER, 50, TableRenderers.text(SwingConstants.CENTER));
        setColumn(StudentTableModel.COL_CGPA, 64, TableRenderers.cgpa());
        setColumn(StudentTableModel.COL_EMAIL, 176, TableRenderers.text(SwingConstants.LEFT));
        setColumn(StudentTableModel.COL_PHONE, 110, TableRenderers.text(SwingConstants.CENTER));
        setColumn(StudentTableModel.COL_GENDER, 70, TableRenderers.text(SwingConstants.CENTER));
        setColumn(StudentTableModel.COL_DOB, 110, TableRenderers.date());
    }

    private void setColumn(int index, int width, javax.swing.table.TableCellRenderer renderer) {
        TableColumn column = table.getColumnModel().getColumn(index);
        column.setPreferredWidth(width);
        column.setMinWidth(Math.min(width, 46));
        column.setCellRenderer(renderer);
    }

    // --------------------------------------------------------------- events

    private void wireEvents() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || suppressSelectionEvents) {
                    return;
                }
                Student selected = getSelectedStudent();
                for (Consumer<Student> listener : selectionListeners) {
                    listener.accept(selected);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && doubleClickAction != null && table.rowAtPoint(e.getPoint()) >= 0) {
                    doubleClickAction.run();
                }
            }
        });

        // Delete key on the grid removes the selection - a standard desktop idiom.
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("DELETE"), "sms.deleteSelection");
        table.getActionMap().put("sms.deleteSelection", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (deleteKeyAction != null && table.getSelectedRowCount() > 0) {
                    deleteKeyAction.run();
                }
            }
        });
    }

    /** Registers a listener notified with the selected student, or {@code null}. */
    public void addSelectionListener(Consumer<Student> listener) {
        selectionListeners.add(listener);
    }

    /** Sets what happens when a row is double-clicked (normally "load into form"). */
    public void setDoubleClickAction(Runnable action) {
        this.doubleClickAction = action;
    }

    /** Sets what happens when DELETE is pressed with rows selected. */
    public void setDeleteKeyAction(Runnable action) {
        this.deleteKeyAction = action;
    }

    // ------------------------------------------------------------- data API

    /**
     * Replaces the rows, keeping the previous selection when that student is
     * still visible.
     *
     * @param students rows to show
     * @param emptyMessage text for the empty state when {@code students} is empty
     */
    public void setStudents(List<Student> students, String emptyMessage) {
        int previouslySelectedId = selectedId();
        suppressSelectionEvents = true;
        try {
            model.setStudents(students);
        } finally {
            suppressSelectionEvents = false;
        }
        emptyLabel.setText(emptyMessage);
        cards.show(body, students.isEmpty() ? CARD_EMPTY : CARD_GRID);
        countLabel.setText(students.size() == 1 ? "1 record" : students.size() + " records");
        if (previouslySelectedId != Student.UNSAVED_ID) {
            selectById(previouslySelectedId);
        }
    }

    /** @return the focused student, or {@code null} when nothing is selected. */
    public Student getSelectedStudent() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return model.getStudentAt(table.convertRowIndexToModel(viewRow));
    }

    /** @return every selected student; empty when there is no selection. */
    public List<Student> getSelectedStudents() {
        List<Student> selected = new ArrayList<Student>();
        for (int viewRow : table.getSelectedRows()) {
            Student s = model.getStudentAt(table.convertRowIndexToModel(viewRow));
            if (s != null) {
                selected.add(s);
            }
        }
        return selected;
    }

    /** @return the students currently listed, in model order. */
    public List<Student> getVisibleStudents() {
        return model.getStudents();
    }

    /** Selects and scrolls to the row holding {@code id}; a no-op if absent. */
    public void selectById(int id) {
        int modelRow = model.indexOfId(id);
        if (modelRow < 0) {
            table.clearSelection();
            return;
        }
        int viewRow = table.convertRowIndexToView(modelRow);
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
        }
    }

    public void clearSelection() {
        table.clearSelection();
    }

    /** Moves keyboard focus into the grid. */
    public void focusTable() {
        table.requestFocusInWindow();
    }

    private int selectedId() {
        Student selected = getSelectedStudent();
        return selected == null ? Student.UNSAVED_ID : selected.getId();
    }
}
