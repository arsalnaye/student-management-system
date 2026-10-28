package com.sms.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raised when a student record fails business validation.
 *
 * <p>Carries a field-keyed map (e.g. {@code "email" -> "Email looks invalid."})
 * so the form can highlight exactly the inputs that need attention instead of
 * showing one generic message. Checked on purpose: every caller must decide how
 * to surface the problem.</p>
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(buildMessage(errors));
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<String, String>(errors));
    }

    public ValidationException(String field, String message) {
        this(single(field, message));
    }

    /** @return field name to message, in the order the fields appear on the form. */
    public Map<String, String> getErrors() {
        return errors;
    }

    private static Map<String, String> single(String field, String message) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(field, message);
        return map;
    }

    private static String buildMessage(Map<String, String> errors) {
        StringBuilder sb = new StringBuilder();
        for (String message : errors.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(message);
        }
        return sb.length() == 0 ? "Invalid student record." : sb.toString();
    }
}
