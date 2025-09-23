package com.studytracker.service;

import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.repository.StudentCalendarInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentInvitationServiceTest {
    
    @Mock
    private StudentCalendarInvitationRepository invitationRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private StudentInvitationService invitationService;
    
    private static final String USER_ID = "user123";
    private static final Long STUDENT_ID = 456L;
    private static final String STUDENT_EMAIL = "student@example.com";
    private static final String STUDENT_NAME = "John Doe";
    private static final String BASE_URL = "http://localhost:8080";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(invitationService, "invitationExpiryHours", 72);
    }
    
    @Test
    void createAndSendInvitation_Success() {
        // Given
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(false);
        when(invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        
        StudentCalendarInvitation savedInvitation = createTestInvitation();
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(savedInvitation);
        
        // When
        StudentCalendarInvitation result = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        
        // Then
        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(STUDENT_ID, result.getStudentId());
        assertEquals(STUDENT_EMAIL, result.getStudentEmail());
        assertEquals(InvitationStatus.PENDING, result.getStatus());
        assertNotNull(result.getInvitationToken());
        assertNotNull(result.getExpiresAt());
        
        verify(invitationRepository).save(any(StudentCalendarInvitation.class));
        verify(emailService).sendCalendarInvitation(eq(STUDENT_EMAIL), eq(STUDENT_NAME), 
                anyString(), any(LocalDateTime.class));
    }
    
    @Test
    void createAndSendInvitation_ActiveInvitationExists_ThrowsException() {
        // Given
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(true);
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                invitationService.createAndSendInvitation(USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME));
        
        assertEquals("Active invitation already exists for this student", exception.getMessage());
        verify(invitationRepository, never()).save(any());
        verify(emailService, never()).sendCalendarInvitation(anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void createAndSendInvitation_DeletesExistingPendingInvitation() {
        // Given
        StudentCalendarInvitation existingInvitation = createTestInvitation();
        existingInvitation.setStatus(InvitationStatus.PENDING);
        
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(false);
        when(invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingInvitation));
        
        StudentCalendarInvitation savedInvitation = createTestInvitation();
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(savedInvitation);
        
        // When
        invitationService.createAndSendInvitation(USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        
        // Then
        verify(invitationRepository).delete(existingInvitation);
        verify(invitationRepository).save(any(StudentCalendarInvitation.class));
    }
    
    @Test
    void createAndSendInvitation_EmailFailure_DoesNotFailInvitationCreation() {
        // Given
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(false);
        when(invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        
        StudentCalendarInvitation savedInvitation = createTestInvitation();
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(savedInvitation);
        
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailService).sendCalendarInvitation(anyString(), anyString(), anyString(), any());
        
        // When
        StudentCalendarInvitation result = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        
        // Then
        assertNotNull(result);
        verify(invitationRepository).save(any(StudentCalendarInvitation.class));
        verify(emailService).sendCalendarInvitation(anyString(), anyString(), anyString(), any());
    }
    
    @Test
    void acceptInvitation_Success() {
        // Given
        String token = "valid-token";
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setInvitationToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(invitation);
        
        // When
        StudentCalendarInvitation result = invitationService.acceptInvitation(token);
        
        // Then
        assertNotNull(result);
        assertEquals(InvitationStatus.ACCEPTED, result.getStatus());
        assertNotNull(result.getAcceptedAt());
        verify(invitationRepository).save(invitation);
    }
    
    @Test
    void acceptInvitation_InvalidToken_ThrowsException() {
        // Given
        String token = "invalid-token";
        when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.empty());
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                invitationService.acceptInvitation(token));
        
        assertEquals("Invalid invitation token", exception.getMessage());
        verify(invitationRepository, never()).save(any());
    }
    
    @Test
    void acceptInvitation_ExpiredInvitation_ThrowsException() {
        // Given
        String token = "expired-token";
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setInvitationToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired
        
        when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(invitation);
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                invitationService.acceptInvitation(token));
        
        assertEquals("Invitation has expired", exception.getMessage());
        assertEquals(InvitationStatus.EXPIRED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }
    
    @Test
    void acceptInvitation_AlreadyAccepted_ThrowsException() {
        // Given
        String token = "accepted-token";
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setInvitationToken(token);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                invitationService.acceptInvitation(token));
        
        assertEquals("Invitation is not in pending status: ACCEPTED", exception.getMessage());
        verify(invitationRepository, never()).save(any());
    }
    
    @Test
    void validateInvitationToken_Success() {
        // Given
        String token = "valid-token";
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setInvitationToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));
        
        // When
        StudentCalendarInvitation result = invitationService.validateInvitationToken(token);
        
        // Then
        assertNotNull(result);
        assertEquals(token, result.getInvitationToken());
        assertEquals(InvitationStatus.PENDING, result.getStatus());
    }
    
    @Test
    void revokeInvitation_Success() {
        // Given
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setStatus(InvitationStatus.PENDING);
        
        when(invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(invitation);
        
        // When
        invitationService.revokeInvitation(USER_ID, STUDENT_ID);
        
        // Then
        assertEquals(InvitationStatus.REVOKED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }
    
    @Test
    void revokeInvitation_InvitationNotFound_DoesNotThrow() {
        // Given
        when(invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertDoesNotThrow(() -> invitationService.revokeInvitation(USER_ID, STUDENT_ID));
        verify(invitationRepository, never()).save(any());
    }
    
    @Test
    void revokeInvitationByToken_Success() {
        // Given
        String token = "token-to-revoke";
        StudentCalendarInvitation invitation = createTestInvitation();
        invitation.setInvitationToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        
        when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(StudentCalendarInvitation.class))).thenReturn(invitation);
        
        // When
        invitationService.revokeInvitationByToken(token);
        
        // Then
        assertEquals(InvitationStatus.REVOKED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }
    
    @Test
    void hasActiveInvitation_ReturnsTrue() {
        // Given
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(true);
        
        // When
        boolean result = invitationService.hasActiveInvitation(USER_ID, STUDENT_ID);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void hasActiveInvitation_ReturnsFalse() {
        // Given
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(false);
        
        // When
        boolean result = invitationService.hasActiveInvitation(USER_ID, STUDENT_ID);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void getUserInvitations_ReturnsInvitations() {
        // Given
        List<StudentCalendarInvitation> invitations = Arrays.asList(
                createTestInvitation(), createTestInvitation());
        when(invitationRepository.findByUserId(USER_ID)).thenReturn(invitations);
        
        // When
        List<StudentCalendarInvitation> result = invitationService.getUserInvitations(USER_ID);
        
        // Then
        assertEquals(2, result.size());
        verify(invitationRepository).findByUserId(USER_ID);
    }
    
    @Test
    void cleanupExpiredInvitations_Success() {
        // Given
        when(invitationRepository.markExpiredInvitations(
                eq(InvitationStatus.PENDING), eq(InvitationStatus.EXPIRED), any(LocalDateTime.class)))
                .thenReturn(5);
        when(invitationRepository.deleteExpiredInvitations(any(LocalDateTime.class)))
                .thenReturn(3);
        
        // When
        int result = invitationService.cleanupExpiredInvitations();
        
        // Then
        assertEquals(8, result);
        verify(invitationRepository).markExpiredInvitations(
                eq(InvitationStatus.PENDING), eq(InvitationStatus.EXPIRED), any(LocalDateTime.class));
        verify(invitationRepository).deleteExpiredInvitations(any(LocalDateTime.class));
    }
    
    @Test
    void generateSecureToken_GeneratesUniqueTokens() {
        // Given
        when(invitationRepository.existsActiveInvitation(eq(USER_ID), eq(STUDENT_ID), 
                eq(InvitationStatus.PENDING), any(LocalDateTime.class))).thenReturn(false);
        when(invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.empty());
        
        ArgumentCaptor<StudentCalendarInvitation> captor = ArgumentCaptor.forClass(StudentCalendarInvitation.class);
        when(invitationRepository.save(captor.capture())).thenReturn(createTestInvitation());
        
        // When
        invitationService.createAndSendInvitation(USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        invitationService.createAndSendInvitation(USER_ID, STUDENT_ID + 1, STUDENT_EMAIL, STUDENT_NAME);
        
        // Then
        List<StudentCalendarInvitation> savedInvitations = captor.getAllValues();
        assertEquals(2, savedInvitations.size());
        assertNotEquals(savedInvitations.get(0).getInvitationToken(), 
                       savedInvitations.get(1).getInvitationToken());
        
        // Verify tokens are not empty and have reasonable length
        assertTrue(savedInvitations.get(0).getInvitationToken().length() > 20);
        assertTrue(savedInvitations.get(1).getInvitationToken().length() > 20);
    }
    
    private StudentCalendarInvitation createTestInvitation() {
        return new StudentCalendarInvitation(
                USER_ID, 
                STUDENT_ID, 
                STUDENT_EMAIL, 
                "test-token-" + System.currentTimeMillis(),
                LocalDateTime.now().plusHours(72)
        );
    }
}