package com.studytracker.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Configuration class for Google Calendar API integration.
 * Sets up the necessary beans for Google Calendar API client and OAuth flow.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleCalendarConfig {
    
    private final GoogleOAuthProperties oAuthProperties;
    private final GoogleCalendarProperties calendarProperties;
    
    /**
     * JSON factory for Google API client
     */
    @Bean
    public JsonFactory jsonFactory() {
        return GsonFactory.getDefaultInstance();
    }
    
    /**
     * HTTP transport for Google API client
     */
    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }
    
    /**
     * Google Calendar service builder factory.
     * This creates a builder that can be used to create Calendar service instances
     * with different credentials for different users.
     */
    @Bean
    public Calendar.Builder calendarBuilder(HttpTransport httpTransport, JsonFactory jsonFactory) {
        return new Calendar.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(calendarProperties.getApplicationName());
    }
}