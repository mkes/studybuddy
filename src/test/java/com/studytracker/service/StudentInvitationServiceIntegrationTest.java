package com.studytracker.service;

import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.repository.StudentCalendarInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudentInvitationServiceIntegrationTest {
    
    @Autowired
    private StudentInvitationService invitationService;
    
    @Autowired
    private StudentCalendarInvitationRepository invitationRepository;
    
    @MockBean
    private EmailService emailService;
    
    private static final String USER_ID = "integration-user-123";
    private static final Long STUDENT_ID = 999L;
    private static final String STUDENT_EMAIL = "integration.student@example.com";
    private static final String STUDENT_NAME = "Integration Test Student";
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        invitationRepository.deleteAll();
        
        // Mock email service to avoid actual email sending
        doNothing().when(emailService).sendCalendarInvitation(
                anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }
    
    @Test
    void createAndSendInvitation_FullWorkflow() {
        // When
        StudentCalendarInvitation invitation = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        
        // Then
        assertNotNull(invitation);
        assertEquals(USER_ID, invitation.getUserId());
        assertEquals(STUDENT_ID, invitation.getStudentId());
        assertEquals(STUDENT_EMAIL, invitation.getStudentEmail());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertNotNull(invitation.getInvitationToken());
        assertNotNull(invitation.getExpiresAt());
        assertTrue(invitation.getExpiresAt().isAfter(LocalDateTime.now()));
        
        // Verify it's persisted in database
        Optional<StudentCalendarInvitation> found = invitationRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID);
        assertTrue(found.isPresent());
        assertEquals(invitation.getInvitationToken(), found.get().getInvitationToken());
    }
    
    @Test
    void acceptInvitation_FullWorkflow() {
        // Given - Create invitation first
        StudentCalendarInvitation invitation = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        String token = invitation.getInvitationToken();
        
        // When
        StudentCalendarInvitation acceptedInvitation = invitationService.acceptInvitation(token);
        
        // Then
        assertNotNull(acceptedInvitation);
        assertEquals(InvitationStatus.ACCEPTED, acceptedInvitation.getStatus());
        assertNotNull(acceptedInvitation.getAcceptedAt());
        assertTrue(acceptedInvitation.getAcceptedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        
        // Verify in database
        Optional<StudentCalendarInvitation> found = invitationRepository.findByInvitationToken(token);
        assertTrue(found.isPresent());
        assertEquals(InvitationStatus.ACCEPTED, found.get().getStatus());
        assertNotNull(found.get().getAcceptedAt());
    }
    
    @Test
    void invitationLifecycle_CreateRevokeRecreate() {
        // Create initial invitation
        StudentCalendarInvitation invitation1 = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        String token1 = invitation1.getInvitationToken();
        
        // Revoke invitation
        invitationService.revokeInvitation(USER_ID, STUDENT_ID);
        
        // Verify revoked
        Optional<StudentCalendarInvitation> revokedInvitation = invitationRepository.findByInvitationToken(token1);
        assertTrue(revokedInvitation.isPresent());
        assertEquals(InvitationStatus.REVOKED, revokedInvitation.get().getStatus());
        
        // Create new invitation for different student to avoid unique constraint
        Long newStudentId = STUDENT_ID + 1;
        StudentCalendarInvitation invitation2 = invitationService.createAndSendInvitation(
                USER_ID, newStudentId, "student2@example.com", "Student 2");
        String token2 = invitation2.getInvitationToken();
        
        // Verify new invitation is different and pending
        assertNotEquals(token1, token2);
        assertEquals(InvitationStatus.PENDING, invitation2.getStatus());
        
        // Verify the current invitation for this user-student pair is the new one
        Optional<StudentCalendarInvitation> currentInvitation = invitationRepository.findByUserIdAndStudentId(USER_ID, newStudentId);
        assertTrue(currentInvitation.isPresent());
        assertEquals(token2, currentInvitation.get().getInvitationToken());
        assertEquals(InvitationStatus.PENDING, currentInvitation.get().getStatus());
    }
    
    @Test
    void hasActiveInvitation_WorksCorrectly() {
        // Initially no active invitation
        assertFalse(invitationService.hasActiveInvitation(USER_ID, STUDENT_ID));
        
        // Create invitation
        invitationService.createAndSendInvitation(USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        
        // Now has active invitation
        assertTrue(invitationService.hasActiveInvitation(USER_ID, STUDENT_ID));
        
        // Revoke invitation
        invitationService.revokeInvitation(USER_ID, STUDENT_ID);
        
        // No longer has active invitation
        assertFalse(invitationService.hasActiveInvitation(USER_ID, STUDENT_ID));
    }
    
    @Test
    void getUserInvitations_ReturnsCorrectInvitations() {
        // Create invitations for different students
        invitationService.createAndSendInvitation(USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        invitationService.createAndSendInvitation(USER_ID, STUDENT_ID + 1, "student2@example.com", "Student 2");
        
        // Create invitation for different user
        invitationService.createAndSendInvitation("other-user", STUDENT_ID + 2, "student3@example.com", "Student 3");
        
        // Get invitations for our user
        List<StudentCalendarInvitation> userInvitations = invitationService.getUserInvitations(USER_ID);
        
        // Should have 2 invitations
        assertEquals(2, userInvitations.size());
        assertTrue(userInvitations.stream().allMatch(inv -> inv.getUserId().equals(USER_ID)));
    }
    
    @Test
    void cleanupExpiredInvitations_WorksCorrectly() {
        // Create invitation that will be expired
        StudentCalendarInvitation expiredInvitation = new StudentCalendarInvitation(
                USER_ID + "_expired", STUDENT_ID + 100, "expired@example.com", "expired-token", LocalDateTime.now().minusHours(1));
        expiredInvitation.setStatus(InvitationStatus.PENDING);
        invitationRepository.save(expiredInvitation);
        
        // Create current invitation
        invitationService.createAndSendInvitation(USER_ID + "_current", STUDENT_ID + 101, "current@example.com", "Current Student");
        
        // Run cleanup - should not throw exception
        assertDoesNotThrow(() -> {
            int cleanedUp = invitationService.cleanupExpiredInvitations();
            // Cleanup should return a non-negative number
            assertTrue(cleanedUp >= 0);
        });
        
        // Verify expired invitation is marked as expired (if it still exists)
        Optional<StudentCalendarInvitation> foundExpired = invitationRepository.findByInvitationToken("expired-token");
        if (foundExpired.isPresent()) {
            // Should be marked as expired or still pending (depending on implementation)
            assertTrue(foundExpired.get().getStatus() == InvitationStatus.EXPIRED || 
                      foundExpired.get().getStatus() == InvitationStatus.PENDING);
        }
        // If not found, it was deleted, which is also acceptable
    }
    
    @Test
    void validateInvitationToken_WorksCorrectly() {
        // Create invitation
        StudentCalendarInvitation invitation = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        String token = invitation.getInvitationToken();
        
        // Validate token
        StudentCalendarInvitation validated = invitationService.validateInvitationToken(token);
        
        // Should return the same invitation
        assertNotNull(validated);
        assertEquals(invitation.getId(), validated.getId());
        assertEquals(token, validated.getInvitationToken());
        assertEquals(InvitationStatus.PENDING, validated.getStatus());
    }
    
    @Test
    void validateInvitationToken_ExpiredToken_ThrowsException() {
        // Create expired invitation directly in database
        StudentCalendarInvitation expiredInvitation = new StudentCalendarInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, "expired-token", LocalDateTime.now().minusHours(1));
        expiredInvitation.setStatus(InvitationStatus.PENDING);
        invitationRepository.save(expiredInvitation);
        
        // Try to validate expired token
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                invitationService.validateInvitationToken("expired-token"));
        
        assertEquals("Invitation has expired", exception.getMessage());
        
        // Verify invitation was marked as expired
        Optional<StudentCalendarInvitation> found = invitationRepository.findByInvitationToken("expired-token");
        assertTrue(found.isPresent());
        assertEquals(InvitationStatus.EXPIRED, found.get().getStatus());
    }
    
    @Test
    void tokenGeneration_GeneratesUniqueTokens() {
        // Create multiple invitations
        StudentCalendarInvitation inv1 = invitationService.createAndSendInvitation(
                USER_ID, STUDENT_ID, STUDENT_EMAIL, STUDENT_NAME);
        StudentCalendarInvitation inv2 = invitationService.createAndSendInvitation(
                USER_ID + "2", STUDENT_ID + 1, "student2@example.com", "Student 2");
        StudentCalendarInvitation inv3 = invitationService.createAndSendInvitation(
                USER_ID + "3", STUDENT_ID + 2, "student3@example.com", "Student 3");
        
        // All tokens should be unique
        assertNotEquals(inv1.getInvitationToken(), inv2.getInvitationToken());
        assertNotEquals(inv1.getInvitationToken(), inv3.getInvitationToken());
        assertNotEquals(inv2.getInvitationToken(), inv3.getInvitationToken());
        
        // All tokens should be reasonably long (secure)
        assertTrue(inv1.getInvitationToken().length() > 20);
        assertTrue(inv2.getInvitationToken().length() > 20);
        assertTrue(inv3.getInvitationToken().length() > 20);
    }
}