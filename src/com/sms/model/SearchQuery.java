package com.sms.model;

import com.sms.util.Validators;

import java.util.Locale;

/**
 * An immutable set of search filters.
 *
 * <p>All filters combine with AND; an unset filter matches everything.</p>
 *
 * <p>This lives in the model package rather than in the service package on
 * purpose. The data-access layer accepts a {@code SearchQuery} and turns it into
 * a WHERE clause, so if the type lived in {@code service} the dependency arrow
 * between service and dao would point both ways. As a model type it is shared by
 * both layers without creating a cycle.</p>
 *
 * <p>{@link #matches(Student)} exists so that a non-SQL store can apply exactly
 * the same rules the SQL store applies, and so the rules can be unit tested
 * without a database.</p>
 */
public final class SearchQuery {

    /** Sentinel meaning "any semester". */
    public static final int ANY_SEMESTER = 0;

    private static final SearchQuery EMPTY = new SearchQuery("", null, ANY_SEMESTER, 0.0);

    private final String keyword;
    private final Department department;
    private final int semester;
    private final double minCgpa;

    public SearchQuery(String keyword, Department department, int semester, double minCgpa) {
        this.keyword = Validators.trimmed(keyword);
        this.department = department;
        this.semester = semester;
        this.minCgpa = minCgpa;
    }

    /** @return a query that matches every student. */
    public static SearchQuery all() {
        return EMPTY;
    }

    public String getKeyword() {
        return keyword;
    }

    public Department getDepartment() {
        return department;
    }

    public int getSemester() {
        return semester;
    }

    public double getMinCgpa() {
        return minCgpa;
    }

    /** @return {@code true} when no filter is active. */
    public boolean isEmpty() {
        return keyword.isEmpty() && department == null && semester == ANY_SEMESTER && minCgpa <= 0.0;
    }

    public boolean hasKeyword() {
        return !keyword.isEmpty();
    }

    public boolean hasMinCgpa() {
        return minCgpa > 0.0;
    }

    /** @return {@code true} when {@code student} satisfies every active filter. */
    public boolean matches(Student student) {
        if (student == null) {
            return false;
        }
        if (department != null && student.getDepartment() != department) {
            return false;
        }
        if (semester != ANY_SEMESTER && student.getSemester() != semester) {
            return false;
        }
        if (hasMinCgpa() && student.getCgpa() < minCgpa) {
            return false;
        }
        return matchesKeyword(student);
    }

    private boolean matchesKeyword(Student student) {
        if (keyword.isEmpty()) {
            return true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        return contains(student.getRollNo(), needle)
                || contains(student.getName(), needle)
                || contains(student.getEmail(), needle)
                || contains(student.getPhone(), needle)
                || contains(student.getDepartment().code(), needle)
                || contains(student.getDepartment().label(), needle);
    }

    private static boolean contains(String haystack, String lowercaseNeedle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(lowercaseNeedle);
    }

    /** @return a short human readable summary, used by the status bar. */
    public String describe() {
        if (isEmpty()) {
            return "All students";
        }
        StringBuilder sb = new StringBuilder();
        if (!keyword.isEmpty()) {
            sb.append("matching \"").append(keyword).append('"');
        }
        if (department != null) {
            append(sb, "in " + department.code());
        }
        if (semester != ANY_SEMESTER) {
            append(sb, "semester " + semester);
        }
        if (hasMinCgpa()) {
            append(sb, String.format(Locale.ROOT, "CGPA >= %.1f", Double.valueOf(minCgpa)));
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(part);
    }

    @Override
    public String toString() {
        return "SearchQuery[" + describe() + ']';
    }
}
