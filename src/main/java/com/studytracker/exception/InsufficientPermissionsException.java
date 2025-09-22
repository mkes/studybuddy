package com.studytracker.exception;

/**
 * Exception thrown when Canvas API token lacks required observer permissions.
 * Corresponds to HTTP 403 Forbidden responses from Canvas API.
 */
public class InsufficientPermissionsException extends CanvasApiException {

    public InsufficientPermissionsException(String message) {
        super(message, 403);
    }

    public InsufficientPermissionsException(String message, Throwable cause) {
        super(message, cause, 403);
    }
}