package com.microvault.auth.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;

    public static ErrorResponse of(int status, String message, String path) {
        ErrorResponse error = new ErrorResponse();
        error.timestamp = LocalDateTime.now();
        error.status = status;
        error.message = message;
        error.path = path;
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
