package com.sms.model;

/** Gender options offered by the student form. */
public enum Gender {

    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Parses a gender from its name or label, ignoring case and padding.
     *
     * @throws IllegalArgumentException if nothing matches
     */
    public static Gender fromToken(String token) {
        if (token != null) {
            String needle = token.trim();
            for (Gender g : values()) {
                if (g.name().equalsIgnoreCase(needle) || g.label.equalsIgnoreCase(needle)) {
                    return g;
                }
            }
        }
        throw new IllegalArgumentException("Unknown gender: " + token);
    }

    @Override
    public String toString() {
        return label;
    }
}
