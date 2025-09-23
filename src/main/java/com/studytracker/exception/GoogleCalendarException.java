package com.studytracker.exception;

/**
 * Exception thrown when Google Calendar API operations fail.
 * This is a general exception for calendar-related errors.
 */
public class GoogleCalendarException extends RuntimeException {
    
    public GoogleCalendarException(String message) {
        super(message);
    }
    
    public GoogleCalendarException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GoogleCalendarException(Throwable cause) {
        super(cause);
    }
}