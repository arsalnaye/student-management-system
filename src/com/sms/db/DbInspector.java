package com.sms.db;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prints what is actually inside the database: the driver, the table
 * definition, every row, and a few aggregates.
 *
 * <p>Useful for three things: proving to yourself that the Swing application
 * really did write to SQLite, producing the database screenshots for the
 * report, and checking a MySQL connection before launching the full app.</p>
 *
 * <pre>
 *   java -cp "out;lib/*" com.sms.db.DbInspector                use db.properties
 *   java -cp "out;lib/*" com.sms.db.DbInspector --db other.db  a specific file
 *   java -cp "out;lib/*" com.sms.db.DbInspector --schema       also show the columns
 * </pre>
 *
 * <p>Column headings are aliased to short names so the row table fits inside a
 * standard 120-column terminal without wrapping.</p>
 */
public final class DbInspector {

    private static final String DEFAULT_DB_FILE = "data/students.db";

    private DbInspector() {
    }

    public static void main(String[] args) {
        String file = null;
        boolean showSchema = false;
        for (int i = 0; i < args.length; i++) {
            if ("--db".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                file = args[i + 1];
            } else if ("--schema".equalsIgnoreCase(args[i])) {
                showSchema = true;
            }
        }
        Database database = file != null
                ? Database.sqlite(Paths.get(file))
                : Database.fromConfig(Paths.get(Database.CONFIG_FILE), Paths.get(DEFAULT_DB_FILE));

        try (Connection connection = database.open()) {
            printConnection(connection, database);
            if (showSchema) {
                printColumns(connection);
            }
            printRows(connection);
            printAggregates(connection);
        } catch (SQLException e) {
            System.out.println("Could not read the database: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printConnection(Connection connection, Database database) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        heading("CONNECTION");
        System.out.println("  url            : " + database.url());
        System.out.println("  dialect        : " + database.dialect());
        System.out.println("  database       : " + meta.getDatabaseProductName()
                + " " + meta.getDatabaseProductVersion());
        System.out.println("  jdbc driver    : " + meta.getDriverName() + " " + meta.getDriverVersion());
        System.out.println("  auto-commit    : " + connection.getAutoCommit());
    }

    /**
     * Reads the column layout from {@link ResultSetMetaData} rather than from a
     * vendor-specific PRAGMA, so this works unchanged against MySQL.
     */
    private static void printColumns(Connection connection) throws SQLException {
        heading("TABLE student");
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM student WHERE 1 = 0")) {
            ResultSetMetaData meta = rows.getMetaData();
            List<String[]> table = new ArrayList<String[]>();
            table.add(new String[]{"#", "COLUMN", "TYPE", "NULLABLE"});
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                table.add(new String[]{
                        String.valueOf(i),
                        meta.getColumnName(i),
                        meta.getColumnTypeName(i),
                        meta.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NOT NULL" : "NULL"
                });
            }
            printTable(table);
        }
    }

    private static void printRows(Connection connection) throws SQLException {
        heading("ROWS");
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, roll_no AS roll, name, department AS dept, semester AS sem,"
                             + " cgpa, gender AS sex, date_of_birth AS dob, email, phone"
                             + " FROM student ORDER BY id")) {
            ResultSetMetaData meta = rows.getMetaData();
            int columns = meta.getColumnCount();

            List<String[]> table = new ArrayList<String[]>();
            String[] header = new String[columns];
            for (int i = 1; i <= columns; i++) {
                header[i - 1] = meta.getColumnLabel(i).toUpperCase(Locale.ROOT);
            }
            table.add(header);

            int count = 0;
            while (rows.next()) {
                String[] row = new String[columns];
                for (int i = 1; i <= columns; i++) {
                    Object value = rows.getObject(i);
                    row[i - 1] = value == null ? "" : String.valueOf(value);
                }
                table.add(row);
                count++;
            }
            printTable(table);
            System.out.println();
            System.out.println("  " + count + " row(s)");
        }
    }

    private static void printAggregates(Connection connection) throws SQLException {
        heading("AGGREGATES");
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COUNT(*), AVG(cgpa), MIN(cgpa), MAX(cgpa) FROM student")) {
            if (rows.next()) {
                System.out.println("  students       : " + rows.getInt(1));
                System.out.println("  average cgpa   : " + format(rows.getDouble(2)));
                System.out.println("  lowest cgpa    : " + format(rows.getDouble(3)));
                System.out.println("  highest cgpa   : " + format(rows.getDouble(4)));
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT department, COUNT(*) FROM student GROUP BY department ORDER BY 2 DESC")) {
            System.out.println();
            System.out.println("  headcount per department");
            while (rows.next()) {
                System.out.println("    " + pad(rows.getString(1), 8) + rows.getInt(2));
            }
        }
    }

    // ------------------------------------------------------------ printing

    /** Prints rows as an aligned box, sizing each column to its widest cell. */
    private static void printTable(List<String[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        int columns = rows.get(0).length;
        int[] widths = new int[columns];
        for (String[] row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(widths[i], row[i] == null ? 0 : row[i].length());
            }
        }
        String separator = border(widths);
        System.out.println(separator);
        System.out.println(line(rows.get(0), widths));
        System.out.println(separator);
        for (int r = 1; r < rows.size(); r++) {
            System.out.println(line(rows.get(r), widths));
        }
        System.out.println(separator);
    }

    private static String border(int[] widths) {
        StringBuilder sb = new StringBuilder("  +");
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) {
                sb.append('-');
            }
            sb.append('+');
        }
        return sb.toString();
    }

    private static String line(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < widths.length; i++) {
            sb.append(' ').append(pad(cells[i] == null ? "" : cells[i], widths[i])).append(" |");
        }
        return sb.toString();
    }

    private static String pad(String value, int width) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("=== " + title + " " + repeat('=', Math.max(0, 60 - title.length())));
        System.out.println();
    }

    private static String repeat(char ch, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", Double.valueOf(value));
    }
}
