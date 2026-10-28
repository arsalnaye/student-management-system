package com.sms.service;

import com.sms.dao.StudentDao;
import com.sms.model.Department;
import com.sms.model.SearchQuery;
import com.sms.model.Student;
import com.sms.util.Csv;
import com.sms.util.Validators;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All student business rules live here: validation, uniqueness, search and
 * reporting.
 *
 * <p>This is the only class the UI talks to. It never imports a Swing type, and
 * the Swing layer never imports a DAO type - that one rule is what keeps the
 * three layers genuinely separate.</p>
 */
public class StudentService {

    private static final DateTimeFormatter EXPORT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final StudentDao dao;

    public StudentService(StudentDao dao) {
        if (dao == null) {
            throw new IllegalArgumentException("dao is required");
        }
        this.dao = dao;
    }

    // ------------------------------------------------------------------ CRUD

    /** @return every student, newest last. */
    public List<Student> findAll() {
        return dao.findAll();
    }

    public Optional<Student> findById(int id) {
        return dao.findById(id);
    }

    /**
     * Validates and stores a new student.
     *
     * @param student a record with {@link Student#UNSAVED_ID}
     * @return the stored record, carrying its generated id
     * @throws ValidationException if any field is invalid or a key is taken
     */
    public Student add(Student student) throws ValidationException {
        Student clean = normalize(student).toBuilder().id(Student.UNSAVED_ID).build();
        validate(clean);
        return dao.insert(clean);
    }

    /**
     * Validates and overwrites an existing student.
     *
     * @throws ValidationException if a field is invalid, a key is taken, or the
     *                             record no longer exists
     */
    public Student update(Student student) throws ValidationException {
        if (student.isNew()) {
            throw new ValidationException("form", "Select a student from the table before updating.");
        }
        if (!dao.findById(student.getId()).isPresent()) {
            throw new ValidationException("form", "That student no longer exists. Refresh and try again.");
        }
        Student clean = normalize(student);
        validate(clean);
        return dao.update(clean);
    }

    /** @return {@code true} if a record was removed. */
    public boolean delete(int id) {
        return dao.deleteById(id);
    }

    /**
     * Removes several records as one unit of work.
     *
     * @return how many rows were removed
     */
    public int deleteAll(Collection<Integer> ids) {
        return dao.deleteMany(ids);
    }

    public int count() {
        return dao.count();
    }

    // ---------------------------------------------------------------- Search

    /**
     * Applies every active filter of {@code query}.
     *
     * <p>Delegated to the store rather than filtered here, so the SQL
     * implementation can push the work into a WHERE clause instead of loading
     * the whole table into memory.</p>
     *
     * @param query filters to apply; {@code null} means "everything"
     * @return matching students, ordered by id
     */
    public List<Student> search(SearchQuery query) {
        return dao.search(query == null ? SearchQuery.all() : query);
    }

    // ------------------------------------------------------------ Reporting

    /**
     * @return aggregate figures for the dashboard tiles
     *
     * <p>Each figure is a single aggregate query against the store (COUNT,
     * GROUP BY, AVG, ORDER BY with a limit) rather than a scan in Java, so the
     * dashboard stays cheap as the table grows.</p>
     */
    public Stats stats() {
        return new Stats(
                dao.count(),
                dao.averageCgpa(),
                dao.countByDepartment(),
                dao.findTopper().orElse(null));
    }

    // --------------------------------------------------------------- Export

    /**
     * Writes the given students to a human friendly CSV report.
     *
     * @param students rows to write, usually the currently filtered view
     * @param target   file to create or overwrite
     * @throws IOException if the file cannot be written
     */
    public void exportCsv(List<Student> students, Path target) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.join("Roll No", "Name", "Email", "Phone", "Department",
                "Semester", "CGPA", "Grade", "Gender", "Date of Birth", "Address"));
        sb.append(System.lineSeparator());
        for (Student s : students) {
            sb.append(Csv.join(
                    s.getRollNo(),
                    s.getName(),
                    s.getEmail(),
                    s.getPhone(),
                    s.getDepartment().label(),
                    String.valueOf(s.getSemester()),
                    String.format(java.util.Locale.ROOT, "%.2f", Double.valueOf(s.getCgpa())),
                    s.grade(),
                    s.getGender().label(),
                    s.getDateOfBirth() == null ? "" : EXPORT_DATE.format(s.getDateOfBirth()),
                    Csv.encodeNewlines(s.getAddress())));
            sb.append(System.lineSeparator());
        }
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Adds records that are not already present. Used to seed a fresh install. */
    public int seed(List<Student> samples) {
        int added = 0;
        for (Student s : samples) {
            try {
                add(s);
                added++;
            } catch (ValidationException ignored) {
                // A duplicate or malformed sample is simply skipped.
            }
        }
        return added;
    }

    // ----------------------------------------------------------- Validation

    /** Trims, collapses spaces and canonicalises case so storage is consistent. */
    private static Student normalize(Student student) {
        return student.toBuilder()
                .rollNo(Validators.trimmed(student.getRollNo()).toUpperCase())
                .name(Validators.normalizeSpaces(student.getName()))
                .email(Validators.trimmed(student.getEmail()).toLowerCase())
                .phone(Validators.normalizePhone(student.getPhone()))
                .address(Validators.trimmed(student.getAddress()))
                .build();
    }

    /**
     * Runs every rule and reports <em>all</em> problems at once, so the user does
     * not have to fix one field, submit, and discover the next.
     */
    private void validate(Student s) throws ValidationException {
        Map<String, String> errors = new LinkedHashMap<String, String>();

        if (Validators.isBlank(s.getRollNo())) {
            errors.put("rollNo", "Roll number is required.");
        } else if (!Validators.isValidRollNo(s.getRollNo())) {
            errors.put("rollNo", "Roll number must be 4-20 characters (letters, digits, - or /).");
        }

        if (Validators.isBlank(s.getName())) {
            errors.put("name", "Name is required.");
        } else if (!Validators.isValidName(s.getName())) {
            errors.put("name", "Name must be 2-50 letters; spaces, . ' and - are allowed.");
        }

        if (Validators.isBlank(s.getEmail())) {
            errors.put("email", "Email is required.");
        } else if (!Validators.isValidEmail(s.getEmail())) {
            errors.put("email", "Email must look like name@college.edu.");
        }

        if (Validators.isBlank(s.getPhone())) {
            errors.put("phone", "Phone number is required.");
        } else if (!Validators.isValidPhone(s.getPhone())) {
            errors.put("phone", "Phone number must contain exactly 10 digits.");
        }

        if (s.getDepartment() == null) {
            errors.put("department", "Choose a department.");
        }

        if (!Validators.isValidSemester(s.getSemester())) {
            errors.put("semester", "Semester must be between "
                    + Validators.MIN_SEMESTER + " and " + Validators.MAX_SEMESTER + ".");
        }

        if (!Validators.isValidCgpa(s.getCgpa())) {
            errors.put("cgpa", "CGPA must be between "
                    + String.format(java.util.Locale.ROOT, "%.1f", Double.valueOf(Validators.MIN_CGPA))
                    + " and "
                    + String.format(java.util.Locale.ROOT, "%.1f", Double.valueOf(Validators.MAX_CGPA)) + ".");
        }

        validateDateOfBirth(s, errors);
        validateUniqueness(s, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateDateOfBirth(Student s, Map<String, String> errors) {
        LocalDate dob = s.getDateOfBirth();
        if (dob == null) {
            errors.put("dateOfBirth", "Date of birth is required.");
            return;
        }
        LocalDate today = LocalDate.now();
        if (!dob.isBefore(today)) {
            errors.put("dateOfBirth", "Date of birth must be in the past.");
            return;
        }
        int age = s.ageOn(today);
        if (age < Validators.MIN_AGE || age > Validators.MAX_AGE) {
            errors.put("dateOfBirth", "Age works out to " + age + " years; expected "
                    + Validators.MIN_AGE + "-" + Validators.MAX_AGE + ".");
        }
    }

    /** The roll number and e-mail are business keys and must stay unique. */
    private void validateUniqueness(Student s, Map<String, String> errors) {
        if (!errors.containsKey("rollNo")) {
            Optional<Student> clash = dao.findByRollNo(s.getRollNo());
            if (clash.isPresent() && clash.get().getId() != s.getId()) {
                errors.put("rollNo", "Roll number " + s.getRollNo()
                        + " already belongs to " + clash.get().getName() + ".");
            }
        }
        if (!errors.containsKey("email")) {
            Optional<Student> clash = dao.findByEmail(s.getEmail());
            if (clash.isPresent() && clash.get().getId() != s.getId()) {
                errors.put("email", "That email is already used by " + clash.get().getName() + ".");
            }
        }
    }

    /** Aggregate figures shown on the dashboard. */
    public static final class Stats {

        private final int total;
        private final double averageCgpa;
        private final Map<Department, Integer> byDepartment;
        private final Student topper;

        Stats(int total, double averageCgpa, Map<Department, Integer> byDepartment, Student topper) {
            this.total = total;
            this.averageCgpa = averageCgpa;
            this.byDepartment = byDepartment;
            this.topper = topper;
        }

        public int getTotal() {
            return total;
        }

        public double getAverageCgpa() {
            return averageCgpa;
        }

        /** @return head count per department; departments with nobody are absent. */
        public Map<Department, Integer> getByDepartment() {
            return byDepartment;
        }

        /** @return how many departments currently have at least one student. */
        public int getActiveDepartments() {
            return byDepartment.size();
        }

        /** @return the highest scoring student, or {@code null} when there are none. */
        public Student getTopper() {
            return topper;
        }

        /** @return the department with the largest head count, or {@code null}. */
        public Department getLargestDepartment() {
            Department largest = null;
            int best = -1;
            for (Map.Entry<Department, Integer> e : byDepartment.entrySet()) {
                if (e.getValue().intValue() > best) {
                    best = e.getValue().intValue();
                    largest = e.getKey();
                }
            }
            return largest;
        }
    }

    /** Sorts students by CGPA, highest first - handy for reports. */
    public static Comparator<Student> byCgpaDescending() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return Double.compare(b.getCgpa(), a.getCgpa());
            }
        };
    }
}
