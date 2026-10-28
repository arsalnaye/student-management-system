package com.sms.util;

import java.util.regex.Pattern;

/**
 * Reusable field-level validation primitives.
 *
 * <p>Kept free of any UI or persistence dependency so the same rules can be used
 * by the service layer, by importers and by the unit tests.</p>
 */
public final class Validators {

    /** 4-20 characters: letters, digits, dash or slash (e.g. {@code 23CSE101}, {@code CS/2023/07}). */
    public static final Pattern ROLL_NO = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9/-]{3,19}$");

    /** Letters plus space, dot, apostrophe and hyphen; 2-50 characters. */
    public static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,49}$");

    /** Pragmatic e-mail shape check: {@code local@domain.tld}. */
    public static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Exactly ten digits, optionally written with spaces or dashes. */
    public static final Pattern PHONE = Pattern.compile("^\\d{10}$");

    public static final int MIN_SEMESTER = 1;
    public static final int MAX_SEMESTER = 8;
    public static final double MIN_CGPA = 0.0;
    public static final double MAX_CGPA = 10.0;
    public static final int MIN_AGE = 14;
    public static final int MAX_AGE = 80;

    private Validators() {
    }

    /** @return {@code true} when {@code value} is null, empty or only whitespace. */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** @return {@code value} trimmed, or {@code ""} when it is null. */
    public static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    /** Collapses runs of whitespace so {@code "Ravi   Kumar"} becomes {@code "Ravi Kumar"}. */
    public static String normalizeSpaces(String value) {
        return trimmed(value).replaceAll("\\s+", " ");
    }

    /** Strips spaces, dashes and brackets from a phone number before checking it. */
    public static String normalizePhone(String value) {
        return trimmed(value).replaceAll("[\\s()-]", "");
    }

    public static boolean isValidRollNo(String value) {
        return !isBlank(value) && ROLL_NO.matcher(value.trim()).matches();
    }

    public static boolean isValidName(String value) {
        return !isBlank(value) && NAME.matcher(value.trim()).matches();
    }

    public static boolean isValidEmail(String value) {
        return !isBlank(value) && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isValidPhone(String value) {
        return !isBlank(value) && PHONE.matcher(normalizePhone(value)).matches();
    }

    public static boolean isValidSemester(int semester) {
        return semester >= MIN_SEMESTER && semester <= MAX_SEMESTER;
    }

    public static boolean isValidCgpa(double cgpa) {
        return !Double.isNaN(cgpa) && cgpa >= MIN_CGPA && cgpa <= MAX_CGPA;
    }
}
