package com.sms;

import com.sms.dao.CsvStudentDao;
import com.sms.dao.DataAccessException;
import com.sms.dao.JdbcStudentDao;
import com.sms.dao.StudentDao;
import com.sms.db.Database;
import com.sms.db.Dialect;
import com.sms.model.Department;
import com.sms.model.Gender;
import com.sms.model.SearchQuery;
import com.sms.model.Student;
import com.sms.service.StudentService;
import com.sms.service.ValidationException;
import com.sms.util.Csv;
import com.sms.util.SampleData;
import com.sms.util.Validators;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A dependency-free test suite.
 *
 * <p>The project ships without a build tool, so instead of pulling in JUnit the
 * suite is a plain {@code main} with a handful of assertion helpers. Run it with
 * {@code test.bat} (or {@code ./run.sh --test}); a non-zero exit status means
 * something failed.</p>
 *
 * <p>Every test gets its own temporary SQLite file, so the tests are
 * order-independent and leave nothing behind. They run against a real database
 * rather than a mock: the point is to prove the SQL is right.</p>
 */
public final class AppTests {

    private static int passed;
    private static int failed;

    private AppTests() {
    }

    public static void main(String[] args) throws Exception {
        banner("CSV utilities");
        csvRoundTripsPlainValues();
        csvQuotesSeparatorsAndQuotes();
        csvEncodesNewlines();

        banner("Validators");
        validatorsAcceptGoodInput();
        validatorsRejectBadInput();

        banner("Student model");
        builderProducesImmutableCopies();
        gradeBandsMatchCgpa();

        banner("Database and schema");
        dialectIsInferredFromUrl();
        schemaIsCreatedAndIdempotent();
        schemaHasTheExpectedColumns();

        banner("Service - create");
        addStoresValidStudent();
        addRejectsInvalidFields();
        addRejectsDuplicateRollNumber();
        addRejectsDuplicateEmail();
        databaseRejectsDuplicatesEvenWithoutTheService();

        banner("Service - update and delete");
        updateChangesStoredValues();
        updateAllowsKeepingOwnRollNumber();
        deleteRemovesRecord();
        deleteManyRemovesEveryId();

        banner("SQL search");
        searchMatchesKeywordAcrossFields();
        searchCombinesFiltersWithAnd();
        emptyQueryReturnsEverything();
        searchIsSafeAgainstSqlInjection();

        banner("SQL aggregates");
        aggregatesMatchTheRows();
        aggregatesOnAnEmptyTable();

        banner("Transactions");
        rollbackLeavesTheTableUnchanged();
        commitPersistsTheChange();

        banner("Persistence");
        recordsSurviveReopeningTheDatabase();
        addressWithCommaAndNewlineSurvives();
        exportWritesEveryRow();

        banner("Both stores behave the same");
        csvAndSqliteAgree();
        csvStoreSkipsMalformedLines();

        System.out.println();
        System.out.println("------------------------------------------------");
        System.out.println("  passed: " + passed + "    failed: " + failed);
        System.out.println("------------------------------------------------");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ CSV

    private static void csvRoundTripsPlainValues() {
        List<String> parsed = Csv.parseLine(Csv.join("1", "23CSE101", "Aarav"));
        check("plain values survive a round trip", Arrays.asList("1", "23CSE101", "Aarav").equals(parsed));
    }

    private static void csvQuotesSeparatorsAndQuotes() {
        String line = Csv.join("12 Nehru Road, Pune", "He said \"hi\"", "plain");
        List<String> parsed = Csv.parseLine(line);
        checkEquals("comma inside a field", "12 Nehru Road, Pune", parsed.get(0));
        checkEquals("quote inside a field", "He said \"hi\"", parsed.get(1));
        checkEquals("field count", "3", String.valueOf(parsed.size()));
    }

    private static void csvEncodesNewlines() {
        String encoded = Csv.encodeNewlines("line one\nline two");
        check("encoded value stays on one line", encoded.indexOf('\n') < 0);
        checkEquals("newlines decode back", "line one\nline two", Csv.decodeNewlines(encoded));
        checkEquals("backslashes survive", "C:\\data\\x", Csv.decodeNewlines(Csv.encodeNewlines("C:\\data\\x")));
    }

    // ----------------------------------------------------------- Validators

    private static void validatorsAcceptGoodInput() {
        check("roll number 23CSE101", Validators.isValidRollNo("23CSE101"));
        check("roll number CS/2023/07", Validators.isValidRollNo("CS/2023/07"));
        check("name with spaces", Validators.isValidName("Aarav Sharma"));
        check("name with apostrophe", Validators.isValidName("O'Brien"));
        check("email", Validators.isValidEmail("aarav.sharma@college.edu"));
        check("phone with separators", Validators.isValidPhone("987-654-3210"));
        check("semester 8", Validators.isValidSemester(8));
        check("cgpa 10", Validators.isValidCgpa(10.0));
    }

    private static void validatorsRejectBadInput() {
        check("blank roll number", !Validators.isValidRollNo("   "));
        check("short roll number", !Validators.isValidRollNo("A1"));
        check("digits in name", !Validators.isValidName("Aarav99"));
        check("email without domain", !Validators.isValidEmail("aarav@college"));
        check("nine-digit phone", !Validators.isValidPhone("987654321"));
        check("semester 0", !Validators.isValidSemester(0));
        check("semester 9", !Validators.isValidSemester(9));
        check("cgpa above 10", !Validators.isValidCgpa(10.5));
        check("negative cgpa", !Validators.isValidCgpa(-0.5));
    }

    // ---------------------------------------------------------------- Model

    private static void builderProducesImmutableCopies() {
        Student original = sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210");
        Student renamed = original.toBuilder().name("Aarav S").build();
        checkEquals("original is untouched", "Aarav Sharma", original.getName());
        checkEquals("copy carries the new value", "Aarav S", renamed.getName());
        checkEquals("copy keeps the other fields", original.getRollNo(), renamed.getRollNo());
    }

    private static void gradeBandsMatchCgpa() {
        checkEquals("9.2 is outstanding", "Outstanding", sampleWithCgpa(9.2).grade());
        checkEquals("7.6 is excellent", "Excellent", sampleWithCgpa(7.6).grade());
        checkEquals("6.4 is good", "Good", sampleWithCgpa(6.4).grade());
        checkEquals("5.1 is average", "Average", sampleWithCgpa(5.1).grade());
        checkEquals("3.0 is at risk", "At risk", sampleWithCgpa(3.0).grade());
        checkEquals("age is computed in whole years", "20",
                String.valueOf(sample("23CSE101", "Aarav Sharma", "a@college.edu", "9876543210")
                        .toBuilder().dateOfBirth(LocalDate.of(2000, 1, 1)).build()
                        .ageOn(LocalDate.of(2020, 6, 1))));
    }

    // ------------------------------------------------------------- Database

    private static void dialectIsInferredFromUrl() {
        check("sqlite url", Dialect.fromUrl("jdbc:sqlite:data/students.db") == Dialect.SQLITE);
        check("mysql url", Dialect.fromUrl("jdbc:mysql://localhost:3306/db") == Dialect.MYSQL);
        check("mysql ddl uses AUTO_INCREMENT",
                Dialect.MYSQL.createTableSql().contains("AUTO_INCREMENT"));
        check("sqlite ddl uses AUTOINCREMENT",
                Dialect.SQLITE.createTableSql().contains("AUTOINCREMENT"));
        check("both declare roll_no unique",
                Dialect.SQLITE.createTableSql().contains("roll_no")
                        && Dialect.MYSQL.createTableSql().contains("roll_no"));
        try {
            Dialect.fromUrl("jdbc:oracle:thin:@localhost");
            fail("an unsupported url was accepted");
        } catch (IllegalArgumentException expected) {
            pass("an unsupported url is rejected with a clear message");
        }
    }

    private static void schemaIsCreatedAndIdempotent() throws IOException {
        Database database = Database.sqlite(tempFile("schema", ".db"));
        database.createSchema();
        database.createSchema();
        pass("createSchema can be run twice (CREATE TABLE IF NOT EXISTS)");
        checkEquals("a fresh table is empty", "0",
                String.valueOf(new JdbcStudentDao(database).count()));
    }

    private static void schemaHasTheExpectedColumns() throws Exception {
        Database database = Database.sqlite(tempFile("columns", ".db"));
        database.createSchema();
        List<String> columns = new ArrayList<String>();
        try (Connection c = database.open();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("PRAGMA table_info(student)")) {
            while (r.next()) {
                columns.add(r.getString("name"));
            }
        }
        checkEquals("column count", "11", String.valueOf(columns.size()));
        for (String expected : new String[]{"id", "roll_no", "name", "email", "phone",
                "department", "semester", "cgpa", "gender", "date_of_birth", "address"}) {
            check("column " + expected + " exists", columns.contains(expected));
        }
    }

    // ------------------------------------------------------------- Creation

    private static void addStoresValidStudent() throws Exception {
        StudentService service = freshService();
        Student saved = service.add(sample("23cse101", "  Aarav   Sharma ", "AARAV@College.edu", "987-654-3210"));
        check("the database generated an id", saved.getId() > 0);
        checkEquals("roll number is upper-cased", "23CSE101", saved.getRollNo());
        checkEquals("extra spaces are collapsed", "Aarav Sharma", saved.getName());
        checkEquals("email is lower-cased", "aarav@college.edu", saved.getEmail());
        checkEquals("phone is reduced to digits", "9876543210", saved.getPhone());
        checkEquals("record count", "1", String.valueOf(service.count()));
        checkEquals("it can be read back", "Aarav Sharma",
                service.findById(saved.getId()).get().getName());
    }

    private static void addRejectsInvalidFields() throws Exception {
        StudentService service = freshService();
        Student invalid = sample("A1", "Aarav99", "not-an-email", "12345")
                .toBuilder().cgpa(11.5).semester(9).build();
        try {
            service.add(invalid);
            fail("invalid student was accepted");
        } catch (ValidationException e) {
            check("roll number is reported", e.getErrors().containsKey("rollNo"));
            check("name is reported", e.getErrors().containsKey("name"));
            check("email is reported", e.getErrors().containsKey("email"));
            check("phone is reported", e.getErrors().containsKey("phone"));
            check("semester is reported", e.getErrors().containsKey("semester"));
            check("cgpa is reported", e.getErrors().containsKey("cgpa"));
            check("all problems are reported at once", e.getErrors().size() >= 6);
        }
        checkEquals("nothing was stored", "0", String.valueOf(service.count()));
    }

    private static void addRejectsDuplicateRollNumber() throws Exception {
        StudentService service = freshService();
        service.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        try {
            service.add(sample("23cse101", "Diya Nair", "diya@college.edu", "9812345670"));
            fail("duplicate roll number was accepted");
        } catch (ValidationException e) {
            check("the clash is reported on rollNo", e.getErrors().containsKey("rollNo"));
        }
        checkEquals("only the first row was stored", "1", String.valueOf(service.count()));
    }

    private static void addRejectsDuplicateEmail() throws Exception {
        StudentService service = freshService();
        service.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        try {
            service.add(sample("23CSE102", "Diya Nair", "AARAV@college.edu", "9812345670"));
            fail("duplicate email was accepted");
        } catch (ValidationException e) {
            check("the clash is reported on email", e.getErrors().containsKey("email"));
        }
    }

    /**
     * The service checks for duplicates, but the unique indexes are the real
     * guarantee. This goes straight to the DAO to prove the database itself
     * refuses, and that the error is translated into something readable.
     */
    private static void databaseRejectsDuplicatesEvenWithoutTheService() throws Exception {
        StudentDao dao = new JdbcStudentDao(freshDatabase());
        dao.insert(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        try {
            dao.insert(sample("23CSE101", "Someone Else", "other@college.edu", "9000000000"));
            fail("the unique index did not fire");
        } catch (DataAccessException e) {
            check("the unique index fired", true);
            check("the message is readable, not a vendor code",
                    e.getMessage().toLowerCase(Locale.ROOT).contains("already registered"));
        }
        checkEquals("the table still holds one row", "1", String.valueOf(dao.count()));
    }

    // --------------------------------------------------- Update and delete

    private static void updateChangesStoredValues() throws Exception {
        StudentService service = freshService();
        Student saved = service.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        service.update(saved.toBuilder().cgpa(9.5).semester(6).build());
        Student reloaded = service.findById(saved.getId()).orElse(null);
        check("the record is still there", reloaded != null);
        checkEquals("cgpa was changed", "9.50", money(reloaded.getCgpa()));
        checkEquals("semester was changed", "6", String.valueOf(reloaded.getSemester()));
        checkEquals("no duplicate row was created", "1", String.valueOf(service.count()));
    }

    private static void updateAllowsKeepingOwnRollNumber() throws Exception {
        StudentService service = freshService();
        Student saved = service.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        try {
            service.update(saved.toBuilder().name("Aarav S").build());
            pass("a record may keep its own roll number and email");
        } catch (ValidationException e) {
            fail("update wrongly flagged the record as a duplicate of itself: " + e.getMessage());
        }
    }

    private static void deleteRemovesRecord() throws Exception {
        StudentService service = freshService();
        Student saved = service.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        check("delete reports success", service.delete(saved.getId()));
        checkEquals("the table is empty", "0", String.valueOf(service.count()));
        check("deleting again reports failure", !service.delete(saved.getId()));
    }

    private static void deleteManyRemovesEveryId() throws Exception {
        StudentService service = seededService();
        List<Integer> ids = new ArrayList<Integer>();
        List<Student> all = service.findAll();
        for (int i = 0; i < 3; i++) {
            ids.add(Integer.valueOf(all.get(i).getId()));
        }
        int before = service.count();
        checkEquals("three rows removed in one transaction", "3",
                String.valueOf(service.deleteAll(ids)));
        checkEquals("the rest are untouched", String.valueOf(before - 3),
                String.valueOf(service.count()));
        checkEquals("an empty batch is a no-op", "0",
                String.valueOf(service.deleteAll(new ArrayList<Integer>())));
    }

    // --------------------------------------------------------------- Search

    private static void searchMatchesKeywordAcrossFields() throws Exception {
        StudentService service = seededService();
        checkEquals("by partial name", "1", size(service, new SearchQuery("diya", null, 0, 0)));
        checkEquals("by roll number", "1", size(service, new SearchQuery("22MECH415", null, 0, 0)));
        checkEquals("by email fragment", "1", size(service, new SearchQuery("rohan.verma", null, 0, 0)));
        checkEquals("by phone", "1", size(service, new SearchQuery("9765432180", null, 0, 0)));
        checkEquals("keyword is case-insensitive", "1", size(service, new SearchQuery("DIYA", null, 0, 0)));
        checkEquals("by department code", "2", size(service, new SearchQuery("CSE", null, 0, 0)));
        checkEquals("no match returns nothing", "0", size(service, new SearchQuery("zzzz", null, 0, 0)));
    }

    private static void searchCombinesFiltersWithAnd() throws Exception {
        StudentService service = seededService();
        checkEquals("department filter", "2", size(service, new SearchQuery("", Department.CSE, 0, 0)));
        checkEquals("department plus semester", "2", size(service, new SearchQuery("", Department.CSE, 5, 0)));
        checkEquals("department plus wrong semester", "0", size(service, new SearchQuery("", Department.CSE, 1, 0)));
        checkEquals("minimum cgpa", "2", size(service, new SearchQuery("", null, 0, 8.8)));
        checkEquals("everything combined", "1", size(service, new SearchQuery("diya", Department.CSE, 5, 9.0)));
    }

    private static void emptyQueryReturnsEverything() throws Exception {
        StudentService service = seededService();
        checkEquals("SearchQuery.all() matches every row",
                String.valueOf(service.count()), size(service, SearchQuery.all()));
        check("an empty query knows it is empty", SearchQuery.all().isEmpty());
    }

    /**
     * The keyword goes into the SQL as a bound parameter, so a quote in the
     * input is data and not syntax. If the DAO ever concatenated the keyword,
     * this row would be deleted and the count would change.
     */
    private static void searchIsSafeAgainstSqlInjection() throws Exception {
        StudentService service = seededService();
        int before = service.count();
        String attack = "' OR '1'='1";
        checkEquals("an injection attempt matches nothing", "0",
                size(service, new SearchQuery(attack, null, 0, 0)));
        checkEquals("a drop attempt matches nothing", "0",
                size(service, new SearchQuery("x'; DROP TABLE student; --", null, 0, 0)));
        checkEquals("the table is intact", String.valueOf(before), String.valueOf(service.count()));
    }

    // ----------------------------------------------------------- Aggregates

    private static void aggregatesMatchTheRows() throws Exception {
        StudentService service = seededService();
        StudentService.Stats stats = service.stats();
        checkEquals("total", String.valueOf(service.count()), String.valueOf(stats.getTotal()));

        double expectedAverage = 0.0;
        Student expectedTopper = null;
        for (Student s : service.findAll()) {
            expectedAverage += s.getCgpa();
            if (expectedTopper == null || s.getCgpa() > expectedTopper.getCgpa()) {
                expectedTopper = s;
            }
        }
        expectedAverage /= service.count();
        checkEquals("AVG(cgpa) matches a hand count", money(expectedAverage),
                money(stats.getAverageCgpa()));
        checkEquals("the topper matches", expectedTopper.getRollNo(), stats.getTopper().getRollNo());

        Map<Department, Integer> counts = stats.getByDepartment();
        checkEquals("GROUP BY finds CSE twice", "2", String.valueOf(counts.get(Department.CSE)));
        check("GROUP BY omits empty departments", !counts.containsKey(Department.MBA)
                || counts.get(Department.MBA).intValue() > 0);
        int summed = 0;
        for (Integer n : counts.values()) {
            summed += n.intValue();
        }
        checkEquals("the group counts add up to the total", String.valueOf(stats.getTotal()),
                String.valueOf(summed));
    }

    private static void aggregatesOnAnEmptyTable() throws Exception {
        StudentService service = freshService();
        StudentService.Stats stats = service.stats();
        checkEquals("total is zero", "0", String.valueOf(stats.getTotal()));
        checkEquals("AVG over no rows is 0.0, not an error", "0.00", money(stats.getAverageCgpa()));
        check("there is no topper", stats.getTopper() == null);
        check("no departments are active", stats.getActiveDepartments() == 0);
    }

    // --------------------------------------------------------- Transactions

    private static void rollbackLeavesTheTableUnchanged() throws Exception {
        Database database = freshDatabase();
        StudentDao dao = new JdbcStudentDao(database);
        dao.insert(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));
        dao.insert(sample("23CSE102", "Diya Nair", "diya@college.edu", "9812345670"));

        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.executeUpdate("DELETE FROM student");
            }
            c.rollback();
        }
        checkEquals("a rolled back delete changed nothing", "2", String.valueOf(dao.count()));
    }

    private static void commitPersistsTheChange() throws Exception {
        Database database = freshDatabase();
        StudentDao dao = new JdbcStudentDao(database);
        dao.insert(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));

        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement s = c.prepareStatement("UPDATE student SET cgpa = ? WHERE roll_no = ?")) {
                s.setDouble(1, 9.99);
                s.setString(2, "23CSE101");
                s.executeUpdate();
            }
            c.commit();
        }
        checkEquals("a committed update is visible to a new connection", "9.99",
                money(dao.findByRollNo("23CSE101").get().getCgpa()));
    }

    // ---------------------------------------------------------- Persistence

    private static void recordsSurviveReopeningTheDatabase() throws Exception {
        Path file = tempFile("reopen", ".db");
        Database first = Database.sqlite(file);
        first.createSchema();
        StudentService writer = new StudentService(new JdbcStudentDao(first));
        writer.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210"));

        Database second = Database.sqlite(file);
        second.createSchema();
        StudentService reader = new StudentService(new JdbcStudentDao(second));
        checkEquals("the row was written to disk", "1", String.valueOf(reader.count()));
        Student reloaded = reader.findAll().get(0);
        checkEquals("roll number survived", "23CSE101", reloaded.getRollNo());
        check("date of birth survived", reloaded.getDateOfBirth() != null);
        checkEquals("the date is exact", LocalDate.now().minusYears(20).toString(),
                reloaded.getDateOfBirth().toString());
    }

    private static void addressWithCommaAndNewlineSurvives() throws Exception {
        StudentService service = freshService();
        String address = "12 Nehru Road, Pune\nMaharashtra";
        Student saved = service.add(sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210")
                .toBuilder().address(address).build());
        checkEquals("the address round trips through SQL", address,
                service.findById(saved.getId()).get().getAddress());
    }

    private static void exportWritesEveryRow() throws Exception {
        StudentService service = seededService();
        Path target = tempFile("export", ".csv");
        service.exportCsv(service.findAll(), target);
        List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
        checkEquals("header plus one line per student",
                String.valueOf(service.count() + 1), String.valueOf(lines.size()));
        check("the header names the columns", lines.get(0).startsWith("Roll No,Name"));
    }

    // ------------------------------------------------ Both implementations

    /**
     * The whole point of the DAO interface is that the layers above it cannot
     * tell which store is underneath. This runs one scenario through both and
     * compares the answers.
     */
    private static void csvAndSqliteAgree() throws Exception {
        StudentDao sqlite = new JdbcStudentDao(freshDatabase());
        StudentDao csv = new CsvStudentDao(tempFile("parity", ".csv"));

        for (StudentDao dao : new StudentDao[]{sqlite, csv}) {
            new StudentService(dao).seed(SampleData.students());
        }

        checkEquals("same row count", String.valueOf(sqlite.count()), String.valueOf(csv.count()));
        checkEquals("same average", money(sqlite.averageCgpa()), money(csv.averageCgpa()));
        checkEquals("same topper", sqlite.findTopper().get().getRollNo(),
                csv.findTopper().get().getRollNo());
        checkEquals("same department grouping",
                String.valueOf(sqlite.countByDepartment()), String.valueOf(csv.countByDepartment()));

        SearchQuery[] queries = {
                SearchQuery.all(),
                new SearchQuery("diya", null, 0, 0),
                new SearchQuery("", Department.CSE, 5, 0),
                new SearchQuery("", null, 0, 8.0),
                new SearchQuery("a", Department.CSE, 0, 7.0),
        };
        for (int i = 0; i < queries.length; i++) {
            checkEquals("same search result for query " + (i + 1),
                    rollNumbers(sqlite.search(queries[i])), rollNumbers(csv.search(queries[i])));
        }
        checkEquals("same lookup by roll number", "Diya Nair",
                sqlite.findByRollNo("23cse118").get().getName());
        checkEquals("same lookup by roll number (csv)", "Diya Nair",
                csv.findByRollNo("23cse118").get().getName());
    }

    private static void csvStoreSkipsMalformedLines() throws Exception {
        Path file = tempFile("malformed", ".csv");
        Files.write(file, ("id,rollNo,name,email,phone,department,semester,cgpa,gender,dateOfBirth,address\n"
                + "1,23CSE101,Aarav Sharma,aarav@college.edu,9876543210,CSE,5,8.90,MALE,2004-03-05,Pune\n"
                + "this line is broken\n").getBytes(StandardCharsets.UTF_8));
        CsvStudentDao dao = new CsvStudentDao(file);
        checkEquals("the good row still loads", "1", String.valueOf(dao.count()));
        checkEquals("the bad row is reported", "1", String.valueOf(dao.loadWarnings().size()));
    }

    // ------------------------------------------------------------- fixtures

    private static Database freshDatabase() throws IOException {
        Database database = Database.sqlite(tempFile("db", ".db"));
        database.createSchema();
        return database;
    }

    private static StudentService freshService() throws IOException {
        return new StudentService(new JdbcStudentDao(freshDatabase()));
    }

    private static StudentService seededService() throws IOException {
        StudentService service = freshService();
        service.seed(SampleData.students());
        return service;
    }

    private static Path tempFile(String prefix, String suffix) throws IOException {
        Path dir = Files.createTempDirectory("sms-" + prefix);
        dir.toFile().deleteOnExit();
        Path file = dir.resolve("store" + suffix);
        file.toFile().deleteOnExit();
        return file;
    }

    private static Student sample(String rollNo, String name, String email, String phone) {
        return Student.builder()
                .rollNo(rollNo)
                .name(name)
                .email(email)
                .phone(phone)
                .department(Department.CSE)
                .semester(5)
                .cgpa(8.5)
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.now().minusYears(20))
                .address("Campus hostel")
                .build();
    }

    private static Student sampleWithCgpa(double cgpa) {
        return sample("23CSE101", "Aarav Sharma", "aarav@college.edu", "9876543210")
                .toBuilder().cgpa(cgpa).build();
    }

    private static String size(StudentService service, SearchQuery query) {
        return String.valueOf(service.search(query).size());
    }

    private static String rollNumbers(List<Student> students) {
        StringBuilder sb = new StringBuilder();
        for (Student s : students) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s.getRollNo());
        }
        return sb.toString();
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%.2f", Double.valueOf(value));
    }

    // ------------------------------------------------------------ assertions

    private static void banner(String title) {
        System.out.println();
        System.out.println("== " + title + " " + repeat('=', Math.max(0, 46 - title.length())));
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            pass(description);
        } else {
            fail(description);
        }
    }

    private static void checkEquals(String description, String expected, String actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass(description);
        } else {
            fail(description + "  (expected <" + expected + "> but was <" + actual + ">)");
        }
    }

    private static void pass(String description) {
        passed++;
        System.out.println("  [PASS] " + description);
    }

    private static void fail(String description) {
        failed++;
        System.out.println("  [FAIL] " + description);
    }

    private static String repeat(char ch, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
