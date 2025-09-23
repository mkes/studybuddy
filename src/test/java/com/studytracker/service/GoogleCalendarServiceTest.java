package com.studytracker.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.calendar.Calendar;
import com.studytracker.config.GoogleOAuthProperties;
import com.studytracker.exception.GoogleCalendarException;
import com.studytracker.model.AccountType;
import com.studytracker.model.PlannerItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GoogleCalendarService.
 * Tests OAuth flows, calendar operations, and event management with mocked Google Calendar API.
 */
@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {
    
    @Mock
    private Calendar.Builder calendarBuilder;
    
    @Mock
    private HttpTransport httpTransport;
    
    @Mock
    private JsonFactory jsonFactory;
    
    @Mock
    private GoogleOAuthProperties oAuthProperties;
    
    @Mock
    private CalendarTokenService tokenService;
    
    private GoogleCalendarService googleCalendarService;
    
    private static final String USER_ID = "user123";
    private static final Long STUDENT_ID = 456L;
    private static final String ACCESS_TOKEN = "access_token_123";
    private static final String CALENDAR_ID = "calendar_123";
    private static final String EVENT_ID = "event_123";
    private static final String STUDENT_NAME = "John Doe";
    
    @BeforeEach
    void setUp() {
        // Setup OAuth properties
        lenient().when(oAuthProperties.getClientId()).thenReturn("client_id");
        lenient().when(oAuthProperties.getClientSecret()).thenReturn("client_secret");
        lenient().when(oAuthProperties.getRedirectUri()).thenReturn("http://localhost:8080/auth/google/callback");
        lenient().when(oAuthProperties.getScopes()).thenReturn(Arrays.asList(
                "https://www.googleapis.com/auth/calendar",
                "https://www.googleapis.com/auth/calendar.events"
        ));
        
        googleCalendarService = new GoogleCalendarService(
                calendarBuilder, httpTransport, jsonFactory, oAuthProperties, tokenService);
    }
    
    @Test
    void testInitiateOAuthFlow_Success() {
        // Given
        AccountType accountType = AccountType.PARENT;
        String state = "state123";
        
        // When
        String authUrl = googleCalendarService.initiateOAuthFlow(accountType, state);
        
        // Then
        assertNotNull(authUrl);
        assertTrue(authUrl.contains("accounts.google.com"));
        assertTrue(authUrl.contains("state=" + state));
    }
    
    @Test
    void testCreateAssignmentEvent_NoValidToken() {
        // Given
        PlannerItem assignment = createTestAssignment();
        
        when(tokenService.getValidAccessToken(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(GoogleCalendarException.class, () -> 
                googleCalendarService.createAssignmentEvent(
                        USER_ID, STUDENT_ID, AccountType.PARENT, assignment));
    }
    
    @Test
    void testIsCalendarAccessValid_NoToken() {
        // Given
        when(tokenService.getValidAccessToken(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When
        boolean result = googleCalendarService.isCalendarAccessValid(
                USER_ID, STUDENT_ID, AccountType.PARENT);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testRefreshAccessToken_NoRefreshToken() {
        // Given
        when(tokenService.getRefreshToken(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When
        boolean result = googleCalendarService.refreshAccessToken(
                USER_ID, STUDENT_ID, AccountType.PARENT);
        
        // Then
        assertFalse(result);
        verify(tokenService, never()).updateTokenExpiration(any(), any(), any(), any(), any());
    }
    
    @Test
    void testParseReminderMinutes_ValidJson() {
        // Given
        String reminderJson = "[1440,120,30]";
        
        // When
        List<Integer> result = GoogleCalendarService.parseReminderMinutes(reminderJson);
        
        // Then
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1440), result.get(0));
        assertEquals(Integer.valueOf(120), result.get(1));
        assertEquals(Integer.valueOf(30), result.get(2));
    }
    
    @Test
    void testParseReminderMinutes_EmptyJson() {
        // Given
        String reminderJson = "[]";
        
        // When
        List<Integer> result = GoogleCalendarService.parseReminderMinutes(reminderJson);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testParseReminderMinutes_NullJson() {
        // Given
        String reminderJson = null;
        
        // When
        List<Integer> result = GoogleCalendarService.parseReminderMinutes(reminderJson);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testParseReminderMinutes_InvalidJson() {
        // Given
        String reminderJson = "[1440,invalid,30]";
        
        // When
        List<Integer> result = GoogleCalendarService.parseReminderMinutes(reminderJson);
        
        // Then
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1440), result.get(0));
        assertEquals(Integer.valueOf(30), result.get(1));
    }
    
    @Test
    void testCreateAssignmentEvent_WithCustomReminders_NoValidToken() {
        // Given
        PlannerItem assignment = createTestAssignment();
        List<Integer> customReminders = Arrays.asList(60, 15); // 1 hour, 15 minutes
        
        when(tokenService.getValidAccessToken(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(GoogleCalendarException.class, () -> 
                googleCalendarService.createAssignmentEvent(
                        USER_ID, STUDENT_ID, AccountType.PARENT, assignment, customReminders));
    }
    
    @Test
    void testMarkEventAsCompleted_NoValidToken() {
        // Given
        PlannerItem completedAssignment = createCompletedAssignment();
        
        when(tokenService.getValidAccessToken(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(GoogleCalendarException.class, () -> 
                googleCalendarService.markEventAsCompleted(
                        USER_ID, STUDENT_ID, AccountType.PARENT, EVENT_ID, completedAssignment));
    }
    
    @Test
    void testHandleCompletedAssignmentEvent_DeleteCompleted_NoValidToken() {
        // Given
        PlannerItem completedAssignment = createCompletedAssignment();
        
        when(tokenService.getValidAccessToken(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(GoogleCalendarException.class, () -> 
                googleCalendarService.handleCompletedAssignmentEvent(
                        USER_ID, STUDENT_ID, AccountType.PARENT, EVENT_ID, completedAssignment, true));
    }
    
    private PlannerItem createTestAssignment() {
        return PlannerItem.builder()
                .id(1L)
                .studentId(STUDENT_ID)
                .plannableId(789L)
                .assignmentTitle("Math Homework")
                .contextName("Mathematics 101")
                .dueAt(LocalDateTime.now().plusDays(2))
                .pointsPossible(new BigDecimal("100"))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();
    }
    
    private PlannerItem createCompletedAssignment() {
        return PlannerItem.builder()
                .id(1L)
                .studentId(STUDENT_ID)
                .plannableId(789L)
                .assignmentTitle("Math Homework")
                .contextName("Mathematics 101")
                .dueAt(LocalDateTime.now().minusDays(1))
                .pointsPossible(new BigDecimal("100"))
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .build();
    }
    
    private PlannerItem createQuizAssignment() {
        return PlannerItem.builder()
                .id(2L)
                .studentId(STUDENT_ID)
                .plannableId(790L)
                .assignmentTitle("Chapter 5 Quiz")
                .contextName("Biology 101")
                .dueAt(LocalDateTime.now().plusDays(1))
                .pointsPossible(new BigDecimal("50"))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();
    }
    
    private PlannerItem createMissingAssignment() {
        return PlannerItem.builder()
                .id(3L)
                .studentId(STUDENT_ID)
                .plannableId(791L)
                .assignmentTitle("Essay Assignment")
                .contextName("English 101")
                .dueAt(LocalDateTime.now().minusDays(2))
                .pointsPossible(new BigDecimal("200"))
                .submitted(false)
                .missing(true)
                .late(true)
                .graded(false)
                .build();
    }

}