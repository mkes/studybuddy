package com.studytracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mock email service for testing when SMTP is not configured
 */
@Service
@ConditionalOnProperty(name = "app.email.mock", havingValue = "true", matchIfMissing = false)
public class MockEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a");
    
    @Value("${spring.mail.from:noreply@studytracker.com}")
    private String fromEmail;
    
    @Value("${app.name:StudyTracker}")
    private String appName;
    
    /**
     * Mock send calendar invitation email
     */
    public void sendCalendarInvitation(String studentEmail, String studentName, 
                                     String invitationUrl, LocalDateTime expiresAt) {
        logger.info("=== MOCK EMAIL SERVICE ===");
        logger.info("Would send calendar invitation email to: {}", studentEmail);
        logger.info("From: {}", fromEmail);
        logger.info("Subject: Calendar Sync Invitation - {}", appName);
        logger.info("Invitation URL: {}", invitationUrl);
        logger.info("Expires: {}", expiresAt.format(DATE_FORMATTER));
        logger.info("=== END MOCK EMAIL ===");
    }
    
    /**
     * Mock send generic email
     */
    public void sendEmail(String toEmail, String subject, String body) {
        logger.info("=== MOCK EMAIL SERVICE ===");
        logger.info("Would send email to: {}", toEmail);
        logger.info("From: {}", fromEmail);
        logger.info("Subject: {}", subject);
        logger.info("Body preview: {}", body.length() > 100 ? body.substring(0, 100) + "..." : body);
        logger.info("=== END MOCK EMAIL ===");
    }
    
    /**
     * Mock send invitation reminder email
     */
    public void sendInvitationReminder(String studentEmail, String studentName, 
                                     String invitationUrl, LocalDateTime expiresAt) {
        logger.info("=== MOCK EMAIL SERVICE ===");
        logger.info("Would send invitation reminder email to: {}", studentEmail);
        logger.info("From: {}", fromEmail);
        logger.info("Subject: Reminder: Calendar Sync Invitation - {}", appName);
        logger.info("Invitation URL: {}", invitationUrl);
        logger.info("Expires: {}", expiresAt.format(DATE_FORMATTER));
        logger.info("=== END MOCK EMAIL ===");
    }
}