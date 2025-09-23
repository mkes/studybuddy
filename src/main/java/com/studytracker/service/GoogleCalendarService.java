package com.studytracker.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.studytracker.config.GoogleOAuthProperties;
import com.studytracker.exception.GoogleCalendarException;
import com.studytracker.exception.RateLimitExceededException;
import com.studytracker.model.AccountType;
import com.studytracker.model.PlannerItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for Google Calendar API integration.
 * Handles OAuth 2.0 flows, calendar operations, and event management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {
    
    private final Calendar.Builder calendarBuilder;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    private final GoogleOAuthProperties oAuthProperties;
    private final CalendarTokenService tokenService;
    
    private static final String PARENT_CALENDAR_NAME_TEMPLATE = "Student Assignments - %s";
    private static final String STUDENT_CALENDAR_NAME = "My Assignments";
    private static final String CALENDAR_DESCRIPTION_TEMPLATE = "Assignments and quizzes for %s";
    private static final String EVENT_COLOR_ID = "4"; // Blue color for assignments
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    
    /**
     * Initiate OAuth 2.0 flow for calendar access
     */
    public String initiateOAuthFlow(AccountType accountType, String state) {
        try {
            AuthorizationCodeFlow flow = createAuthorizationFlow();
            
            return flow.newAuthorizationUrl()
                    .setRedirectUri(oAuthProperties.getRedirectUri())
                    .setState(state)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to initiate OAuth flow for account type {}", accountType, e);
            throw new GoogleCalendarException("Failed to initiate OAuth flow", e);
        }
    }
    
    /**
     * Handle OAuth callback and exchange code for tokens
     */
    public OAuthTokenResult handleOAuthCallback(String code, String state, AccountType accountType,
                                              String userId, Long studentId) {
        try {
            AuthorizationCodeFlow flow = createAuthorizationFlow();
            
            GoogleTokenResponse tokenResponse = (GoogleTokenResponse) flow.newTokenRequest(code)
                    .setRedirectUri(oAuthProperties.getRedirectUri())
                    .execute();
            
            // Get user info to retrieve email
            Credential credential = flow.createAndStoreCredential(tokenResponse, userId);
            String googleEmail = getUserEmail(credential);
            
            // Calculate expiration time
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(tokenResponse.getExpiresInSeconds());
            
            // Store tokens
            tokenService.storeTokens(userId, studentId, accountType,
                    tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(),
                    expiresAt, googleEmail, null);
            
            log.info("Successfully handled OAuth callback for user {} student {} account type {}", 
                    userId, studentId, accountType);
            
            return new OAuthTokenResult(tokenResponse.getAccessToken(), 
                    tokenResponse.getRefreshToken(), expiresAt, googleEmail);
                    
        } catch (Exception e) {
            log.error("Failed to handle OAuth callback for account type {}", accountType, e);
            throw new GoogleCalendarException("Failed to handle OAuth callback", e);
        }
    }
    
    /**
     * Create dedicated calendar for parent account
     */
    public String createParentCalendar(String userId, Long studentId, String studentName) {
        return createCalendar(userId, studentId, AccountType.PARENT, 
                String.format(PARENT_CALENDAR_NAME_TEMPLATE, studentName),
                String.format(CALENDAR_DESCRIPTION_TEMPLATE, studentName));
    }
    
    /**
     * Create dedicated calendar for student account
     */
    public String createStudentCalendar(String userId, Long studentId) {
        return createCalendar(userId, studentId, AccountType.STUDENT,
                STUDENT_CALENDAR_NAME,
                String.format(CALENDAR_DESCRIPTION_TEMPLATE, "student"));
    }
    
    /**
     * Create calendar with retry logic
     */
    private String createCalendar(String userId, Long studentId, AccountType accountType,
                                String calendarName, String description) {
        return executeWithRetry(() -> {
            Calendar calendarService = createCalendarService(userId, studentId, accountType);
            
            com.google.api.services.calendar.model.Calendar calendar = 
                    new com.google.api.services.calendar.model.Calendar();
            calendar.setSummary(calendarName);
            calendar.setDescription(description);
            calendar.setTimeZone(TimeZone.getDefault().getID());
            
            com.google.api.services.calendar.model.Calendar createdCalendar = 
                    calendarService.calendars().insert(calendar).execute();
            
            String calendarId = createdCalendar.getId();
            
            // Update token with calendar ID
            tokenService.updateCalendarId(userId, studentId, accountType, calendarId);
            
            log.info("Created calendar {} for user {} student {} account type {}", 
                    calendarId, userId, studentId, accountType);
            
            return calendarId;
        }, "create calendar");
    }
    
    /**
     * Create assignment event in calendar
     */
    public String createAssignmentEvent(String userId, Long studentId, AccountType accountType,
                                      PlannerItem assignment) {
        return executeWithRetry(() -> {
            Calendar calendarService = createCalendarService(userId, studentId, accountType);
            String calendarId = getOrCreateCalendarId(userId, studentId, accountType, "Student");
            
            Event event = buildAssignmentEvent(assignment, accountType);
            
            Event createdEvent = calendarService.events()
                    .insert(calendarId, event)
                    .execute();
            
            log.info("Created event {} for assignment {} in calendar {} for user {} student {} account type {}", 
                    createdEvent.getId(), assignment.getPlannableId(), calendarId, 
                    userId, studentId, accountType);
            
            return createdEvent.getId();
        }, "create assignment event");
    }
    
    /**
     * Update assignment event in calendar
     */
    public boolean updateAssignmentEvent(String userId, Long studentId, AccountType accountType,
                                       String eventId, PlannerItem assignment) {
        return executeWithRetry(() -> {
            Calendar calendarService = createCalendarService(userId, studentId, accountType);
            String calendarId = getCalendarId(userId, studentId, accountType);
            
            if (calendarId == null) {
                log.warn("No calendar ID found for user {} student {} account type {}", 
                        userId, studentId, accountType);
                return false;
            }
            
            Event event = buildAssignmentEvent(assignment, accountType);
            
            calendarService.events()
                    .update(calendarId, eventId, event)
                    .execute();
            
            log.info("Updated event {} for assignment {} in calendar {} for user {} student {} account type {}", 
                    eventId, assignment.getPlannableId(), calendarId, 
                    userId, studentId, accountType);
            
            return true;
        }, "update assignment event");
    }
    
    /**
     * Delete assignment event from calendar
     */
    public boolean deleteAssignmentEvent(String userId, Long studentId, AccountType accountType,
                                       String eventId) {
        return executeWithRetry(() -> {
            Calendar calendarService = createCalendarService(userId, studentId, accountType);
            String calendarId = getCalendarId(userId, studentId, accountType);
            
            if (calendarId == null) {
                log.warn("No calendar ID found for user {} student {} account type {}", 
                        userId, studentId, accountType);
                return false;
            }
            
            calendarService.events()
                    .delete(calendarId, eventId)
                    .execute();
            
            log.info("Deleted event {} from calendar {} for user {} student {} account type {}", 
                    eventId, calendarId, userId, studentId, accountType);
            
            return true;
        }, "delete assignment event");
    }
    
    /**
     * Refresh access token using refresh token
     */
    public boolean refreshAccessToken(String userId, Long studentId, AccountType accountType) {
        try {
            Optional<String> refreshTokenOpt = tokenService.getRefreshToken(userId, studentId, accountType);
            
            if (refreshTokenOpt.isEmpty()) {
                log.warn("No refresh token found for user {} student {} account type {}", 
                        userId, studentId, accountType);
                return false;
            }
            
            AuthorizationCodeFlow flow = createAuthorizationFlow();
            
            GoogleTokenResponse tokenResponse = (GoogleTokenResponse) flow.newTokenRequest(refreshTokenOpt.get())
                    .setGrantType("refresh_token")
                    .execute();
            
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(tokenResponse.getExpiresInSeconds());
            
            boolean updated = tokenService.updateTokenExpiration(userId, studentId, accountType,
                    tokenResponse.getAccessToken(), expiresAt);
            
            if (updated) {
                log.info("Successfully refreshed access token for user {} student {} account type {}", 
                        userId, studentId, accountType);
            }
            
            return updated;
            
        } catch (Exception e) {
            log.error("Failed to refresh access token for user {} student {} account type {}", 
                    userId, studentId, accountType, e);
            return false;
        }
    }
    
    /**
     * Check if calendar access is valid
     */
    public boolean isCalendarAccessValid(String userId, Long studentId, AccountType accountType) {
        try {
            Calendar calendarService = createCalendarService(userId, studentId, accountType);
            
            // Try to list calendars to verify access
            CalendarList calendarList = calendarService.calendarList().list()
                    .setMaxResults(1)
                    .execute();
            
            return calendarList != null;
            
        } catch (Exception e) {
            log.debug("Calendar access not valid for user {} student {} account type {}: {}", 
                    userId, studentId, accountType, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get or create calendar ID for account type
     */
    private String getOrCreateCalendarId(String userId, Long studentId, AccountType accountType, 
                                       String studentName) {
        String calendarId = getCalendarId(userId, studentId, accountType);
        
        if (calendarId == null) {
            // Create calendar if it doesn't exist
            if (accountType == AccountType.PARENT) {
                calendarId = createParentCalendar(userId, studentId, studentName);
            } else {
                calendarId = createStudentCalendar(userId, studentId);
            }
        }
        
        return calendarId;
    }
    
    /**
     * Get calendar ID from token service
     */
    private String getCalendarId(String userId, Long studentId, AccountType accountType) {
        return tokenService.getCalendarId(userId, studentId, accountType).orElse(null);
    }
    
    /**
     * Create Calendar service instance with user credentials
     */
    private Calendar createCalendarService(String userId, Long studentId, AccountType accountType) 
            throws IOException {
        Optional<String> accessTokenOpt = tokenService.getValidAccessToken(userId, studentId, accountType);
        
        if (accessTokenOpt.isEmpty()) {
            throw new GoogleCalendarException("No valid access token available");
        }
        
        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                .setAccessToken(accessTokenOpt.get());
        
        return calendarBuilder.setHttpRequestInitializer(credential).build();
    }
    
    /**
     * Build Event object from PlannerItem
     */
    private Event buildAssignmentEvent(PlannerItem assignment, AccountType accountType) {
        Event event = new Event();
        
        // Set event title
        event.setSummary(assignment.getAssignmentTitle());
        
        // Set event description
        StringBuilder description = new StringBuilder();
        description.append("Course: ").append(assignment.getContextName()).append("\n");
        if (assignment.getPointsPossible() != null) {
            description.append("Points: ").append(assignment.getPointsPossible()).append("\n");
        }
        description.append("Assignment ID: ").append(assignment.getPlannableId());
        event.setDescription(description.toString());
        
        // Set event time (1 hour duration ending at due time)
        if (assignment.getDueAt() != null) {
            DateTime startTime = new DateTime(
                    assignment.getDueAt().minusHours(1)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
            DateTime endTime = new DateTime(
                    assignment.getDueAt()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
            
            event.setStart(new EventDateTime().setDateTime(startTime));
            event.setEnd(new EventDateTime().setDateTime(endTime));
        }
        
        // Set reminders based on account type
        Event.Reminders reminders = new Event.Reminders();
        reminders.setUseDefault(false);
        
        List<EventReminder> reminderList = new ArrayList<>();
        if (accountType == AccountType.PARENT) {
            // Parent reminders: 24h, 2h
            reminderList.add(new EventReminder().setMethod("email").setMinutes(1440)); // 24h
            reminderList.add(new EventReminder().setMethod("popup").setMinutes(120));  // 2h
        } else {
            // Student reminders: 2h, 30min
            reminderList.add(new EventReminder().setMethod("popup").setMinutes(120));  // 2h
            reminderList.add(new EventReminder().setMethod("popup").setMinutes(30));   // 30min
        }
        
        reminders.setOverrides(reminderList);
        event.setReminders(reminders);
        
        // Set color
        event.setColorId(EVENT_COLOR_ID);
        
        // Set extended properties for assignment metadata
        Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
        Map<String, String> privateProperties = new HashMap<>();
        privateProperties.put("assignmentId", assignment.getPlannableId().toString());
        privateProperties.put("studentId", assignment.getStudentId().toString());
        if (assignment.getPointsPossible() != null) {
            privateProperties.put("pointsPossible", assignment.getPointsPossible().toString());
        }
        extendedProperties.setPrivate(privateProperties);
        event.setExtendedProperties(extendedProperties);
        
        return event;
    }
    
    /**
     * Create authorization flow for OAuth
     */
    private AuthorizationCodeFlow createAuthorizationFlow() throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory,
                oAuthProperties.getClientId(),
                oAuthProperties.getClientSecret(),
                oAuthProperties.getScopes())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }
    
    /**
     * Get user email from credential
     */
    private String getUserEmail(Credential credential) {
        // This would typically require an additional API call to get user info
        // For now, we'll return null and handle email separately
        return null;
    }
    
    /**
     * Execute operation with retry logic and rate limiting
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) {
        Exception lastException = null;
        long delay = INITIAL_RETRY_DELAY_MS;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
                
            } catch (Exception e) {
                lastException = e;
                
                if (isRateLimitException(e)) {
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        log.warn("Rate limit exceeded for {}, retrying in {}ms (attempt {}/{})", 
                                operationName, delay, attempt, MAX_RETRY_ATTEMPTS);
                        
                        try {
                            Thread.sleep(delay);
                            delay *= 2; // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new GoogleCalendarException("Operation interrupted", ie);
                        }
                        continue;
                    } else {
                        throw new RateLimitExceededException("Rate limit exceeded after " + 
                                MAX_RETRY_ATTEMPTS + " attempts", e, delay / 1000);
                    }
                }
                
                // For non-rate-limit exceptions, don't retry
                break;
            }
        }
        
        throw new GoogleCalendarException("Failed to execute " + operationName + 
                " after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }
    
    /**
     * Check if exception is due to rate limiting
     */
    private boolean isRateLimitException(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("429") || 
                message.contains("Rate Limit Exceeded") ||
                message.contains("rateLimitExceeded")
        );
    }
    
    /**
     * Functional interface for retryable operations
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Result class for OAuth token exchange
     */
    public static class OAuthTokenResult {
        private final String accessToken;
        private final String refreshToken;
        private final LocalDateTime expiresAt;
        private final String googleEmail;
        
        public OAuthTokenResult(String accessToken, String refreshToken, 
                              LocalDateTime expiresAt, String googleEmail) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
            this.googleEmail = googleEmail;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getGoogleEmail() { return googleEmail; }
    }
}