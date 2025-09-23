package com.studytracker.config;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for Google Calendar configuration.
 * Verifies that all Google Calendar API beans are properly configured.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "google.oauth.client-id=test-client-id",
    "google.oauth.client-secret=test-client-secret",
    "google.oauth.redirect-uri=http://localhost:8080/auth/google/callback",
    "google.oauth.scopes[0]=https://www.googleapis.com/auth/calendar",
    "google.oauth.scopes[1]=https://www.googleapis.com/auth/calendar.events",
    "google.calendar.application-name=StudyTracker Test"
})
class GoogleCalendarConfigTest {
    
    @Autowired
    private GoogleOAuthProperties oAuthProperties;
    
    @Autowired
    private GoogleCalendarProperties calendarProperties;
    
    @Autowired
    private JsonFactory jsonFactory;
    
    @Autowired
    private HttpTransport httpTransport;
    
    @Autowired
    private Calendar.Builder calendarBuilder;
    
    @Autowired
    private GoogleClientSecrets googleClientSecrets;
    
    @Autowired
    private GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
    
    @Test
    void shouldLoadGoogleOAuthProperties() {
        assertThat(oAuthProperties.getClientId()).isEqualTo("test-client-id");
        assertThat(oAuthProperties.getClientSecret()).isEqualTo("test-client-secret");
        assertThat(oAuthProperties.getRedirectUri()).isEqualTo("http://localhost:8080/auth/google/callback");
        assertThat(oAuthProperties.getScopes()).containsExactly(
            "https://www.googleapis.com/auth/calendar",
            "https://www.googleapis.com/auth/calendar.events"
        );
    }
    
    @Test
    void shouldLoadGoogleCalendarProperties() {
        assertThat(calendarProperties.getApplicationName()).isEqualTo("StudyTracker Test");
    }
    
    @Test
    void shouldCreateJsonFactory() {
        assertThat(jsonFactory).isNotNull();
    }
    
    @Test
    void shouldCreateHttpTransport() {
        assertThat(httpTransport).isNotNull();
    }
    
    @Test
    void shouldCreateCalendarBuilder() {
        assertThat(calendarBuilder).isNotNull();
        assertThat(calendarBuilder.getApplicationName()).isEqualTo("StudyTracker Test");
    }
    
    @Test
    void shouldCreateGoogleClientSecrets() {
        assertThat(googleClientSecrets).isNotNull();
        assertThat(googleClientSecrets.getInstalled().getClientId()).isEqualTo("test-client-id");
        assertThat(googleClientSecrets.getInstalled().getClientSecret()).isEqualTo("test-client-secret");
    }
    
    @Test
    void shouldCreateGoogleAuthorizationCodeFlow() {
        assertThat(googleAuthorizationCodeFlow).isNotNull();
        assertThat(googleAuthorizationCodeFlow.getScopes()).containsExactly(
            "https://www.googleapis.com/auth/calendar",
            "https://www.googleapis.com/auth/calendar.events"
        );
    }
}