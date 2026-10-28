package com.sms.db;

import com.sms.dao.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Owns the JDBC connection settings and hands out connections.
 *
 * <p>One instance is created at start-up and passed to the DAO. Each database
 * operation opens and closes its own {@link Connection} through
 * try-with-resources: for a single-user desktop application against a local file
 * that is both simple and fast. A server-side application would put a pool in
 * front of exactly this method and change nothing else.</p>
 *
 * <p>Configuration comes from {@code db.properties} when present, so switching
 * from SQLite to MySQL never requires a recompile.</p>
 */
public final class Database {

    /** Configuration file looked for next to the application. */
    public static final String CONFIG_FILE = "db.properties";

    private final String url;
    private final String user;
    private final String password;
    private final Dialect dialect;

    /**
     * @param url      JDBC URL, for example {@code jdbc:sqlite:data/students.db}
     * @param user     database user, may be empty for SQLite
     * @param password database password, may be empty for SQLite
     * @param dialect  which SQL flavour {@code url} speaks
     */
    public Database(String url, String user, String password, Dialect dialect) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("A JDBC url is required");
        }
        this.url = url.trim();
        this.user = user == null ? "" : user;
        this.password = password == null ? "" : password;
        this.dialect = dialect == null ? Dialect.fromUrl(this.url) : dialect;
    }

    /** Convenience factory for a SQLite file, used by the app and by the tests. */
    public static Database sqlite(Path databaseFile) {
        Path absolute = databaseFile.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new DataAccessException("Could not create the folder for "
                        + absolute + ": " + e.getMessage(), e);
            }
        }
        return new Database("jdbc:sqlite:" + absolute, "", "", Dialect.SQLITE);
    }

    /**
     * Loads settings from {@code db.properties} if it exists, otherwise falls
     * back to a SQLite file at {@code fallbackSqliteFile}.
     *
     * <p>Recognised keys: {@code db.url}, {@code db.user}, {@code db.password},
     * {@code db.driver}, {@code db.dialect}.</p>
     *
     * @param configFile         path to the properties file, may not exist
     * @param fallbackSqliteFile database file used when there is no config
     * @return a configured instance, never {@code null}
     */
    public static Database fromConfig(Path configFile, Path fallbackSqliteFile) {
        if (!Files.exists(configFile)) {
            return sqlite(fallbackSqliteFile);
        }
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = Files.newInputStream(configFile);
            props.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + configFile + ": " + e.getMessage(), e);
        } finally {
            closeQuietly(in);
        }

        String url = props.getProperty("db.url", "").trim();
        if (url.isEmpty()) {
            return sqlite(fallbackSqliteFile);
        }
        Dialect dialect = props.containsKey("db.dialect")
                ? Dialect.fromName(props.getProperty("db.dialect"))
                : Dialect.fromUrl(url);

        loadDriverIfRequested(props.getProperty("db.driver", "").trim());

        if (dialect == Dialect.SQLITE) {
            createSqliteParentFolder(url);
        }
        return new Database(url, props.getProperty("db.user", ""),
                props.getProperty("db.password", ""), dialect);
    }

    /**
     * Opens a connection. Callers must close it, ideally with try-with-resources.
     *
     * @throws DataAccessException if the database cannot be reached
     */
    public Connection open() {
        try {
            if (user.isEmpty() && password.isEmpty()) {
                return DriverManager.getConnection(url);
            }
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException(describeConnectionFailure(e), e);
        }
    }

    /**
     * Creates the {@code student} table when it is missing. Safe to call on every
     * start-up: the statement is {@code CREATE TABLE IF NOT EXISTS}.
     */
    public void createSchema() {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(dialect.createTableSql());
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the student table: " + e.getMessage(), e);
        }
    }

    public Dialect dialect() {
        return dialect;
    }

    public String url() {
        return url;
    }

    /** @return a short description for the status bar, with any password removed. */
    public String describe() {
        return dialect.name().toLowerCase(java.util.Locale.ROOT) + " : " + url;
    }

    @Override
    public String toString() {
        return "Database[" + describe() + ']';
    }

    // ------------------------------------------------------------- internals

    private static void loadDriverIfRequested(String driverClass) {
        if (driverClass.isEmpty()) {
            return; // JDBC 4 discovers drivers on the classpath automatically.
        }
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new DataAccessException("JDBC driver " + driverClass
                    + " is not on the classpath. Put its jar in the lib folder.", e);
        }
    }

    private static void createSqliteParentFolder(String url) {
        String path = url.substring("jdbc:sqlite:".length());
        if (path.isEmpty() || path.startsWith(":")) {
            return; // in-memory database, nothing to create
        }
        Path parent = Paths.get(path).toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new DataAccessException("Could not create " + parent + ": " + e.getMessage(), e);
        }
    }

    private String describeConnectionFailure(SQLException e) {
        StringBuilder sb = new StringBuilder("Could not connect to ").append(url).append(". ");
        if (dialect == Dialect.SQLITE) {
            sb.append("Check that the sqlite-jdbc jar is in the lib folder. ");
        } else {
            sb.append("Check that the MySQL server is running and that the ")
                    .append("mysql-connector-j jar is in the lib folder. ");
        }
        return sb.append(e.getMessage()).toString();
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
                // Nothing useful to do while already unwinding.
            }
        }
    }
}
