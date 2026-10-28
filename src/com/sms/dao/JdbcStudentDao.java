package com.sms.dao;

import com.sms.db.Database;
import com.sms.model.Department;
import com.sms.model.Gender;
import com.sms.model.SearchQuery;
import com.sms.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * SQL implementation of {@link StudentDao}, tested against SQLite and written to
 * run unchanged on MySQL.
 *
 * <p>Three rules are followed without exception here:</p>
 * <ol>
 *   <li><b>Every value reaches SQL as a bound parameter.</b> No user input is
 *       ever concatenated into a statement, including in the dynamic search,
 *       so SQL injection is structurally impossible rather than merely
 *       unlikely.</li>
 *   <li><b>Every JDBC object is closed by try-with-resources.</b> Connections,
 *       statements and result sets all implement {@code AutoCloseable}; leaking
 *       them is the classic way a JDBC application dies slowly.</li>
 *   <li><b>SQLExceptions are translated.</b> Callers above this class never see
 *       a vendor error code; they see a {@link DataAccessException} with a
 *       sentence a user could read.</li>
 * </ol>
 */
public class JdbcStudentDao implements StudentDao {

    private static final DateTimeFormatter STORAGE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String COLUMNS =
            "id, roll_no, name, email, phone, department, semester, cgpa, gender, date_of_birth, address";

    private static final String INSERT_SQL =
            "INSERT INTO student (roll_no, name, email, phone, department, semester, cgpa, gender,"
                    + " date_of_birth, address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE student SET roll_no = ?, name = ?, email = ?, phone = ?, department = ?,"
                    + " semester = ?, cgpa = ?, gender = ?, date_of_birth = ?, address = ? WHERE id = ?";

    private final Database database;

    /**
     * @param database connection source; its schema must already exist
     *                 (call {@link Database#createSchema()} once at start-up)
     */
    public JdbcStudentDao(Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database is required");
        }
        this.database = database;
    }

    /** @return a description of the store, for the status bar. */
    public String describe() {
        return database.describe();
    }

    // ----------------------------------------------------------------- reads

    @Override
    public List<Student> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM student ORDER BY id");
    }

    /**
     * Builds the WHERE clause from whichever filters are active.
     *
     * <p>The clause text is assembled from constants only; every value travels
     * as a bound parameter in the matching order. That is the safe way to write
     * dynamic SQL.</p>
     */
    @Override
    public List<Student> search(SearchQuery query) {
        if (query == null || query.isEmpty()) {
            return findAll();
        }
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM student WHERE 1 = 1");
        List<Object> parameters = new ArrayList<Object>();

        if (query.hasKeyword()) {
            sql.append(" AND (LOWER(roll_no) LIKE ? OR LOWER(name) LIKE ? OR LOWER(email) LIKE ?")
                    .append(" OR phone LIKE ? OR LOWER(department) LIKE ?)");
            String like = "%" + query.getKeyword().toLowerCase(Locale.ROOT) + "%";
            for (int i = 0; i < 5; i++) {
                parameters.add(like);
            }
        }
        if (query.getDepartment() != null) {
            sql.append(" AND department = ?");
            parameters.add(query.getDepartment().code());
        }
        if (query.getSemester() != SearchQuery.ANY_SEMESTER) {
            sql.append(" AND semester = ?");
            parameters.add(Integer.valueOf(query.getSemester()));
        }
        if (query.hasMinCgpa()) {
            sql.append(" AND cgpa >= ?");
            parameters.add(Double.valueOf(query.getMinCgpa()));
        }
        sql.append(" ORDER BY id");

        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindAll(statement, parameters);
            try (ResultSet rows = statement.executeQuery()) {
                return readAll(rows);
            }
        } catch (SQLException e) {
            throw translate("search students", e);
        }
    }

    @Override
    public Optional<Student> findById(int id) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNS + " FROM student WHERE id = ?")) {
            statement.setInt(1, id);
            return readOne(statement);
        } catch (SQLException e) {
            throw translate("look up student " + id, e);
        }
    }

    /**
     * The unique indexes on {@code roll_no} and {@code email} are declared
     * {@code COLLATE NOCASE} on SQLite and use MySQL's default case-insensitive
     * collation, so a plain equality test is case-insensitive on both and still
     * uses the index. {@code UPPER(roll_no) = UPPER(?)} would also work but
     * would throw the index away.
     */
    @Override
    public Optional<Student> findByRollNo(String rollNo) {
        return findByUniqueColumn("roll_no", rollNo);
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return findByUniqueColumn("email", email);
    }

    private Optional<Student> findByUniqueColumn(String column, String value) {
        if (value == null) {
            return Optional.empty();
        }
        // `column` is one of two hard-coded constants chosen by the method that
        // calls this one; it never comes from user input.
        String sql = "SELECT " + COLUMNS + " FROM student WHERE " + column + " = ?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value.trim());
            return readOne(statement);
        } catch (SQLException e) {
            throw translate("look up student by " + column, e);
        }
    }

    @Override
    public int count() {
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM student")) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException e) {
            throw translate("count students", e);
        }
    }

    /** A GROUP BY, so the database aggregates instead of the application. */
    @Override
    public Map<Department, Integer> countByDepartment() {
        Map<Department, Integer> counts = new EnumMap<Department, Integer>(Department.class);
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT department, COUNT(*) FROM student GROUP BY department")) {
            while (rows.next()) {
                counts.put(Department.fromToken(rows.getString(1)), Integer.valueOf(rows.getInt(2)));
            }
            return counts;
        } catch (SQLException e) {
            throw translate("count students per department", e);
        }
    }

    @Override
    public double averageCgpa() {
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT AVG(cgpa) FROM student")) {
            // AVG over zero rows is SQL NULL, which getDouble reports as 0.0.
            return rows.next() ? rows.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw translate("average the CGPA", e);
        }
    }

    @Override
    public Optional<Student> findTopper() {
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT " + COLUMNS + " FROM student ORDER BY cgpa DESC, id ASC LIMIT 1")) {
            return rows.next() ? Optional.of(read(rows)) : Optional.<Student>empty();
        } catch (SQLException e) {
            throw translate("find the top performer", e);
        }
    }

    // ---------------------------------------------------------------- writes

    @Override
    public Student insert(Student student) {
        try (Connection connection = database.open();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindStudent(statement, student);
            statement.executeUpdate();
            return student.toBuilder().id(generatedId(statement)).build();
        } catch (SQLException e) {
            throw translate("save " + student.getName(), e);
        }
    }

    @Override
    public Student update(Student student) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            int next = bindStudent(statement, student);
            statement.setInt(next, student.getId());
            if (statement.executeUpdate() == 0) {
                throw new DataAccessException("No student exists with id " + student.getId());
            }
            return student;
        } catch (SQLException e) {
            throw translate("update " + student.getName(), e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM student WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate("delete student " + id, e);
        }
    }

    /**
     * Deletes a batch inside one transaction.
     *
     * <p>Auto-commit is turned off, every delete is batched, and the work is
     * committed only if all of it succeeded. If anything throws, the rollback
     * leaves the table exactly as it was, so a half-finished multi-delete can
     * never be observed.</p>
     */
    @Override
    public int deleteMany(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Connection connection = null;
        try {
            connection = database.open();
            connection.setAutoCommit(false);
            int removed = 0;
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM student WHERE id = ?")) {
                for (Integer id : ids) {
                    if (id == null) {
                        continue;
                    }
                    statement.setInt(1, id.intValue());
                    removed += statement.executeUpdate();
                }
            }
            connection.commit();
            return removed;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw translate("delete " + ids.size() + " students", e);
        } finally {
            restoreAutoCommitAndClose(connection);
        }
    }

    @Override
    public void deleteAll() {
        try (Connection connection = database.open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM student");
        } catch (SQLException e) {
            throw translate("clear the student table", e);
        }
    }

    // ------------------------------------------------------------- plumbing

    private List<Student> queryList(String sql) {
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            return readAll(rows);
        } catch (SQLException e) {
            throw translate("read students", e);
        }
    }

    private Optional<Student> readOne(PreparedStatement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery()) {
            return rows.next() ? Optional.of(read(rows)) : Optional.<Student>empty();
        }
    }

    private static List<Student> readAll(ResultSet rows) throws SQLException {
        List<Student> students = new ArrayList<Student>();
        while (rows.next()) {
            students.add(read(rows));
        }
        return students;
    }

    /** Maps the current row to a {@link Student}. Column order matches {@link #COLUMNS}. */
    private static Student read(ResultSet rows) throws SQLException {
        String dob = rows.getString("date_of_birth");
        return Student.builder()
                .id(rows.getInt("id"))
                .rollNo(rows.getString("roll_no"))
                .name(rows.getString("name"))
                .email(rows.getString("email"))
                .phone(rows.getString("phone"))
                .department(Department.fromToken(rows.getString("department")))
                .semester(rows.getInt("semester"))
                .cgpa(rows.getDouble("cgpa"))
                .gender(Gender.fromToken(rows.getString("gender")))
                .dateOfBirth(dob == null || dob.isEmpty() ? null : LocalDate.parse(dob, STORAGE_DATE))
                .address(rows.getString("address") == null ? "" : rows.getString("address"))
                .build();
    }

    /**
     * Binds the ten value columns in the order both INSERT and UPDATE declare.
     *
     * <p>The date is bound as an ISO string rather than a {@code java.sql.Date}
     * on purpose: {@code java.sql.Date} carries the JVM's default time zone and
     * can shift a date of birth by a day across a DST boundary. MySQL parses
     * {@code yyyy-MM-dd} into a DATE column and returns the same text.</p>
     *
     * @return the index of the next free parameter
     */
    private static int bindStudent(PreparedStatement statement, Student student) throws SQLException {
        int i = 1;
        statement.setString(i++, student.getRollNo());
        statement.setString(i++, student.getName());
        statement.setString(i++, student.getEmail());
        statement.setString(i++, student.getPhone());
        statement.setString(i++, student.getDepartment().code());
        statement.setInt(i++, student.getSemester());
        statement.setDouble(i++, student.getCgpa());
        statement.setString(i++, student.getGender().name());
        statement.setString(i++, student.getDateOfBirth() == null
                ? null : STORAGE_DATE.format(student.getDateOfBirth()));
        statement.setString(i++, student.getAddress() == null ? "" : student.getAddress());
        return i;
    }

    private static void bindAll(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    private static int generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new DataAccessException("The database did not return a generated id.");
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // The original failure is the one worth reporting.
            }
        }
    }

    private static void restoreAutoCommitAndClose(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Closing next anyway.
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Nothing useful to do.
        }
    }

    /** Turns a vendor {@link SQLException} into a sentence the user can act on. */
    private DataAccessException translate(String action, SQLException e) {
        if (database.dialect().isUniqueViolation(e)) {
            return new DataAccessException("Could not " + action
                    + ": that roll number or email is already registered.", e);
        }
        return new DataAccessException("Could not " + action + ": " + e.getMessage(), e);
    }
}
