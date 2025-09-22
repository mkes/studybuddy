package com.studytracker.exception;

/**
 * Exception thrown when Canvas API token is invalid or expired.
 * Corresponds to HTTP 401 Unauthorized responses from Canvas API.
 */
public class InvalidTokenException extends CanvasApiException {

    public InvalidTokenException(String message) {
        super(message, 401);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause, 401);
    }
}