package com.microvault.financeprofile.exception;

/**
 * Incoming profile data breaks a rule, for example a negative amount.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
