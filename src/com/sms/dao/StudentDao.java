package com.sms.dao;

import com.sms.model.Department;
import com.sms.model.SearchQuery;
import com.sms.model.Student;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistence contract for {@link Student} records.
 *
 * <p>The service layer talks only to this interface, so the SQLite/MySQL
 * implementation can be exchanged for the CSV one (or for a REST client, or a
 * test double) without touching a line of business or UI code. Both shipped
 * implementations are exercised by the same tests, which is what keeps the two
 * honest about behaving identically.</p>
 */
public interface StudentDao {

    /** @return every stored student, ordered by id. Never {@code null}. */
    List<Student> findAll();

    /**
     * Applies every active filter of {@code query}.
     *
     * <p>The SQL implementation turns this into a parameterised WHERE clause so
     * the database does the work; the CSV implementation filters in memory.
     * Both produce the same rows.</p>
     *
     * @param query filters to apply; {@code null} means "everything"
     */
    List<Student> search(SearchQuery query);

    Optional<Student> findById(int id);

    /** Lookup by the natural business key; matching is case-insensitive. */
    Optional<Student> findByRollNo(String rollNo);

    /** Lookup by email; matching is case-insensitive. */
    Optional<Student> findByEmail(String email);

    /**
     * Stores a brand new record.
     *
     * @param student a student whose id is {@link Student#UNSAVED_ID}
     * @return the stored student, carrying the id the database generated
     * @throws DataAccessException if the roll number or email is already taken
     */
    Student insert(Student student);

    /**
     * Overwrites an existing record.
     *
     * @throws DataAccessException if no row carries that id
     */
    Student update(Student student);

    /** @return {@code true} if a row was actually removed. */
    boolean deleteById(int id);

    /**
     * Removes several rows as one unit of work: either every id given is gone,
     * or the database is left exactly as it was.
     *
     * @return how many rows were removed
     */
    int deleteMany(Collection<Integer> ids);

    /** Empties the table. Used by tests and by "start fresh". */
    void deleteAll();

    int count();

    /** @return head count per department; departments with nobody are absent. */
    Map<Department, Integer> countByDepartment();

    /** @return the mean CGPA, or {@code 0.0} when there are no students. */
    double averageCgpa();

    /** @return the highest scoring student, or empty when there are none. */
    Optional<Student> findTopper();
}
