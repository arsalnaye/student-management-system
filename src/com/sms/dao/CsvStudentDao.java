package com.sms.dao;

import com.sms.model.Department;
import com.sms.model.Gender;
import com.sms.model.SearchQuery;
import com.sms.model.Student;
import com.sms.util.Csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * File-backed {@link StudentDao}: the alternative to the SQL store.
 *
 * <p>The whole table is held in a {@link LinkedHashMap} for O(1) lookups and
 * stable ordering, and mirrored to a UTF-8 CSV file after every mutation. The
 * write goes to a sibling temp file which is then moved over the original, so a
 * crash mid-write can never leave a half-written data file behind.</p>
 *
 * <p>This class is not decoration. It is the evidence that the {@link StudentDao}
 * interface really does decouple the layers above it: the service, the
 * validation, the dashboard and every Swing panel run unchanged on top of it,
 * and the test suite pushes one scenario through both this and
 * {@link JdbcStudentDao} and asserts the answers are identical. If a SQL WHERE
 * clause and the in-memory rules ever drift apart, that test fails.</p>
 *
 * <p>Select it at runtime with {@code run.bat --csv data\students.csv}.</p>
 */
public class CsvStudentDao implements StudentDao {

    private static final String HEADER =
            "id,rollNo,name,email,phone,department,semester,cgpa,gender,dateOfBirth,address";
    private static final int FIELD_COUNT = 11;
    private static final DateTimeFormatter STORAGE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Path file;
    private final Map<Integer, Student> store = new LinkedHashMap<Integer, Student>();
    private final List<String> loadWarnings = new ArrayList<String>();
    private int sequence = Student.UNSAVED_ID;

    /**
     * @param file CSV file to use; it is created on first write if missing
     */
    public CsvStudentDao(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("data file path is required");
        }
        this.file = file;
        load();
    }

    /** @return the data file this DAO reads and writes. */
    public Path file() {
        return file;
    }

    /**
     * @return messages for lines that could not be parsed during load; the rest
     *         of the file is still usable. Empty when everything loaded cleanly.
     */
    public List<String> loadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
    }

    @Override
    public List<Student> findAll() {
        return new ArrayList<Student>(store.values());
    }

    /** Applies the same rules the SQL store applies, in memory. */
    @Override
    public List<Student> search(SearchQuery query) {
        if (query == null || query.isEmpty()) {
            return findAll();
        }
        List<Student> matches = new ArrayList<Student>();
        for (Student s : store.values()) {
            if (query.matches(s)) {
                matches.add(s);
            }
        }
        return matches;
    }

    @Override
    public Optional<Student> findById(int id) {
        return Optional.ofNullable(store.get(Integer.valueOf(id)));
    }

    @Override
    public Optional<Student> findByRollNo(String rollNo) {
        if (rollNo == null) {
            return Optional.empty();
        }
        String needle = rollNo.trim();
        for (Student s : store.values()) {
            if (s.getRollNo().equalsIgnoreCase(needle)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String needle = email.trim();
        for (Student s : store.values()) {
            if (s.getEmail().equalsIgnoreCase(needle)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public Student insert(Student student) {
        Student saved = student.toBuilder().id(++sequence).build();
        store.put(Integer.valueOf(saved.getId()), saved);
        flush();
        return saved;
    }

    @Override
    public Student update(Student student) {
        Integer key = Integer.valueOf(student.getId());
        if (!store.containsKey(key)) {
            throw new DataAccessException("No student exists with id " + student.getId());
        }
        store.put(key, student);
        flush();
        return student;
    }

    @Override
    public boolean deleteById(int id) {
        boolean removed = store.remove(Integer.valueOf(id)) != null;
        if (removed) {
            flush();
        }
        return removed;
    }

    /**
     * Removes a batch, writing the file once at the end.
     *
     * <p>The single {@link #flush()} is this store's equivalent of a
     * transaction: the map is updated first and the file is replaced in one
     * atomic move, so an observer sees either the old table or the new one.</p>
     */
    @Override
    public int deleteMany(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (Integer id : ids) {
            if (id != null && store.remove(id) != null) {
                removed++;
            }
        }
        if (removed > 0) {
            flush();
        }
        return removed;
    }

    @Override
    public void deleteAll() {
        if (!store.isEmpty()) {
            store.clear();
            flush();
        }
    }

    @Override
    public int count() {
        return store.size();
    }

    @Override
    public Map<Department, Integer> countByDepartment() {
        Map<Department, Integer> counts = new EnumMap<Department, Integer>(Department.class);
        for (Student s : store.values()) {
            Integer previous = counts.get(s.getDepartment());
            counts.put(s.getDepartment(), Integer.valueOf(previous == null ? 1 : previous.intValue() + 1));
        }
        return counts;
    }

    @Override
    public double averageCgpa() {
        if (store.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Student s : store.values()) {
            total += s.getCgpa();
        }
        return total / store.size();
    }

    @Override
    public Optional<Student> findTopper() {
        Student best = null;
        for (Student s : store.values()) {
            if (best == null || s.getCgpa() > best.getCgpa()) {
                best = s;
            }
        }
        return Optional.ofNullable(best);
    }

    // ------------------------------------------------------------------ I/O

    private void load() {
        store.clear();
        loadWarnings.clear();
        if (!Files.exists(file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + file + ": " + e.getMessage(), e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            if (i == 0 && line.startsWith("id,")) {
                continue; // header
            }
            try {
                Student student = fromCsv(line);
                store.put(Integer.valueOf(student.getId()), student);
                sequence = Math.max(sequence, student.getId());
            } catch (RuntimeException e) {
                loadWarnings.add("Line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
    }

    /** Writes the whole table atomically. Called after every mutation. */
    private void flush() {
        StringBuilder out = new StringBuilder(HEADER).append(System.lineSeparator());
        for (Student s : store.values()) {
            out.append(toCsv(s)).append(System.lineSeparator());
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = (parent == null ? file.toAbsolutePath() : parent.resolve(file.getFileName() + ".tmp"));
            Files.write(temp, out.toString().getBytes(StandardCharsets.UTF_8));
            moveIntoPlace(temp, file);
        } catch (IOException e) {
            throw new DataAccessException("Could not save to " + file + ": " + e.getMessage(), e);
        }
    }

    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ------------------------------------------------------------- mapping

    static String toCsv(Student s) {
        return Csv.join(
                String.valueOf(s.getId()),
                s.getRollNo(),
                s.getName(),
                s.getEmail(),
                s.getPhone(),
                s.getDepartment().code(),
                String.valueOf(s.getSemester()),
                String.format(java.util.Locale.ROOT, "%.2f", Double.valueOf(s.getCgpa())),
                s.getGender().name(),
                s.getDateOfBirth() == null ? "" : STORAGE_DATE.format(s.getDateOfBirth()),
                Csv.encodeNewlines(s.getAddress()));
    }

    static Student fromCsv(String line) {
        List<String> f = Csv.parseLine(line);
        if (f.size() < FIELD_COUNT) {
            throw new IllegalArgumentException("expected " + FIELD_COUNT + " columns but found " + f.size());
        }
        String dob = f.get(9).trim();
        return Student.builder()
                .id(Integer.parseInt(f.get(0).trim()))
                .rollNo(f.get(1).trim())
                .name(f.get(2).trim())
                .email(f.get(3).trim())
                .phone(f.get(4).trim())
                .department(Department.fromToken(f.get(5)))
                .semester(Integer.parseInt(f.get(6).trim()))
                .cgpa(Double.parseDouble(f.get(7).trim()))
                .gender(Gender.fromToken(f.get(8)))
                .dateOfBirth(dob.isEmpty() ? null : LocalDate.parse(dob, STORAGE_DATE))
                .address(Csv.decodeNewlines(f.get(10)))
                .build();
    }
}
