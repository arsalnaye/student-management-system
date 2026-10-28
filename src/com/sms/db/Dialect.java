package com.sms.db;

import java.sql.SQLException;
import java.util.Locale;

/**
 * The small set of things that genuinely differ between SQLite and MySQL.
 *
 * <p>Everything else in the data-access layer is plain JDBC and portable SQL, so
 * moving between the two databases means changing {@code db.properties} and
 * nothing else. Isolating the differences here is what makes that claim true
 * rather than aspirational.</p>
 *
 * <p>The three real differences are the auto-increment syntax, the column types,
 * and how a unique-constraint violation is reported.</p>
 */
public enum Dialect {

    /** File-backed, zero install. The default for this project. */
    SQLITE {
        @Override
        public String createTableSql() {
            return "CREATE TABLE IF NOT EXISTS student ("
                    + "  id            INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  roll_no       TEXT    NOT NULL COLLATE NOCASE UNIQUE,"
                    + "  name          TEXT    NOT NULL,"
                    + "  email         TEXT    NOT NULL COLLATE NOCASE UNIQUE,"
                    + "  phone         TEXT    NOT NULL,"
                    + "  department    TEXT    NOT NULL,"
                    + "  semester      INTEGER NOT NULL,"
                    + "  cgpa          REAL    NOT NULL,"
                    + "  gender        TEXT    NOT NULL,"
                    + "  date_of_birth TEXT    NOT NULL,"
                    + "  address       TEXT    NOT NULL DEFAULT ''"
                    + ")";
        }

        /**
         * SQLite reports every constraint failure through the vendor error code;
         * {@code getSQLState()} is null, so there is nothing else to test.
         * 19 is SQLITE_CONSTRAINT, and the message names the failing index.
         */
        @Override
        public boolean isUniqueViolation(SQLException e) {
            return e.getErrorCode() == 19
                    && String.valueOf(e.getMessage()).toUpperCase(Locale.ROOT).contains("UNIQUE");
        }

        @Override
        public String defaultDriverClass() {
            return "org.sqlite.JDBC";
        }
    },

    /** The database the assignment names; supported by changing configuration only. */
    MYSQL {
        @Override
        public String createTableSql() {
            return "CREATE TABLE IF NOT EXISTS student ("
                    + "  id            INT AUTO_INCREMENT PRIMARY KEY,"
                    + "  roll_no       VARCHAR(20)  NOT NULL UNIQUE,"
                    + "  name          VARCHAR(50)  NOT NULL,"
                    + "  email         VARCHAR(120) NOT NULL UNIQUE,"
                    + "  phone         VARCHAR(15)  NOT NULL,"
                    + "  department    VARCHAR(16)  NOT NULL,"
                    + "  semester      INT          NOT NULL,"
                    + "  cgpa          DECIMAL(4,2) NOT NULL,"
                    + "  gender        VARCHAR(10)  NOT NULL,"
                    + "  date_of_birth DATE         NOT NULL,"
                    + "  address       VARCHAR(255) NOT NULL DEFAULT ''"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }

        /** MySQL uses the standard integrity-violation SQLState plus code 1062. */
        @Override
        public boolean isUniqueViolation(SQLException e) {
            return e.getErrorCode() == 1062 || "23000".equals(e.getSQLState());
        }

        @Override
        public String defaultDriverClass() {
            return "com.mysql.cj.jdbc.Driver";
        }
    };

    /** @return the {@code CREATE TABLE} statement for this dialect, safe to re-run. */
    public abstract String createTableSql();

    /** @return {@code true} if {@code e} means "that roll number or email is taken". */
    public abstract boolean isUniqueViolation(SQLException e);

    /** @return the JDBC driver class, used only when a driver is not auto-discovered. */
    public abstract String defaultDriverClass();

    /**
     * Guesses the dialect from a JDBC URL, so a correct {@code db.url} alone is
     * usually enough configuration.
     *
     * @param url a JDBC URL such as {@code jdbc:sqlite:data/students.db}
     * @return the matching dialect
     * @throws IllegalArgumentException if the URL names an unsupported database
     */
    public static Dialect fromUrl(String url) {
        String lower = String.valueOf(url).toLowerCase(Locale.ROOT);
        if (lower.startsWith("jdbc:sqlite:")) {
            return SQLITE;
        }
        if (lower.startsWith("jdbc:mysql:") || lower.startsWith("jdbc:mariadb:")) {
            return MYSQL;
        }
        throw new IllegalArgumentException("Unsupported JDBC URL: " + url
                + " (expected jdbc:sqlite: or jdbc:mysql:)");
    }

    /** Parses a dialect name such as {@code sqlite} or {@code MySQL}. */
    public static Dialect fromName(String name) {
        String needle = String.valueOf(name).trim().toUpperCase(Locale.ROOT);
        for (Dialect d : values()) {
            if (d.name().equals(needle)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown dialect: " + name);
    }
}
