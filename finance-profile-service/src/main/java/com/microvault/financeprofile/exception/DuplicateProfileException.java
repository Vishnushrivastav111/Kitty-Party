package com.microvault.financeprofile.exception;

/**
 * The user already has an active financial profile.
 */
public class DuplicateProfileException extends RuntimeException {

    public DuplicateProfileException(String message) {
        super(message);
    }
}
