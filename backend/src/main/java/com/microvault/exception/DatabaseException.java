package com.microvault.exception;

/**
 * Thrown by DAO implementations when a JDBC call fails. It wraps the original
 * SQLException so the cause is not lost.
 */
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
