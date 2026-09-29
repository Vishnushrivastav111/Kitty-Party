package com.microvault.financeprofile.exception;

/**
 * No active profile for the id or user that was asked for.
 */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(String message) {
        super(message);
    }
}
