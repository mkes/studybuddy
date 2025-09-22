package com.studytracker.exception;

/**
 * Exception thrown when sync operation exceeds the timeout limit.
 * Corresponds to HTTP 503 Service Unavailable when sync takes too long.
 */
public class SyncTimeoutException extends RuntimeException {

    private final long timeoutSeconds;

    public SyncTimeoutException(String message, long timeoutSeconds) {
        super(message);
        this.timeoutSeconds = timeoutSeconds;
    }

    public SyncTimeoutException(String message, Throwable cause, long timeoutSeconds) {
        super(message, cause);
        this.timeoutSeconds = timeoutSeconds;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}