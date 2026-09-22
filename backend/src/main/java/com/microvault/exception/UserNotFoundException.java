package com.microvault.exception;

import java.util.UUID;

/**
 * Thrown when a user id or email does not match any active user.
 */
public class UserNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(UUID userId) {
        super("No active user found with id " + userId);
    }
}
