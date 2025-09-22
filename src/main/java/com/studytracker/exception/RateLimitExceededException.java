package com.studytracker.exception;

/**
 * Exception thrown when Canvas API rate limit is exceeded.
 * Corresponds to HTTP 429 Too Many Requests responses from Canvas API.
 */
public class RateLimitExceededException extends CanvasApiException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message, 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, Throwable cause, long retryAfterSeconds) {
        super(message, cause, 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}