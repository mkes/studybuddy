package com.studytracker.exception;

/**
 * Exception thrown when Canvas API is unavailable or experiencing downtime.
 * Corresponds to HTTP 502/503 responses from Canvas API.
 */
public class CanvasUnavailableException extends CanvasApiException {

    public CanvasUnavailableException(String message) {
        super(message, 502);
    }

    public CanvasUnavailableException(String message, Throwable cause) {
        super(message, cause, 502);
    }
}