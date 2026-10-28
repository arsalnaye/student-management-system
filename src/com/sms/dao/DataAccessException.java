package com.sms.dao;

/**
 * Thrown when the storage layer cannot read or write records.
 *
 * <p>Unchecked on purpose: a broken data file is not something the calling code
 * can meaningfully recover from field-by-field, so it travels up to the single
 * handler in the UI that reports it to the user.</p>
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
