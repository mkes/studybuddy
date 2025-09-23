package com.studytracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Google Calendar API integration.
 * Binds properties from application.yml under the 'google.calendar' prefix.
 */
@Data
@Component
@ConfigurationProperties(prefix = "google.calendar")
public class GoogleCalendarProperties {
    
    /**
     * Application name to identify the application to Google Calendar API
     */
    private String applicationName;
}