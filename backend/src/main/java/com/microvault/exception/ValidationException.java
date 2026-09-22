package com.microvault.exception;

/**
 * Thrown by the service layer when the incoming data breaks a business rule,
 * for example a negative amount or an empty category.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
