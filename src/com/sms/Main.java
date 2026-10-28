package com.sms;

import com.sms.dao.CsvStudentDao;
import com.sms.dao.DataAccessException;
import com.sms.dao.JdbcStudentDao;
import com.sms.dao.StudentDao;
import com.sms.db.Database;
import com.sms.service.StudentService;
import com.sms.ui.Dialogs;
import com.sms.ui.MainFrame;
import com.sms.ui.theme.Theme;
import com.sms.util.SampleData;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Paths;
import java.util.List;

/**
 * Application entry point.
 *
 * <p>Wires the layers together (database, DAO, service, window) and hands
 * control to Swing. Everything after {@link SwingUtilities#invokeLater} runs on
 * the Event Dispatch Thread, which is the one rule Swing does not forgive.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   java -cp "out;lib/*" com.sms.Main                  default SQLite database
 *   java -cp "out;lib/*" com.sms.Main --db my.db       a different SQLite file
 *   java -cp "out;lib/*" com.sms.Main --csv my.csv     the CSV store instead
 * </pre>
 *
 * <p>With no arguments the settings come from {@code db.properties} when that
 * file exists, so pointing the application at MySQL never needs a recompile.</p>
 */
public final class Main {

    private static final String DEFAULT_DB_FILE = "data/students.db";

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Student Management System needs a graphical display.");
            System.exit(2);
        }

        final String storeArgument = valueOf(args, "--db");
        final String csvArgument = valueOf(args, "--csv");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Theme.install();
                try {
                    start(storeArgument, csvArgument);
                } catch (DataAccessException e) {
                    JOptionPane.showMessageDialog(null,
                            "Could not open the database:\n" + e.getMessage(),
                            "Student Management System", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        });
    }

    private static void start(String dbArgument, String csvArgument) {
        StudentDao dao;
        String description;
        List<String> warnings = null;

        if (csvArgument != null) {
            // The alternative store. It exists to keep the DAO interface honest:
            // the service and the UI cannot tell which one they are running on.
            CsvStudentDao csvDao = new CsvStudentDao(Paths.get(csvArgument));
            warnings = csvDao.loadWarnings();
            dao = csvDao;
            description = "csv : " + csvDao.file().toAbsolutePath();
        } else {
            Database database = dbArgument != null
                    ? Database.sqlite(Paths.get(dbArgument))
                    : Database.fromConfig(Paths.get(Database.CONFIG_FILE), Paths.get(DEFAULT_DB_FILE));
            database.createSchema();
            dao = new JdbcStudentDao(database);
            description = database.describe();
        }

        StudentService service = new StudentService(dao);
        if (service.count() == 0) {
            service.seed(SampleData.students());
        }

        MainFrame frame = new MainFrame(service, description);
        frame.setVisible(true);

        if (warnings != null && !warnings.isEmpty()) {
            Dialogs.error(frame, "Some records could not be read",
                    warnings.size() + " line(s) in the CSV file were skipped because they are"
                            + " malformed. The first problem was: " + warnings.get(0));
        }
    }

    /**
     * @return the value following {@code flag}, or {@code null} when the flag is
     *         absent or has nothing after it
     */
    private static String valueOf(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equalsIgnoreCase(args[i]) && !args[i + 1].trim().isEmpty()) {
                return args[i + 1].trim();
            }
        }
        return null;
    }
}
