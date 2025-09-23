package com.studytracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.dto.StudentDto;
import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarSyncSettings;
import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for CalendarController
 */
@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleCalendarService googleCalendarService;

    @MockBean
    private CalendarTokenService tokenService;

    @MockBean
    private CalendarSyncService syncService;

    @MockBean
    private StudentInvitationService invitationService;

    @MockBean
    private CanvasApiService canvasApiService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;
    private StudentDto testStudent;
    private CalendarSyncSettings testSyncSettings;
    private StudentCalendarInvitation testInvitation;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute("canvasToken", "test-canvas-token");

        testStudent = new StudentDto();
        testStudent.setId(123L);
        testStudent.setName("Test Student");

        testSyncSettings = new CalendarSyncSettings("test-user-id", 123L);

        testInvitation = new StudentCalendarInvitation(
                "test-user-id", 123L, "student@example.com", 
                "test-token", LocalDateTime.now().plusHours(72)
        );
    }

    @Test
    void calendarIntegrationPage_WithValidSession_ShouldDisplayPage() throws Exception {
        // Arrange
        when(canvasApiService.getObservedStudents("test-canvas-token"))
                .thenReturn(List.of(testStudent));
        
        CalendarTokenService.CalendarConnectionStatus connectionStatus = 
                new CalendarTokenService.CalendarConnectionStatus(true, false);
        when(tokenService.getConnectionStatus(anyString(), eq(123L)))
                .thenReturn(connectionStatus);
        
        when(invitationService.getInvitation(anyString(), eq(123L)))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/students/123/calendar")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar-integration"))
                .andExpect(model().attribute("studentId", 123L))
                .andExpect(model().attribute("student", testStudent))
                .andExpect(model().attribute("parentConnected", true))
                .andExpect(model().attribute("studentConnected", false));
    }

    @Test
    void calendarIntegrationPage_WithoutSession_ShouldRedirectToLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/students/123/calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));
    }

    @Test
    void calendarIntegrationPage_WithInvalidStudent_ShouldRedirectToStudents() throws Exception {
        // Arrange
        when(canvasApiService.getObservedStudents("test-canvas-token"))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/students/123/calendar")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attribute("error", "Student not found or access denied."));
    }

    @Test
    void initiateParentOAuth_WithValidSession_ShouldRedirectToGoogle() throws Exception {
        // Arrange
        String authUrl = "https://accounts.google.com/oauth/authorize?client_id=test";
        when(googleCalendarService.initiateOAuthFlow(eq(AccountType.PARENT), anyString()))
                .thenReturn(authUrl);

        // Act & Assert
        mockMvc.perform(get("/students/123/calendar/connect/parent")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(authUrl));

        // Verify session attributes are set
        assert session.getAttribute("oauth_state") != null;
        assert "PARENT".equals(session.getAttribute("oauth_account_type"));
        assert Long.valueOf(123L).equals(session.getAttribute("oauth_student_id"));
    }

    @Test
    void initiateParentOAuth_WithoutSession_ShouldRedirectToLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/students/123/calendar/connect/parent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));
    }

    @Test
    void initiateStudentOAuth_WithValidToken_ShouldRedirectToGoogle() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("valid-token"))
                .thenReturn(testInvitation);
        
        String authUrl = "https://accounts.google.com/oauth/authorize?client_id=test";
        when(googleCalendarService.initiateOAuthFlow(eq(AccountType.STUDENT), anyString()))
                .thenReturn(authUrl);

        // Act & Assert
        mockMvc.perform(get("/students/123/calendar/connect/student")
                .param("token", "valid-token")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(authUrl));

        // Verify session attributes are set
        assert session.getAttribute("oauth_state") != null;
        assert "STUDENT".equals(session.getAttribute("oauth_account_type"));
        assert Long.valueOf(123L).equals(session.getAttribute("oauth_student_id"));
        assert "test-user-id".equals(session.getAttribute("oauth_user_id"));
        assert "valid-token".equals(session.getAttribute("invitation_token"));
    }

    @Test
    void initiateStudentOAuth_WithInvalidToken_ShouldRedirectWithError() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("invalid-token"))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act & Assert
        mockMvc.perform(get("/students/123/calendar/connect/student")
                .param("token", "invalid-token")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Failed to connect to Google Calendar. Please try again."));
    }

    @Test
    void handleOAuthCallback_WithValidParentCallback_ShouldRedirectToCalendarPage() throws Exception {
        // Arrange
        session.setAttribute("oauth_state", "test-state");
        session.setAttribute("oauth_account_type", "PARENT");
        session.setAttribute("oauth_student_id", 123L);

        GoogleCalendarService.OAuthTokenResult tokenResult = 
                new GoogleCalendarService.OAuthTokenResult("access-token", "refresh-token", 
                        LocalDateTime.now().plusHours(1), "user@example.com");
        
        when(googleCalendarService.handleOAuthCallback(eq("auth-code"), eq("test-state"), 
                eq(AccountType.PARENT), anyString(), eq(123L)))
                .thenReturn(tokenResult);

        // Act & Assert
        mockMvc.perform(get("/auth/google/callback")
                .param("code", "auth-code")
                .param("state", "test-state")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", "Google Calendar connected successfully for parent account!"));

        // Verify session is cleared
        assert session.getAttribute("oauth_state") == null;
        assert session.getAttribute("oauth_account_type") == null;
        assert session.getAttribute("oauth_student_id") == null;
    }

    @Test
    void handleOAuthCallback_WithValidStudentCallback_ShouldAcceptInvitationAndRedirect() throws Exception {
        // Arrange
        session.setAttribute("oauth_state", "test-state");
        session.setAttribute("oauth_account_type", "STUDENT");
        session.setAttribute("oauth_student_id", 123L);
        session.setAttribute("oauth_user_id", "test-user-id");
        session.setAttribute("invitation_token", "invitation-token");

        GoogleCalendarService.OAuthTokenResult tokenResult = 
                new GoogleCalendarService.OAuthTokenResult("access-token", "refresh-token", 
                        LocalDateTime.now().plusHours(1), "student@example.com");
        
        when(googleCalendarService.handleOAuthCallback(eq("auth-code"), eq("test-state"), 
                eq(AccountType.STUDENT), eq("test-user-id"), eq(123L)))
                .thenReturn(tokenResult);
        
        when(invitationService.acceptInvitation("invitation-token"))
                .thenReturn(testInvitation);

        // Act & Assert
        mockMvc.perform(get("/auth/google/callback")
                .param("code", "auth-code")
                .param("state", "test-state")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", "Google Calendar connected successfully for student account!"));

        verify(invitationService).acceptInvitation("invitation-token");
    }

    @Test
    void handleOAuthCallback_WithInvalidState_ShouldRedirectWithError() throws Exception {
        // Arrange
        session.setAttribute("oauth_state", "correct-state");

        // Act & Assert
        mockMvc.perform(get("/auth/google/callback")
                .param("code", "auth-code")
                .param("state", "wrong-state")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attribute("error", "Invalid authorization request. Please try again."));
    }

    @Test
    void handleOAuthCallback_WithOAuthError_ShouldRedirectWithError() throws Exception {
        // Arrange
        session.setAttribute("oauth_student_id", 123L);
        
        // Act & Assert
        mockMvc.perform(get("/auth/google/callback")
                .param("error", "access_denied")
                .param("state", "test-state")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("error", "Google Calendar authorization was denied or failed."));
    }

    @Test
    void inviteStudent_WithValidEmail_ShouldSendInvitation() throws Exception {
        // Arrange
        when(canvasApiService.getObservedStudents("test-canvas-token"))
                .thenReturn(List.of(testStudent));
        
        when(invitationService.createAndSendInvitation(anyString(), eq(123L), 
                eq("student@example.com"), eq("Test Student")))
                .thenReturn(testInvitation);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/invite-student")
                .param("studentEmail", "student@example.com")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", 
                        "Calendar sync invitation sent to student@example.com. The invitation will expire in 72 hours."));

        verify(invitationService).createAndSendInvitation(anyString(), eq(123L), 
                eq("student@example.com"), eq("Test Student"));
    }

    @Test
    void inviteStudent_WithInvalidEmail_ShouldShowError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/invite-student")
                .param("studentEmail", "invalid-email")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("error", "Please enter a valid email address."));

        verify(invitationService, never()).createAndSendInvitation(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void inviteStudent_WithExistingInvitation_ShouldShowError() throws Exception {
        // Arrange
        when(canvasApiService.getObservedStudents("test-canvas-token"))
                .thenReturn(List.of(testStudent));
        
        when(invitationService.createAndSendInvitation(anyString(), eq(123L), 
                eq("student@example.com"), eq("Test Student")))
                .thenThrow(new IllegalStateException("Active invitation already exists for this student"));

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/invite-student")
                .param("studentEmail", "student@example.com")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("error", "Active invitation already exists for this student"));
    }

    @Test
    void triggerManualSync_WithValidSession_ShouldReturnSyncResult() throws Exception {
        // Arrange
        CalendarSyncService.SyncResult syncResult = CalendarSyncService.SyncResult.success();
        when(syncService.syncStudentAssignments(anyString(), eq(123L)))
                .thenReturn(syncResult);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/sync")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.deleted").value(0));

        verify(syncService).syncStudentAssignments(anyString(), eq(123L));
    }

    @Test
    void triggerManualSync_WithoutSession_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/sync"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(syncService, never()).syncStudentAssignments(anyString(), anyLong());
    }

    @Test
    void triggerManualSync_WithSyncError_ShouldReturnErrorResult() throws Exception {
        // Arrange
        CalendarSyncService.SyncResult syncResult = CalendarSyncService.SyncResult.error("Sync failed");
        when(syncService.syncStudentAssignments(anyString(), eq(123L)))
                .thenReturn(syncResult);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/sync")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Sync completed with errors. Check the error details."));
    }

    @Test
    void disconnectCalendar_WithParentAccount_ShouldDisconnectAndRedirect() throws Exception {
        // Arrange
        when(tokenService.revokeTokens(anyString(), eq(123L), eq(AccountType.PARENT)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/disconnect")
                .param("accountType", "PARENT")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", "parent calendar disconnected successfully."));

        verify(tokenService).revokeTokens(anyString(), eq(123L), eq(AccountType.PARENT));
    }

    @Test
    void disconnectCalendar_WithStudentAccount_ShouldDisconnectAndRedirect() throws Exception {
        // Arrange
        when(tokenService.revokeTokens(anyString(), eq(123L), eq(AccountType.STUDENT)))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/disconnect")
                .param("accountType", "STUDENT")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", "student calendar disconnected successfully."));

        verify(tokenService).revokeTokens(anyString(), eq(123L), eq(AccountType.STUDENT));
    }

    @Test
    void disconnectCalendar_WithFailure_ShouldShowError() throws Exception {
        // Arrange
        when(tokenService.revokeTokens(anyString(), eq(123L), eq(AccountType.PARENT)))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/disconnect")
                .param("accountType", "PARENT")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("error", "Failed to disconnect calendar."));
    }

    @Test
    void updateSyncSettings_WithValidSettings_ShouldUpdateAndRedirect() throws Exception {
        // Arrange
        CalendarSyncService.SyncResult syncResult = CalendarSyncService.SyncResult.success();
        when(syncService.applySyncSettings(anyString(), eq(123L), any(CalendarSyncSettings.class)))
                .thenReturn(syncResult);

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/settings")
                .param("syncEnabled", "true")
                .param("autoSyncEnabled", "true")
                .param("syncCompletedAssignments", "false")
                .param("syncToParentCalendar", "true")
                .param("syncToStudentCalendar", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", "Sync settings updated successfully."));

        verify(syncService).applySyncSettings(anyString(), eq(123L), any(CalendarSyncSettings.class));
    }

    @Test
    void revokeInvitation_WithValidSession_ShouldRevokeAndRedirect() throws Exception {
        // Arrange
        doNothing().when(invitationService).revokeInvitation(anyString(), eq(123L));

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/revoke-invitation")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("success", "Student invitation revoked successfully."));

        verify(invitationService).revokeInvitation(anyString(), eq(123L));
    }

    @Test
    void revokeInvitation_WithError_ShouldShowError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(invitationService).revokeInvitation(anyString(), eq(123L));

        // Act & Assert
        mockMvc.perform(post("/students/123/calendar/revoke-invitation")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar"))
                .andExpect(flash().attribute("error", "Failed to revoke invitation. Please try again."));
    }
}