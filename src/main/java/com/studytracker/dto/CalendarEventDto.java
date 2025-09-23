package com.studytracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Google Calendar event data.
 * Used to transfer calendar event information between service layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto {
    
    /**
     * Event title/summary
     */
    private String summary;
    
    /**
     * Event description with assignment details
     */
    private String description;
    
    /**
     * Event start date and time
     */
    private LocalDateTime startDateTime;
    
    /**
     * Event end date and time
     */
    private LocalDateTime endDateTime;
    
    /**
     * List of reminder minutes before event
     */
    private List<Integer> reminderMinutes;
    
    /**
     * Google Calendar color ID for the event
     */
    private String colorId;
    
    /**
     * Extended properties for storing assignment metadata
     */
    private Map<String, String> extendedProperties;
    
    /**
     * Google Calendar event ID (for updates/deletes)
     */
    private String eventId;
    
    /**
     * Google Calendar ID where event belongs
     */
    private String calendarId;
}