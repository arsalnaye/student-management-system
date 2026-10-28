package com.sms.ui;

import com.sms.model.Department;
import com.sms.model.Gender;
import com.sms.model.Student;
import com.sms.service.ValidationException;
import com.sms.ui.components.ActionButton;
import com.sms.ui.components.CardPanel;
import com.sms.ui.components.HintTextField;
import com.sms.ui.theme.Theme;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The data-entry form: one card holding every student field plus the four
 * commands.
 *
 * <p>Responsibilities are deliberately narrow - collect input, show validation
 * feedback, announce which button was pressed. It never calls the service, so
 * the same form could sit inside a dialog or a wizard unchanged.</p>
 */
public class StudentFormPanel extends CardPanel {

    private static final long serialVersionUID = 1L;

    /** The commands this form can raise. */
    public enum Command {
        ADD, UPDATE, DELETE, CLEAR
    }

    /** Callback used to report a pressed command to the controller. */
    public interface CommandListener {
        void onCommand(Command command);
    }

    private static final int DEFAULT_AGE = 20;

    private final JTextField rollField = new HintTextField("e.g. 23CSE101");
    private final JTextField nameField = new HintTextField("Full name");
    private final JTextField emailField = new HintTextField("name@college.edu");
    private final JTextField phoneField = new HintTextField("10 digits");
    private final JComboBox<Department> departmentCombo = new JComboBox<Department>(Department.values());
    private final JComboBox<Integer> semesterCombo = new JComboBox<Integer>();
    private final JTextField cgpaField = new HintTextField("0.00 - 10.00");
    private final JRadioButton maleRadio = radio("Male");
    private final JRadioButton femaleRadio = radio("Female");
    private final JRadioButton otherRadio = radio("Other");
    private final ButtonGroup genderGroup = new ButtonGroup();
    private final JSpinner dobSpinner = new JSpinner(new SpinnerDateModel());
    private final JTextArea addressArea = new JTextArea(3, 18);

    private final JLabel modeLabel = new JLabel();
    private final JLabel errorLabel = new JLabel();

    private final ActionButton addButton =
            ActionButton.of("Add Student", ActionButton.Style.PRIMARY, "Save a new record (Ctrl+N)");
    private final ActionButton updateButton =
            ActionButton.of("Update", ActionButton.Style.SUCCESS, "Save changes to the selected record (Ctrl+S)");
    private final ActionButton deleteButton =
            ActionButton.of("Delete", ActionButton.Style.DANGER, "Remove the selected record (Del)");
    private final ActionButton clearButton =
            ActionButton.of("Clear", ActionButton.Style.GHOST, "Empty the form (Esc)");

    /** Field key -> input component, used to place validation highlights. */
    private final Map<String, JComponent> inputsByField = new LinkedHashMap<String, JComponent>();

    private CommandListener commandListener;
    private int editingId = Student.UNSAVED_ID;

    public StudentFormPanel() {
        super(new BorderLayout(0, Theme.GAP_S));
        buildInputs();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildFields(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        wireEvents();
        clear();
    }

    // ------------------------------------------------------------- building

    private void buildInputs() {
        for (int i = 1; i <= 8; i++) {
            semesterCombo.addItem(Integer.valueOf(i));
        }
        Theme.styleInput(departmentCombo);
        Theme.styleInput(semesterCombo);

        dobSpinner.setEditor(new JSpinner.DateEditor(dobSpinner, "dd-MM-yyyy"));
        dobSpinner.setValue(defaultDateOfBirth());
        Theme.styleInput(dobSpinner);

        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        addressArea.setFont(Theme.FONT_BODY);
        addressArea.setBorder(Theme.padding(4, 4, 4, 4));

        genderGroup.add(maleRadio);
        genderGroup.add(femaleRadio);
        genderGroup.add(otherRadio);
        maleRadio.setSelected(true);

        inputsByField.put("rollNo", rollField);
        inputsByField.put("name", nameField);
        inputsByField.put("email", emailField);
        inputsByField.put("phone", phoneField);
        inputsByField.put("department", departmentCombo);
        inputsByField.put("semester", semesterCombo);
        inputsByField.put("cgpa", cgpaField);
        inputsByField.put("dateOfBirth", dobSpinner);

        for (JComponent input : inputsByField.values()) {
            installFocusHighlight(input);
        }
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Student Details");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT);

        modeLabel.setFont(Theme.FONT_SMALL);
        modeLabel.setForeground(Theme.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(modeLabel, BorderLayout.SOUTH);
        header.setBorder(Theme.padding(0, 2, Theme.GAP_S, 2));
        return header;
    }

    private JComponent buildFields() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 2, 0);

        addRow(grid, gc, "Roll Number *", rollField);
        addRow(grid, gc, "Full Name *", nameField);
        addRow(grid, gc, "Email *", emailField);
        addRow(grid, gc, "Phone *", phoneField);
        addRow(grid, gc, "Department *", departmentCombo);

        // Semester and CGPA share a row: two half-width cells.
        gc.gridwidth = 1;
        gc.weightx = 0.5;
        gc.insets = new Insets(Theme.GAP_S, 0, 2, Theme.GAP_S);
        grid.add(fieldLabel("Semester *"), at(gc, 0, gc.gridy));
        gc.insets = new Insets(Theme.GAP_S, 0, 2, 0);
        grid.add(fieldLabel("CGPA *"), at(gc, 1, gc.gridy));
        gc.gridy++;
        gc.insets = new Insets(0, 0, 2, Theme.GAP_S);
        grid.add(semesterCombo, at(gc, 0, gc.gridy));
        gc.insets = new Insets(0, 0, 2, 0);
        grid.add(cgpaField, at(gc, 1, gc.gridy));
        gc.gridy++;
        gc.gridwidth = 2;
        gc.weightx = 1.0;

        addRow(grid, gc, "Gender", genderRow());
        addRow(grid, gc, "Date of Birth *", dobSpinner);

        gc.insets = new Insets(Theme.GAP_S, 0, 2, 0);
        grid.add(fieldLabel("Address"), at(gc, 0, gc.gridy));
        gc.gridy++;
        gc.insets = new Insets(0, 0, 2, 0);
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        JScrollPane addressScroll = new JScrollPane(addressArea);
        addressScroll.setBorder(Theme.inputBorder());
        addressScroll.getViewport().setBackground(Theme.SURFACE);
        grid.add(addressScroll, at(gc, 0, gc.gridy));
        gc.gridy++;

        JScrollPane formScroll = new JScrollPane(grid);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        return formScroll;
    }

    private JComponent genderRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, Theme.GAP_XS, 0));
        row.setOpaque(false);
        row.add(maleRadio);
        row.add(femaleRadio);
        row.add(otherRadio);
        return row;
    }

    private JComponent buildFooter() {
        errorLabel.setFont(Theme.FONT_SMALL);
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        errorLabel.setBorder(Theme.padding(Theme.GAP_XS, 2, Theme.GAP_S, 2));

        JPanel buttons = new JPanel(new GridLayout(2, 2, Theme.GAP_S, Theme.GAP_S));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(errorLabel, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);
        return footer;
    }

    private void addRow(JPanel grid, GridBagConstraints gc, String label, JComponent input) {
        gc.insets = new Insets(Theme.GAP_S, 0, 2, 0);
        grid.add(fieldLabel(label), at(gc, 0, gc.gridy));
        gc.gridy++;
        gc.insets = new Insets(0, 0, 2, 0);
        grid.add(input, at(gc, 0, gc.gridy));
        gc.gridy++;
    }

    private static GridBagConstraints at(GridBagConstraints gc, int x, int y) {
        gc.gridx = x;
        gc.gridy = y;
        return gc;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(Theme.FONT_LABEL);
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    private static JRadioButton radio(String text) {
        JRadioButton button = new JRadioButton(text);
        button.setOpaque(false);
        button.setFont(Theme.FONT_BODY);
        button.setForeground(Theme.TEXT);
        button.setFocusPainted(false);
        return button;
    }

    // --------------------------------------------------------------- events

    private void wireEvents() {
        addButton.addActionListener(command(Command.ADD));
        updateButton.addActionListener(command(Command.UPDATE));
        deleteButton.addActionListener(command(Command.DELETE));
        clearButton.addActionListener(command(Command.CLEAR));

        addButton.setMnemonic('A');
        updateButton.setMnemonic('U');
        deleteButton.setMnemonic('D');
        clearButton.setMnemonic('C');
    }

    private ActionListener command(final Command command) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (commandListener != null) {
                    commandListener.onCommand(command);
                }
            }
        };
    }

    /** Registers the controller that reacts to the four buttons. */
    public void setCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    /**
     * Blue border while focused; a red error border stays until it is cleared.
     *
     * <p>A {@link JSpinner} never receives focus itself - its editor's text
     * field does - so the listener is attached there while the border is still
     * applied to the spinner.</p>
     */
    private void installFocusHighlight(final JComponent input) {
        java.awt.Component target = input;
        if (input instanceof JSpinner) {
            javax.swing.JComponent editor = ((JSpinner) input).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                target = ((JSpinner.DefaultEditor) editor).getTextField();
            }
        }
        target.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!Boolean.TRUE.equals(input.getClientProperty("sms.error"))) {
                    input.setBorder(Theme.focusBorder());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!Boolean.TRUE.equals(input.getClientProperty("sms.error"))) {
                    input.setBorder(Theme.inputBorder());
                }
            }
        });
    }

    // ------------------------------------------------------------- data API

    /**
     * Reads the form into a {@link Student}.
     *
     * <p>Only <em>parse</em> failures are reported here (a CGPA that is not a
     * number); every business rule is checked by the service, so the two layers
     * never disagree about what "valid" means.</p>
     *
     * @return a student carrying the id currently being edited
     * @throws ValidationException if a value cannot be converted at all
     */
    public Student getFormData() throws ValidationException {
        Map<String, String> errors = new LinkedHashMap<String, String>();

        double cgpa = 0.0;
        String rawCgpa = cgpaField.getText().trim();
        if (rawCgpa.isEmpty()) {
            errors.put("cgpa", "CGPA is required.");
        } else {
            try {
                cgpa = Double.parseDouble(rawCgpa.replace(',', '.'));
            } catch (NumberFormatException nfe) {
                errors.put("cgpa", "CGPA must be a number such as 8.75.");
            }
        }

        LocalDate dob = null;
        Object spinnerValue = dobSpinner.getValue();
        if (spinnerValue instanceof Date) {
            dob = ((Date) spinnerValue).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            errors.put("dateOfBirth", "Pick a valid date of birth.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Integer semester = (Integer) semesterCombo.getSelectedItem();
        return Student.builder()
                .id(editingId)
                .rollNo(rollField.getText())
                .name(nameField.getText())
                .email(emailField.getText())
                .phone(phoneField.getText())
                .department((Department) departmentCombo.getSelectedItem())
                .semester(semester == null ? 0 : semester.intValue())
                .cgpa(cgpa)
                .gender(selectedGender())
                .dateOfBirth(dob)
                .address(addressArea.getText())
                .build();
    }

    /** Loads {@code student} into the inputs and switches the form to edit mode. */
    public void setStudent(Student student) {
        if (student == null) {
            clear();
            return;
        }
        clearErrors();
        editingId = student.getId();
        rollField.setText(student.getRollNo());
        nameField.setText(student.getName());
        emailField.setText(student.getEmail());
        phoneField.setText(student.getPhone());
        departmentCombo.setSelectedItem(student.getDepartment());
        semesterCombo.setSelectedItem(Integer.valueOf(student.getSemester()));
        cgpaField.setText(String.format(Locale.ROOT, "%.2f", Double.valueOf(student.getCgpa())));
        selectGender(student.getGender());
        dobSpinner.setValue(student.getDateOfBirth() == null
                ? defaultDateOfBirth()
                : Date.from(student.getDateOfBirth().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        addressArea.setText(student.getAddress());
        addressArea.setCaretPosition(0);
        setEditingMode(true, student);
    }

    /** Empties every input and returns the form to "add new" mode. */
    public void clear() {
        clearErrors();
        editingId = Student.UNSAVED_ID;
        rollField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        departmentCombo.setSelectedIndex(0);
        semesterCombo.setSelectedIndex(0);
        cgpaField.setText("");
        maleRadio.setSelected(true);
        dobSpinner.setValue(defaultDateOfBirth());
        addressArea.setText("");
        setEditingMode(false, null);
    }

    /** @return the id being edited, or {@link Student#UNSAVED_ID} in add mode. */
    public int getEditingId() {
        return editingId;
    }

    /** Moves keyboard focus to the first input. */
    public void focusFirstField() {
        rollField.requestFocusInWindow();
    }

    // -------------------------------------------------------------- feedback

    /**
     * Highlights each offending input and lists the messages under the form.
     *
     * @param errors field key -> message, as produced by the service
     */
    public void showErrors(Map<String, String> errors) {
        clearErrors();
        StringBuilder html = new StringBuilder("<html><body style='width:230px'>");
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            JComponent input = inputsByField.get(entry.getKey());
            if (input != null) {
                input.putClientProperty("sms.error", Boolean.TRUE);
                input.setBorder(Theme.errorBorder());
                input.setToolTipText(entry.getValue());
            }
            html.append("&bull; ").append(escapeHtml(entry.getValue())).append("<br>");
        }
        html.append("</body></html>");
        errorLabel.setText(html.toString());

        // Put the cursor where the first problem is.
        for (String key : errors.keySet()) {
            JComponent input = inputsByField.get(key);
            if (input != null) {
                input.requestFocusInWindow();
                break;
            }
        }
    }

    /** Removes every red highlight and the message list. */
    public void clearErrors() {
        for (JComponent input : inputsByField.values()) {
            input.putClientProperty("sms.error", Boolean.FALSE);
            input.setBorder(Theme.inputBorder());
            input.setToolTipText(null);
        }
        errorLabel.setText("");
    }

    // --------------------------------------------------------------- private

    private void setEditingMode(boolean editing, Student student) {
        updateButton.setEnabled(editing);
        deleteButton.setEnabled(editing);
        addButton.setEnabled(!editing);
        if (editing && student != null) {
            modeLabel.setText("Editing record #" + student.getId() + " - " + student.getName());
        } else {
            modeLabel.setText("Adding a new record. Fields marked * are required.");
        }
    }

    private Gender selectedGender() {
        if (femaleRadio.isSelected()) {
            return Gender.FEMALE;
        }
        if (otherRadio.isSelected()) {
            return Gender.OTHER;
        }
        return Gender.MALE;
    }

    private void selectGender(Gender gender) {
        if (gender == Gender.FEMALE) {
            femaleRadio.setSelected(true);
        } else if (gender == Gender.OTHER) {
            otherRadio.setSelected(true);
        } else {
            maleRadio.setSelected(true);
        }
    }

    private static Date defaultDateOfBirth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -DEFAULT_AGE);
        return calendar.getTime();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Keeps the side panel a sensible width regardless of content. */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = Math.max(size.width, 330);
        return size;
    }

}
