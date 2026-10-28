package com.sms.ui;

import com.sms.dao.DataAccessException;
import com.sms.model.Department;
import com.sms.model.SearchQuery;
import com.sms.model.Student;
import com.sms.service.StudentService;
import com.sms.service.ValidationException;
import com.sms.ui.components.GradientPanel;
import com.sms.ui.components.StatTile;
import com.sms.ui.theme.Theme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The application window, and the controller that binds the three panels to the
 * {@link StudentService}.
 *
 * <p>Layout, top to bottom: a gradient banner, a dashboard strip of metric
 * tiles, the search bar, a split pane holding the grid and the form, and the
 * status bar. Panels never talk to each other - each one reports to this class,
 * which is the single place where "what happens when" is decided.</p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String APP_TITLE = "Student Management System";

    private final StudentService service;

    private final SearchPanel searchPanel = new SearchPanel();
    private final StudentTablePanel tablePanel = new StudentTablePanel();
    private final StudentFormPanel formPanel = new StudentFormPanel();
    private final StatusBar statusBar = new StatusBar();

    private final StatTile totalTile = new StatTile("Total students", Theme.PRIMARY);
    private final StatTile averageTile = new StatTile("Average CGPA", Theme.SUCCESS);
    private final StatTile departmentTile = new StatTile("Departments", Theme.WARNING);
    private final StatTile topperTile = new StatTile("Top performer", Theme.HEADER_TO);

    private SearchQuery currentQuery = SearchQuery.all();

    /**
     * @param service          business layer to drive
     * @param storeDescription where records are stored, shown in the status bar
     *                         (for example {@code sqlite : jdbc:sqlite:...})
     */
    public MainFrame(StudentService service, String storeDescription) {
        super(APP_TITLE);
        this.service = service;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        wireEvents();
        installShortcuts();

        statusBar.setContext("Database: " + storeDescription);
        setMinimumSize(new Dimension(1024, 640));
        setSize(1240, 780);
        setLocationRelativeTo(null);

        refresh("Loaded " + service.count() + " records.", StatusBar.Level.INFO);
    }

    // ------------------------------------------------------------- building

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(buildBanner(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        return root;
    }

    private JComponent buildBanner() {
        GradientPanel banner = new GradientPanel(new BorderLayout(), Theme.HEADER_FROM, Theme.HEADER_TO);
        banner.setBorder(Theme.padding(Theme.GAP_L, Theme.GAP_L + 2, Theme.GAP_L, Theme.GAP_L + 2));

        JLabel title = new JLabel(APP_TITLE);
        title.setFont(Theme.FONT_DISPLAY);
        title.setForeground(Theme.TEXT_ON_ACCENT);

        JLabel subtitle = new JLabel("Add, update, delete and search student records");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(new java.awt.Color(0xD6, 0xE2, 0xFF));

        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);

        JLabel hint = new JLabel("Press F1 for keyboard shortcuts");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(new java.awt.Color(0xC2, 0xD4, 0xFF));

        banner.add(text, BorderLayout.WEST);
        banner.add(hint, BorderLayout.EAST);
        return banner;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(Theme.GAP_M, Theme.GAP_M));
        body.setOpaque(false);
        body.setBorder(Theme.padding(Theme.GAP_M, Theme.GAP_M, Theme.GAP_M, Theme.GAP_M));

        JPanel north = new JPanel(new BorderLayout(0, Theme.GAP_M));
        north.setOpaque(false);
        north.add(buildDashboard(), BorderLayout.NORTH);
        north.add(searchPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, formPanel);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(Theme.GAP_M);
        split.setContinuousLayout(true);
        split.setResizeWeight(1.0);

        body.add(north, BorderLayout.NORTH);
        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private JComponent buildDashboard() {
        JPanel dashboard = new JPanel(new GridLayout(1, 4, Theme.GAP_M, 0));
        dashboard.setOpaque(false);
        dashboard.add(totalTile);
        dashboard.add(averageTile);
        dashboard.add(departmentTile);
        dashboard.add(topperTile);
        return dashboard;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        JMenu file = menu("File", KeyEvent.VK_F);
        file.add(item("New Student", KeyEvent.VK_N, "control N", new Runnable() {
            @Override
            public void run() {
                doClear();
            }
        }));
        file.add(item("Export current view...", KeyEvent.VK_E, "control E", new Runnable() {
            @Override
            public void run() {
                doExport();
            }
        }));
        file.addSeparator();
        file.add(item("Exit", KeyEvent.VK_X, "control Q", new Runnable() {
            @Override
            public void run() {
                confirmAndExit();
            }
        }));

        JMenu edit = menu("Edit", KeyEvent.VK_E);
        edit.add(item("Save record", KeyEvent.VK_S, "control S", new Runnable() {
            @Override
            public void run() {
                doSave();
            }
        }));
        edit.add(item("Delete selected", KeyEvent.VK_D, "control D", new Runnable() {
            @Override
            public void run() {
                doDelete();
            }
        }));
        edit.addSeparator();
        edit.add(item("Clear form", KeyEvent.VK_C, "control L", new Runnable() {
            @Override
            public void run() {
                doClear();
            }
        }));

        JMenu view = menu("View", KeyEvent.VK_V);
        view.add(item("Refresh", KeyEvent.VK_R, "F5", new Runnable() {
            @Override
            public void run() {
                refresh("View refreshed.", StatusBar.Level.INFO);
            }
        }));
        view.add(item("Focus search", KeyEvent.VK_F, "control F", new Runnable() {
            @Override
            public void run() {
                searchPanel.focusSearchField();
            }
        }));
        view.add(item("Reset filters", KeyEvent.VK_T, "control R", new Runnable() {
            @Override
            public void run() {
                searchPanel.reset();
            }
        }));

        JMenu help = menu("Help", KeyEvent.VK_H);
        help.add(item("Keyboard shortcuts", KeyEvent.VK_K, "F1", new Runnable() {
            @Override
            public void run() {
                Dialogs.shortcuts(MainFrame.this);
            }
        }));
        help.add(item("About", KeyEvent.VK_A, null, new Runnable() {
            @Override
            public void run() {
                Dialogs.about(MainFrame.this);
            }
        }));

        bar.add(file);
        bar.add(edit);
        bar.add(view);
        bar.add(help);
        return bar;
    }

    private static JMenu menu(String text, int mnemonic) {
        JMenu menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        menu.setFont(Theme.FONT_BODY);
        return menu;
    }

    private static JMenuItem item(String text, int mnemonic, String accelerator, final Runnable action) {
        JMenuItem menuItem = new JMenuItem(text, mnemonic);
        if (accelerator != null) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        menuItem.setFont(Theme.FONT_BODY);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return menuItem;
    }

    // --------------------------------------------------------------- events

    private void wireEvents() {
        searchPanel.setQueryListener(new Consumer<SearchQuery>() {
            @Override
            public void accept(SearchQuery query) {
                currentQuery = query;
                refresh(null, StatusBar.Level.INFO);
            }
        });

        tablePanel.addSelectionListener(new Consumer<Student>() {
            @Override
            public void accept(Student student) {
                if (student == null) {
                    formPanel.clear();
                } else {
                    formPanel.setStudent(student);
                    statusBar.setMessage("Editing " + student.getName() + " (" + student.getRollNo() + ").",
                            StatusBar.Level.INFO);
                }
            }
        });

        tablePanel.setDoubleClickAction(new Runnable() {
            @Override
            public void run() {
                Student selected = tablePanel.getSelectedStudent();
                if (selected != null) {
                    formPanel.setStudent(selected);
                    formPanel.focusFirstField();
                }
            }
        });

        tablePanel.setDeleteKeyAction(new Runnable() {
            @Override
            public void run() {
                doDelete();
            }
        });

        formPanel.setCommandListener(new StudentFormPanel.CommandListener() {
            @Override
            public void onCommand(StudentFormPanel.Command command) {
                switch (command) {
                    case ADD:
                        doAdd();
                        break;
                    case UPDATE:
                        doUpdate();
                        break;
                    case DELETE:
                        doDelete();
                        break;
                    case CLEAR:
                    default:
                        doClear();
                        break;
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }

            /**
             * Start in the form's first field rather than letting focus land on
             * the search box, so the search placeholder stays readable and the
             * user can type a new record straight away.
             */
            @Override
            public void windowOpened(WindowEvent e) {
                formPanel.focusFirstField();
            }
        });
    }

    /** Window-wide keys that are not tied to a menu item. */
    private void installShortcuts() {
        InputMap keys = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getRootPane().getActionMap();

        keys.put(KeyStroke.getKeyStroke("ESCAPE"), "sms.escape");
        actions.put("sms.escape", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                doClear();
                searchPanel.reset();
                statusBar.setMessage("Form cleared and filters reset.", StatusBar.Level.INFO);
            }
        });
    }

    // -------------------------------------------------------------- actions

    /** Validates the form and inserts a new record. */
    private void doAdd() {
        try {
            Student created = service.add(formPanel.getFormData());
            // Rebuild the grid first, then highlight the new row. Selecting it
            // loads it back into the form, so the user can see exactly what was
            // saved; Ctrl+N clears the form for the next entry.
            refresh(null, StatusBar.Level.INFO);
            tablePanel.selectById(created.getId());
            statusBar.setMessage("Added " + created.getName() + " (" + created.getRollNo()
                    + "). Press Ctrl+N to add another.", StatusBar.Level.SUCCESS);
        } catch (ValidationException e) {
            reportInvalid(e);
        } catch (DataAccessException e) {
            reportStorageFailure(e);
        }
    }

    /** Validates the form and overwrites the record being edited. */
    private void doUpdate() {
        if (formPanel.getEditingId() == Student.UNSAVED_ID) {
            statusBar.setMessage("Select a student in the table first.", StatusBar.Level.WARNING);
            return;
        }
        try {
            Student saved = service.update(formPanel.getFormData());
            refresh(null, StatusBar.Level.INFO);
            tablePanel.selectById(saved.getId());
            statusBar.setMessage("Updated " + saved.getName() + " (" + saved.getRollNo() + ").",
                    StatusBar.Level.SUCCESS);
        } catch (ValidationException e) {
            reportInvalid(e);
        } catch (DataAccessException e) {
            reportStorageFailure(e);
        }
    }

    /** Ctrl+S: add or update depending on which mode the form is in. */
    private void doSave() {
        if (formPanel.getEditingId() == Student.UNSAVED_ID) {
            doAdd();
        } else {
            doUpdate();
        }
    }

    /**
     * Deletes the grid selection, falling back to the record loaded in the form
     * when the grid has no selection.
     */
    private void doDelete() {
        List<Student> targets = tablePanel.getSelectedStudents();
        if (targets.isEmpty() && formPanel.getEditingId() != Student.UNSAVED_ID) {
            Student editing = service.findById(formPanel.getEditingId()).orElse(null);
            if (editing != null) {
                targets = java.util.Collections.singletonList(editing);
            }
        }
        if (targets.isEmpty()) {
            statusBar.setMessage("Nothing selected to delete.", StatusBar.Level.WARNING);
            return;
        }
        if (!Dialogs.confirmDelete(this, targets)) {
            statusBar.setMessage("Delete cancelled.", StatusBar.Level.INFO);
            return;
        }
        try {
            int removed = 0;
            for (Student s : targets) {
                if (service.delete(s.getId())) {
                    removed++;
                }
            }
            formPanel.clear();
            tablePanel.clearSelection();
            refresh(removed == 1
                            ? "Deleted " + targets.get(0).getName() + "."
                            : "Deleted " + removed + " records.",
                    StatusBar.Level.SUCCESS);
        } catch (DataAccessException e) {
            reportStorageFailure(e);
        }
    }

    private void doClear() {
        formPanel.clear();
        tablePanel.clearSelection();
        formPanel.focusFirstField();
        statusBar.setMessage("Form cleared - ready for a new student.", StatusBar.Level.INFO);
    }

    /** Writes whatever the grid is currently showing to a CSV file. */
    private void doExport() {
        List<Student> rows = tablePanel.getVisibleStudents();
        if (rows.isEmpty()) {
            statusBar.setMessage("Nothing to export - the current view is empty.", StatusBar.Level.WARNING);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export current view");
        chooser.setSelectedFile(new File("students-export.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV file (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            target = new File(target.getParentFile(), target.getName() + ".csv");
        }
        try {
            service.exportCsv(rows, target.toPath());
            statusBar.setMessage("Exported " + rows.size() + " records to " + target.getName() + ".",
                    StatusBar.Level.SUCCESS);
        } catch (IOException e) {
            Dialogs.error(this, "Export failed", "Could not write the file: " + e.getMessage());
            statusBar.setMessage("Export failed.", StatusBar.Level.ERROR);
        }
    }

    private void confirmAndExit() {
        if (Dialogs.confirmExit(this)) {
            dispose();
            System.exit(0);
        }
    }

    // ---------------------------------------------------------------- state

    /**
     * Re-runs the active query, repaints the grid and refreshes the dashboard.
     *
     * @param message status text, or {@code null} to auto-describe the filter
     */
    private void refresh(String message, StatusBar.Level level) {
        List<Student> rows = service.search(currentQuery);
        tablePanel.setStudents(rows, currentQuery.isEmpty()
                ? "No students yet"
                : "No students match " + currentQuery.describe());
        updateDashboard();
        if (message != null) {
            statusBar.setMessage(message, level);
        } else if (currentQuery.isEmpty()) {
            statusBar.setMessage("Showing all " + rows.size() + " records.", StatusBar.Level.INFO);
        } else {
            statusBar.setMessage("Showing " + rows.size() + " of " + service.count()
                    + " records - " + currentQuery.describe() + ".", StatusBar.Level.INFO);
        }
    }

    private void updateDashboard() {
        StudentService.Stats stats = service.stats();

        totalTile.setValue(String.valueOf(stats.getTotal()));
        totalTile.setCaption(stats.getTotal() == 0 ? "Add your first student" : "records on file");

        averageTile.setValue(stats.getTotal() == 0
                ? "-"
                : String.format(Locale.ROOT, "%.2f", Double.valueOf(stats.getAverageCgpa())));
        averageTile.setCaption("across all students");

        departmentTile.setValue(String.valueOf(stats.getActiveDepartments()));
        Department largest = stats.getLargestDepartment();
        departmentTile.setCaption(largest == null ? "none yet" : "largest: " + largest.code());

        Student topper = stats.getTopper();
        if (topper == null) {
            topperTile.setValue("-");
            topperTile.setCaption("no records yet");
        } else {
            topperTile.setValue(String.format(Locale.ROOT, "%.2f", Double.valueOf(topper.getCgpa())));
            topperTile.setCaption(topper.getName() + " - " + topper.getDepartment().code());
        }
    }

    private void reportInvalid(ValidationException e) {
        formPanel.showErrors(e.getErrors());
        statusBar.setMessage(e.getErrors().size() == 1
                        ? "1 field needs attention."
                        : e.getErrors().size() + " fields need attention.",
                StatusBar.Level.ERROR);
    }

    private void reportStorageFailure(DataAccessException e) {
        Dialogs.error(this, "Storage error", e.getMessage());
        statusBar.setMessage("Could not save to disk.", StatusBar.Level.ERROR);
    }
}
