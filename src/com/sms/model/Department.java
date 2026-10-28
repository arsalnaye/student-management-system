package com.sms.model;

/**
 * Academic departments a student can belong to.
 *
 * <p>The enum carries its own human readable label so that the UI never has to
 * hard-code display strings, and {@link #fromToken(String)} lets the CSV layer
 * round-trip values safely.</p>
 */
public enum Department {

    CSE("Computer Science & Engineering"),
    IT("Information Technology"),
    ECE("Electronics & Communication"),
    EEE("Electrical & Electronics"),
    MECH("Mechanical Engineering"),
    CIVIL("Civil Engineering"),
    MBA("Business Administration");

    private final String label;

    Department(String label) {
        this.label = label;
    }

    /** @return the full, human readable department name. */
    public String label() {
        return label;
    }

    /** @return the short code used in tables and reports (e.g. {@code CSE}). */
    public String code() {
        return name();
    }

    /**
     * Parses a department from its code or its label, ignoring case and padding.
     *
     * @param token value read from a file, a combo box or user input
     * @return the matching department
     * @throws IllegalArgumentException if nothing matches
     */
    public static Department fromToken(String token) {
        if (token != null) {
            String needle = token.trim();
            for (Department d : values()) {
                if (d.name().equalsIgnoreCase(needle) || d.label.equalsIgnoreCase(needle)) {
                    return d;
                }
            }
        }
        throw new IllegalArgumentException("Unknown department: " + token);
    }

    /** Combo boxes render the label, so {@code toString} is the label. */
    @Override
    public String toString() {
        return label;
    }
}
