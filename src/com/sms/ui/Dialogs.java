package com.sms.ui;

import com.sms.model.Student;
import com.sms.ui.theme.Theme;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.List;

/**
 * Thin wrappers around {@link JOptionPane} so every dialog in the app shares one
 * voice: same titles, same button order, same phrasing.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** @return {@code true} if the user confirms deleting {@code students}. */
    public static boolean confirmDelete(Component parent, List<Student> students) {
        String message;
        if (students.size() == 1) {
            Student s = students.get(0);
            message = "<html><body style='width:280px'>Delete <b>" + escape(s.getName()) + "</b> ("
                    + escape(s.getRollNo()) + ")?<br><br>"
                    + "This removes the record from the data file and cannot be undone.</body></html>";
        } else {
            message = "<html><body style='width:280px'>Delete <b>" + students.size()
                    + " selected records</b>?<br><br>This cannot be undone.</body></html>";
        }
        int choice = JOptionPane.showConfirmDialog(parent, message, "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /** @return {@code true} if the user confirms closing the application. */
    public static boolean confirmExit(Component parent) {
        int choice = JOptionPane.showConfirmDialog(parent,
                "Close Student Management System?", "Confirm exit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    public static void error(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent,
                "<html><body style='width:300px'>" + escape(message) + "</body></html>",
                title, JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent,
                "<html><body style='width:300px'>" + escape(message) + "</body></html>",
                title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** The Help > About window. */
    public static void about(Component parent) {
        String html = "<html><body style='width:340px;font-family:" + Theme.FONT_BODY.getFamily() + "'>"
                + "<h2 style='margin-bottom:4px'>Student Management System</h2>"
                + "<p style='margin-top:0;color:#647085'>Version 1.0 &nbsp;&middot;&nbsp; Java Swing desktop app</p>"
                + "<p>A three-layer CRUD application: an immutable model, a swappable "
                + "CSV data-access layer, a service that owns every business rule, and a "
                + "Swing view that only renders and reports gestures.</p>"
                + "<p><b>Built with:</b> Java SE, Swing and AWT only - no external libraries.</p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(parent, html, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    /** The Help > Keyboard shortcuts window. */
    public static void shortcuts(Component parent) {
        String html = "<html><body style='width:320px'>"
                + "<h3 style='margin-bottom:6px'>Keyboard shortcuts</h3>"
                + "<table cellpadding='3'>"
                + row("Ctrl + N", "Clear the form to add a new student")
                + row("Ctrl + S", "Save the record being edited")
                + row("Ctrl + F", "Jump to the search box")
                + row("Delete", "Delete the selected row(s)")
                + row("Esc", "Clear the form and reset filters")
                + row("F5", "Reload from the data file")
                + row("Ctrl + E", "Export the current view to CSV")
                + row("Ctrl + Q", "Quit")
                + row("Double-click", "Load a row into the form")
                + "</table></body></html>";
        JOptionPane.showMessageDialog(parent, html, "Keyboard shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String row(String key, String description) {
        return "<tr><td><b><code>" + key + "</code></b></td><td>" + description + "</td></tr>";
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
