package com.studytracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.service.StudentInvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvitationController.class)
class InvitationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private StudentInvitationService invitationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void sendInvitation_Success() throws Exception {
        // Given
        StudentCalendarInvitation invitation = createTestInvitation();
        when(invitationService.createAndSendInvitation(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(invitation);
        
        Map<String, Object> request = new HashMap<>();
        request.put("userId", "user123");
        request.put("studentId", 456L);
        request.put("studentEmail", "student@example.com");
        request.put("studentName", "John Doe");
        
        // When & Then
        mockMvc.perform(post("/api/invitations/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invitation sent successfully"))
                .andExpect(jsonPath("$.invitationId").value(invitation.getId()));
    }
    
    @Test
    void sendInvitation_ActiveInvitationExists_ReturnsBadRequest() throws Exception {
        // Given
        when(invitationService.createAndSendInvitation(anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Active invitation already exists for this student"));
        
        Map<String, Object> request = new HashMap<>();
        request.put("userId", "user123");
        request.put("studentId", 456L);
        request.put("studentEmail", "student@example.com");
        request.put("studentName", "John Doe");
        
        // When & Then
        mockMvc.perform(post("/api/invitations/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Active invitation already exists for this student"));
    }
    
    @Test
    void acceptInvitation_Success() throws Exception {
        // Given
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        
        when(invitationService.acceptInvitation("valid-token")).thenReturn(invitation);
        
        // When & Then
        mockMvc.perform(post("/api/invitations/accept")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invitation accepted successfully"))
                .andExpect(jsonPath("$.invitationId").value(invitation.getId()));
    }
    
    @Test
    void acceptInvitation_InvalidToken_ReturnsBadRequest() throws Exception {
        // Given
        when(invitationService.acceptInvitation("invalid-token"))
                .thenThrow(new IllegalArgumentException("Invalid invitation token"));
        
        // When & Then
        mockMvc.perform(post("/api/invitations/accept")
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid invitation token"));
    }
    
    @Test
    void validateInvitation_ValidToken_ReturnsValid() throws Exception {
        // Given
        StudentCalendarInvitation invitation = createTestInvitation();
        when(invitationService.validateInvitationToken("valid-token")).thenReturn(invitation);
        
        // When & Then
        mockMvc.perform(get("/api/invitations/validate")
                .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.studentEmail").value(invitation.getStudentEmail()));
    }
    
    @Test
    void validateInvitation_InvalidToken_ReturnsInvalid() throws Exception {
        // Given
        when(invitationService.validateInvitationToken("invalid-token"))
                .thenThrow(new IllegalArgumentException("Invalid invitation token"));
        
        // When & Then
        mockMvc.perform(get("/api/invitations/validate")
                .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid invitation token"));
    }
    
    @Test
    void getUserInvitations_Success() throws Exception {
        // Given
        when(invitationService.getUserInvitations("user123"))
                .thenReturn(Arrays.asList(createTestInvitation(), createTestInvitation()));
        
        // When & Then
        mockMvc.perform(get("/api/invitations/user/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.invitations").isArray())
                .andExpect(jsonPath("$.invitations.length()").value(2));
    }
    
    @Test
    void revokeInvitation_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/invitations/revoke")
                .param("userId", "user123")
                .param("studentId", "456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invitation revoked successfully"));
    }
    
    @Test
    void hasActiveInvitation_ReturnsTrue() throws Exception {
        // Given
        when(invitationService.hasActiveInvitation("user123", 456L)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/invitations/active")
                .param("userId", "user123")
                .param("studentId", "456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.hasActiveInvitation").value(true));
    }
    
    @Test
    void hasActiveInvitation_ReturnsFalse() throws Exception {
        // Given
        when(invitationService.hasActiveInvitation("user123", 456L)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/invitations/active")
                .param("userId", "user123")
                .param("studentId", "456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.hasActiveInvitation").value(false));
    }
    
    private StudentCalendarInvitation createTestInvitation() {
        StudentCalendarInvitation invitation = new StudentCalendarInvitation(
                "user123", 456L, "student@example.com", "test-token", LocalDateTime.now().plusDays(3));
        invitation.setId(1L);
        return invitation;
    }
}