package com.sms.ui;

import com.sms.model.Department;
import com.sms.model.Gender;
import com.sms.model.Student;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapts a {@code List<Student>} to Swing's {@link javax.swing.JTable}.
 *
 * <p>The model is read-only: edits happen in the form, never in the grid, so
 * every change funnels through validation. {@link #getColumnClass(int)} returns
 * real types ({@code Integer}, {@code Double}, {@code LocalDate}) so that
 * {@code TableRowSorter} sorts numbers and dates correctly instead of
 * alphabetically.</p>
 */
public class StudentTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** Column definitions in display order. */
    private static final String[] COLUMNS = {
            "ID", "Roll No", "Name", "Department", "Sem", "CGPA", "Email", "Phone", "Gender", "Date of Birth"
    };

    public static final int COL_ID = 0;
    public static final int COL_ROLL = 1;
    public static final int COL_NAME = 2;
    public static final int COL_DEPARTMENT = 3;
    public static final int COL_SEMESTER = 4;
    public static final int COL_CGPA = 5;
    public static final int COL_EMAIL = 6;
    public static final int COL_PHONE = 7;
    public static final int COL_GENDER = 8;
    public static final int COL_DOB = 9;

    private List<Student> rows = new ArrayList<Student>();

    /** Replaces every row and repaints the table once. */
    public void setStudents(List<Student> students) {
        this.rows = students == null ? new ArrayList<Student>() : new ArrayList<Student>(students);
        fireTableDataChanged();
    }

    /** @return the students currently displayed, in display order. */
    public List<Student> getStudents() {
        return Collections.unmodifiableList(rows);
    }

    /**
     * @param modelRow row index in <em>model</em> coordinates
     * @return the student on that row, or {@code null} if the index is stale
     */
    public Student getStudentAt(int modelRow) {
        return modelRow >= 0 && modelRow < rows.size() ? rows.get(modelRow) : null;
    }

    /** @return the model row holding {@code id}, or {@code -1}. */
    public int indexOfId(int id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case COL_ID:
            case COL_SEMESTER:
                return Integer.class;
            case COL_CGPA:
                return Double.class;
            case COL_DEPARTMENT:
                return Department.class;
            case COL_GENDER:
                return Gender.class;
            case COL_DOB:
                return LocalDate.class;
            default:
                return String.class;
        }
    }

    /** Always {@code false}: the grid displays, the form edits. */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Student s = rows.get(rowIndex);
        switch (columnIndex) {
            case COL_ID:
                return Integer.valueOf(s.getId());
            case COL_ROLL:
                return s.getRollNo();
            case COL_NAME:
                return s.getName();
            case COL_DEPARTMENT:
                return s.getDepartment();
            case COL_SEMESTER:
                return Integer.valueOf(s.getSemester());
            case COL_CGPA:
                return Double.valueOf(s.getCgpa());
            case COL_EMAIL:
                return s.getEmail();
            case COL_PHONE:
                return s.getPhone();
            case COL_GENDER:
                return s.getGender();
            case COL_DOB:
                return s.getDateOfBirth();
            default:
                return null;
        }
    }
}
