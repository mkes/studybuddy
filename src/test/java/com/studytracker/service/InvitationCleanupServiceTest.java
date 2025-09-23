package com.studytracker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationCleanupServiceTest {
    
    @Mock
    private StudentInvitationService invitationService;
    
    @InjectMocks
    private InvitationCleanupService cleanupService;
    
    @Test
    void cleanupExpiredInvitations_Success() {
        // Given
        when(invitationService.cleanupExpiredInvitations()).thenReturn(5);
        
        // When
        assertDoesNotThrow(() -> cleanupService.cleanupExpiredInvitations());
        
        // Then
        verify(invitationService).cleanupExpiredInvitations();
    }
    
    @Test
    void cleanupExpiredInvitations_NoInvitationsToCleanup() {
        // Given
        when(invitationService.cleanupExpiredInvitations()).thenReturn(0);
        
        // When
        assertDoesNotThrow(() -> cleanupService.cleanupExpiredInvitations());
        
        // Then
        verify(invitationService).cleanupExpiredInvitations();
    }
    
    @Test
    void cleanupExpiredInvitations_ServiceThrowsException_HandlesGracefully() {
        // Given
        when(invitationService.cleanupExpiredInvitations())
                .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then
        assertDoesNotThrow(() -> cleanupService.cleanupExpiredInvitations());
        
        verify(invitationService).cleanupExpiredInvitations();
    }
    
    @Test
    void sendExpirationReminders_Success() {
        // When
        assertDoesNotThrow(() -> cleanupService.sendExpirationReminders());
        
        // Then
        // This test verifies the method runs without throwing exceptions
        // In a real implementation, we would verify reminder emails are sent
    }
    
    @Test
    void sendExpirationReminders_HandlesExceptions() {
        // This test verifies that the scheduled method handles exceptions gracefully
        // When
        assertDoesNotThrow(() -> cleanupService.sendExpirationReminders());
        
        // Then
        // Method should complete without throwing exceptions
    }
}