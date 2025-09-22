package com.studytracker.exception;

/**
 * Base exception for Canvas API related errors.
 * Provides a common parent for all Canvas API specific exceptions.
 */
public class CanvasApiException extends RuntimeException {

    private final int statusCode;

    public CanvasApiException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public CanvasApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CanvasApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public CanvasApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}