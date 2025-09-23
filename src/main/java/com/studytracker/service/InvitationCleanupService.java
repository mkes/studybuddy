package com.studytracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup of expired invitations
 */
@Service
public class InvitationCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvitationCleanupService.class);
    
    private final StudentInvitationService invitationService;
    
    @Autowired
    public InvitationCleanupService(StudentInvitationService invitationService) {
        this.invitationService = invitationService;
    }
    
    /**
     * Clean up expired invitations every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredInvitations() {
        logger.debug("Starting scheduled cleanup of expired invitations");
        
        try {
            int cleanedUp = invitationService.cleanupExpiredInvitations();
            
            if (cleanedUp > 0) {
                logger.info("Cleaned up {} expired invitations", cleanedUp);
            } else {
                logger.debug("No expired invitations to clean up");
            }
            
        } catch (Exception e) {
            logger.error("Error during invitation cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send reminder emails for invitations expiring soon (runs daily at 9 AM)
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpirationReminders() {
        logger.info("Starting scheduled reminder emails for expiring invitations");
        
        try {
            // This would be implemented when we add reminder functionality
            // For now, just log that the job ran
            logger.info("Expiration reminder job completed");
            
        } catch (Exception e) {
            logger.error("Error during expiration reminder job: {}", e.getMessage(), e);
        }
    }
}