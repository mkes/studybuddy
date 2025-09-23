package com.studytracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a");
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.from:noreply@studytracker.com}")
    private String fromEmail;
    
    @Value("${app.name:StudyTracker}")
    private String appName;
    
    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send calendar sync invitation email to student
     */
    public void sendCalendarInvitation(String studentEmail, String studentName, 
                                     String invitationUrl, LocalDateTime expiresAt) {
        logger.info("Sending calendar invitation email to: {}", studentEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(studentEmail);
            message.setSubject("Calendar Sync Invitation - " + appName);
            message.setText(buildInvitationEmailBody(studentName, invitationUrl, expiresAt));
            
            mailSender.send(message);
            logger.info("Calendar invitation email sent successfully to: {}", studentEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send calendar invitation email to {}: {}", studentEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }
    
    /**
     * Send invitation reminder email
     */
    public void sendInvitationReminder(String studentEmail, String studentName, 
                                     String invitationUrl, LocalDateTime expiresAt) {
        logger.info("Sending invitation reminder email to: {}", studentEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(studentEmail);
            message.setSubject("Reminder: Calendar Sync Invitation - " + appName);
            message.setText(buildReminderEmailBody(studentName, invitationUrl, expiresAt));
            
            mailSender.send(message);
            logger.info("Invitation reminder email sent successfully to: {}", studentEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send invitation reminder email to {}: {}", studentEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send reminder email", e);
        }
    }
    
    /**
     * Build invitation email body
     */
    private String buildInvitationEmailBody(String studentName, String invitationUrl, LocalDateTime expiresAt) {
        StringBuilder body = new StringBuilder();
        
        body.append("Hello ").append(studentName != null ? studentName : "").append(",\n\n");
        
        body.append("You've been invited to sync your academic calendar with ").append(appName).append("!\n\n");
        
        body.append("Your parent/guardian has set up calendar integration to help keep track of your assignments and due dates. ");
        body.append("By accepting this invitation, your assignments will be automatically synced to your Google Calendar ");
        body.append("with helpful reminders.\n\n");
        
        body.append("To accept this invitation and connect your Google Calendar, please click the link below:\n\n");
        body.append(invitationUrl).append("\n\n");
        
        body.append("This invitation will expire on ").append(expiresAt.format(DATE_FORMATTER)).append(".\n\n");
        
        body.append("Benefits of calendar sync:\n");
        body.append("• Automatic reminders for upcoming assignments\n");
        body.append("• Never miss a due date again\n");
        body.append("• Better organization of your academic schedule\n");
        body.append("• Seamless integration with your existing calendar\n\n");
        
        body.append("If you have any questions or concerns, please contact your parent/guardian.\n\n");
        
        body.append("Best regards,\n");
        body.append("The ").append(appName).append(" Team\n\n");
        
        body.append("---\n");
        body.append("This is an automated message. Please do not reply to this email.\n");
        body.append("If you did not expect this invitation, please ignore this email.");
        
        return body.toString();
    }
    
    /**
     * Build reminder email body
     */
    private String buildReminderEmailBody(String studentName, String invitationUrl, LocalDateTime expiresAt) {
        StringBuilder body = new StringBuilder();
        
        body.append("Hello ").append(studentName != null ? studentName : "").append(",\n\n");
        
        body.append("This is a friendly reminder that you have a pending calendar sync invitation for ").append(appName).append(".\n\n");
        
        body.append("Your parent/guardian is waiting for you to connect your Google Calendar so that your assignments ");
        body.append("can be automatically synced with helpful reminders.\n\n");
        
        body.append("To accept this invitation, please click the link below:\n\n");
        body.append(invitationUrl).append("\n\n");
        
        body.append("⚠️ This invitation will expire on ").append(expiresAt.format(DATE_FORMATTER)).append(".\n\n");
        
        body.append("Don't miss out on:\n");
        body.append("• Automatic assignment reminders\n");
        body.append("• Better organization of your academic schedule\n");
        body.append("• Seamless calendar integration\n\n");
        
        body.append("If you have any questions, please contact your parent/guardian.\n\n");
        
        body.append("Best regards,\n");
        body.append("The ").append(appName).append(" Team\n\n");
        
        body.append("---\n");
        body.append("This is an automated message. Please do not reply to this email.");
        
        return body.toString();
    }
}