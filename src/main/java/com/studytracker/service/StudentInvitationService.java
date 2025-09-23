package com.studytracker.service;

import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.repository.StudentCalendarInvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing student calendar invitations
 */
@Service
@Transactional
public class StudentInvitationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentInvitationService.class);
    private static final int TOKEN_LENGTH = 32; // 256 bits
    private static final int DEFAULT_EXPIRY_HOURS = 72; // 3 days
    
    private final StudentCalendarInvitationRepository invitationRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.invitation.expiry-hours:72}")
    private int invitationExpiryHours;
    
    @Autowired
    public StudentInvitationService(StudentCalendarInvitationRepository invitationRepository,
                                  EmailService emailService) {
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Create and send a calendar sync invitation to a student
     */
    public StudentCalendarInvitation createAndSendInvitation(String userId, Long studentId, 
                                                           String studentEmail, String studentName) {
        logger.info("Creating invitation for user {} to student {} ({})", userId, studentId, studentEmail);
        
        // Check if active invitation already exists
        if (hasActiveInvitation(userId, studentId)) {
            throw new IllegalStateException("Active invitation already exists for this student");
        }
        
        // Delete any existing invitations for this user-student pair to avoid unique constraint violation
        deleteExistingInvitations(userId, studentId);
        
        // Generate secure token
        String invitationToken = generateSecureToken();
        
        // Calculate expiry time
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(invitationExpiryHours);
        
        // Create invitation
        StudentCalendarInvitation invitation = new StudentCalendarInvitation(
            userId, studentId, studentEmail, invitationToken, expiresAt
        );
        
        // Save invitation
        invitation = invitationRepository.save(invitation);
        
        // Send email invitation
        try {
            String invitationUrl = buildInvitationUrl(invitationToken);
            emailService.sendCalendarInvitation(studentEmail, studentName, invitationUrl, expiresAt);
            logger.info("Invitation email sent successfully to {}", studentEmail);
        } catch (Exception e) {
            logger.error("Failed to send invitation email to {}: {}", studentEmail, e.getMessage());
            // Don't fail the invitation creation if email fails
        }
        
        return invitation;
    }
    
    /**
     * Validate and accept an invitation using the token
     */
    public StudentCalendarInvitation acceptInvitation(String invitationToken) {
        logger.info("Attempting to accept invitation with token: {}", 
                   invitationToken.substring(0, Math.min(8, invitationToken.length())) + "...");
        
        Optional<StudentCalendarInvitation> invitationOpt = invitationRepository.findByInvitationToken(invitationToken);
        
        if (invitationOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid invitation token");
        }
        
        StudentCalendarInvitation invitation = invitationOpt.get();
        
        // Validate invitation
        validateInvitationForAcceptance(invitation);
        
        // Accept invitation
        invitation.accept();
        invitation = invitationRepository.save(invitation);
        
        logger.info("Invitation accepted successfully for user {} and student {}", 
                   invitation.getUserId(), invitation.getStudentId());
        
        return invitation;
    }
    
    /**
     * Validate invitation token and return invitation details
     */
    public StudentCalendarInvitation validateInvitationToken(String invitationToken) {
        Optional<StudentCalendarInvitation> invitationOpt = invitationRepository.findByInvitationToken(invitationToken);
        
        if (invitationOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid invitation token");
        }
        
        StudentCalendarInvitation invitation = invitationOpt.get();
        validateInvitationForAcceptance(invitation);
        
        return invitation;
    }
    
    /**
     * Revoke an invitation
     */
    public void revokeInvitation(String userId, Long studentId) {
        logger.info("Revoking invitation for user {} and student {}", userId, studentId);
        
        Optional<StudentCalendarInvitation> invitationOpt = 
            invitationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (invitationOpt.isPresent()) {
            StudentCalendarInvitation invitation = invitationOpt.get();
            if (invitation.getStatus() == InvitationStatus.PENDING) {
                invitation.revoke();
                invitationRepository.save(invitation);
                logger.info("Invitation revoked successfully");
            } else {
                logger.info("Invitation already in status: {}", invitation.getStatus());
            }
        } else {
            logger.warn("No invitation found to revoke for user {} and student {}", userId, studentId);
        }
    }
    
    /**
     * Revoke invitation by token
     */
    public void revokeInvitationByToken(String invitationToken) {
        logger.info("Revoking invitation by token");
        
        Optional<StudentCalendarInvitation> invitationOpt = 
            invitationRepository.findByInvitationToken(invitationToken);
        
        if (invitationOpt.isPresent()) {
            StudentCalendarInvitation invitation = invitationOpt.get();
            invitation.revoke();
            invitationRepository.save(invitation);
            logger.info("Invitation revoked successfully");
        } else {
            logger.warn("No invitation found to revoke with provided token");
        }
    }
    
    /**
     * Get invitation status
     */
    public Optional<StudentCalendarInvitation> getInvitation(String userId, Long studentId) {
        return invitationRepository.findByUserIdAndStudentId(userId, studentId);
    }
    
    /**
     * Get invitation by token
     */
    public Optional<StudentCalendarInvitation> getInvitationByToken(String invitationToken) {
        return invitationRepository.findByInvitationToken(invitationToken);
    }
    
    /**
     * Check if user has active invitation for student
     */
    public boolean hasActiveInvitation(String userId, Long studentId) {
        return invitationRepository.existsActiveInvitation(
            userId, studentId, InvitationStatus.PENDING, LocalDateTime.now()
        );
    }
    
    /**
     * Get all invitations for a user
     */
    public List<StudentCalendarInvitation> getUserInvitations(String userId) {
        return invitationRepository.findByUserId(userId);
    }
    
    /**
     * Clean up expired invitations
     */
    @Transactional
    public int cleanupExpiredInvitations() {
        logger.info("Starting cleanup of expired invitations");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Mark expired invitations as expired
        int markedExpired = invitationRepository.markExpiredInvitations(
            InvitationStatus.PENDING, InvitationStatus.EXPIRED, now
        );
        
        logger.info("Marked {} invitations as expired", markedExpired);
        
        // Optionally delete very old expired invitations (older than 30 days)
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        int deleted = invitationRepository.deleteExpiredInvitations(thirtyDaysAgo);
        
        logger.info("Deleted {} old expired invitations", deleted);
        
        return markedExpired + deleted;
    }
    
    /**
     * Generate a cryptographically secure invitation token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Build invitation URL from token
     */
    private String buildInvitationUrl(String invitationToken) {
        return String.format("%s/calendar/invitation/accept?token=%s", baseUrl, invitationToken);
    }
    
    /**
     * Delete existing invitations for user-student pair to avoid unique constraint violation
     */
    private void deleteExistingInvitations(String userId, Long studentId) {
        Optional<StudentCalendarInvitation> existingInvitation = 
            invitationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (existingInvitation.isPresent()) {
            invitationRepository.delete(existingInvitation.get());
            logger.info("Deleted existing invitation to create new one");
        }
    }
    
    /**
     * Validate invitation for acceptance
     */
    private void validateInvitationForAcceptance(StudentCalendarInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not in pending status: " + invitation.getStatus());
        }
        
        if (invitation.isExpired()) {
            // Mark as expired
            invitation.expire();
            invitationRepository.save(invitation);
            throw new IllegalStateException("Invitation has expired");
        }
    }
}