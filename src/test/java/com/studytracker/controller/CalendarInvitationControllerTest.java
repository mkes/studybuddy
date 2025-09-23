package com.studytracker.controller;

import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.service.StudentInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for CalendarInvitationController
 */
@WebMvcTest(CalendarInvitationController.class)
class CalendarInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentInvitationService invitationService;

    private StudentCalendarInvitation testInvitation;

    @BeforeEach
    void setUp() {
        testInvitation = new StudentCalendarInvitation(
                "test-user-id", 123L, "student@example.com", 
                "test-token", LocalDateTime.now().plusHours(72)
        );
    }

    @Test
    void showInvitationAcceptance_WithValidToken_ShouldDisplayAcceptancePage() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("valid-token"))
                .thenReturn(testInvitation);

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/accept")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar-invitation-accept"))
                .andExpect(model().attribute("invitation", testInvitation))
                .andExpect(model().attribute("token", "valid-token"));

        verify(invitationService).validateInvitationToken("valid-token");
    }

    @Test
    void showInvitationAcceptance_WithInvalidToken_ShouldDisplayErrorPage() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("invalid-token"))
                .thenThrow(new IllegalArgumentException("Invalid invitation token"));

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/accept")
                .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar-invitation-error"))
                .andExpect(model().attribute("error", "Invalid invitation link. Please check the link and try again."));

        verify(invitationService).validateInvitationToken("invalid-token");
    }

    @Test
    void showInvitationAcceptance_WithExpiredToken_ShouldDisplayErrorPage() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("expired-token"))
                .thenThrow(new IllegalStateException("Invitation has expired"));

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/accept")
                .param("token", "expired-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar-invitation-error"))
                .andExpect(model().attribute("error", "Invitation has expired"));

        verify(invitationService).validateInvitationToken("expired-token");
    }

    @Test
    void showInvitationAcceptance_WithServiceError_ShouldDisplayErrorPage() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("error-token"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/accept")
                .param("token", "error-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar-invitation-error"))
                .andExpect(model().attribute("error", "An error occurred while processing your invitation. Please try again later."));

        verify(invitationService).validateInvitationToken("error-token");
    }

    @Test
    void proceedWithInvitation_WithValidToken_ShouldRedirectToOAuthFlow() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("valid-token"))
                .thenReturn(testInvitation);

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/proceed")
                .param("token", "valid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students/123/calendar/connect/student?token=valid-token"));

        verify(invitationService).validateInvitationToken("valid-token");
    }

    @Test
    void proceedWithInvitation_WithInvalidToken_ShouldRedirectBackWithError() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("invalid-token"))
                .thenThrow(new IllegalArgumentException("Invalid invitation token"));

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/proceed")
                .param("token", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar/invitation/accept?token=invalid-token"))
                .andExpect(flash().attribute("error", "Failed to process invitation. Please try again."));

        verify(invitationService).validateInvitationToken("invalid-token");
    }

    @Test
    void proceedWithInvitation_WithServiceError_ShouldRedirectBackWithError() throws Exception {
        // Arrange
        when(invitationService.validateInvitationToken("error-token"))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/proceed")
                .param("token", "error-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar/invitation/accept?token=error-token"))
                .andExpect(flash().attribute("error", "Failed to process invitation. Please try again."));

        verify(invitationService).validateInvitationToken("error-token");
    }

    @Test
    void showInvitationAcceptance_WithMissingToken_ShouldHandleGracefully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/accept"))
                .andExpect(status().isBadRequest());

        verify(invitationService, never()).validateInvitationToken(anyString());
    }

    @Test
    void proceedWithInvitation_WithMissingToken_ShouldHandleGracefully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/calendar/invitation/proceed"))
                .andExpect(status().isBadRequest());

        verify(invitationService, never()).validateInvitationToken(anyString());
    }
}