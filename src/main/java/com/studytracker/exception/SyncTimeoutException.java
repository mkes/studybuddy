package com.studytracker.exception;

/**
 * Exception thrown when calendar synchronization operations timeout.
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