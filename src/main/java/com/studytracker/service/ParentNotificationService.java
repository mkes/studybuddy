package com.studytracker.service;

import com.studytracker.dto.ParentNotificationSettingsDto;
import com.studytracker.model.ParentNotificationSettings;
import com.studytracker.model.PlannerItem;
import com.studytracker.repository.ParentNotificationSettingsRepository;
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
 * Service for managing parent notification settings and sending notifications
 */
@Service
@Transactional
public class ParentNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ParentNotificationService.class);
    
    private final ParentNotificationSettingsRepository notificationRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Value("${app.invitation.expiry-hours:72}")
    private int verificationExpiryHours;
    
    @Autowired
    public ParentNotificationService(ParentNotificationSettingsRepository notificationRepository,
                                   EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Set up parent notification settings
     */
    public ParentNotificationSettings setupParentNotifications(String userId, Long studentId, 
                                                             ParentNotificationSettingsDto settingsDto) {
        logger.info("Setting up parent notifications for user {} student {} email {}", 
                userId, studentId, settingsDto.getParentEmail());
        
        // Check if settings already exist
        Optional<ParentNotificationSettings> existingSettings = 
                notificationRepository.findByUserIdAndStudentId(userId, studentId);
        
        ParentNotificationSettings settings;
        if (existingSettings.isPresent()) {
            settings = existingSettings.get();
            settings.setParentEmail(settingsDto.getParentEmail());
            settings.setParentName(settingsDto.getParentName());
            settings.setEmailVerified(false); // Re-verify if email changed
        } else {
            settings = new ParentNotificationSettings(userId, studentId, settingsDto.getParentEmail());
            settings.setParentName(settingsDto.getParentName());
        }
        
        // Update notification preferences
        updateNotificationPreferences(settings, settingsDto);
        
        // Generate verification token
        String verificationToken = generateVerificationToken();
        settings.setVerificationToken(verificationToken);
        settings.setVerificationExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));
        
        // Save settings
        settings = notificationRepository.save(settings);
        
        // Send verification email
        sendVerificationEmail(settings);
        
        return settings;
    }
    
    /**
     * Update existing parent notification settings
     */
    public ParentNotificationSettings updateParentNotifications(String userId, Long studentId,
                                                              ParentNotificationSettingsDto settingsDto) {
        logger.info("Updating parent notifications for user {} student {}", userId, studentId);
        
        ParentNotificationSettings settings = notificationRepository.findByUserIdAndStudentId(userId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent notification settings not found"));
        
        // Check if email changed
        boolean emailChanged = !settings.getParentEmail().equals(settingsDto.getParentEmail());
        
        if (emailChanged) {
            settings.setParentEmail(settingsDto.getParentEmail());
            settings.setEmailVerified(false);
            
            // Generate new verification token
            String verificationToken = generateVerificationToken();
            settings.setVerificationToken(verificationToken);
            settings.setVerificationExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));
        }
        
        settings.setParentName(settingsDto.getParentName());
        updateNotificationPreferences(settings, settingsDto);
        
        settings = notificationRepository.save(settings);
        
        // Send verification email if email changed
        if (emailChanged) {
            sendVerificationEmail(settings);
        }
        
        return settings;
    }
    
    /**
     * Verify parent email address
     */
    public boolean verifyParentEmail(String verificationToken) {
        logger.info("Verifying parent email with token: {}", verificationToken);
        
        Optional<ParentNotificationSettings> settingsOpt = 
                notificationRepository.findByVerificationToken(verificationToken);
        
        if (settingsOpt.isEmpty()) {
            logger.warn("Invalid verification token: {}", verificationToken);
            return false;
        }
        
        ParentNotificationSettings settings = settingsOpt.get();
        
        if (settings.isVerificationExpired()) {
            logger.warn("Verification token expired for email: {}", settings.getParentEmail());
            return false;
        }
        
        // Mark as verified
        settings.setEmailVerified(true);
        settings.setVerificationToken(null);
        settings.setVerificationExpiresAt(null);
        notificationRepository.save(settings);
        
        logger.info("Successfully verified parent email: {}", settings.getParentEmail());
        return true;
    }
    
    /**
     * Get parent notification settings
     */
    public Optional<ParentNotificationSettings> getParentNotificationSettings(String userId, Long studentId) {
        return notificationRepository.findByUserIdAndStudentId(userId, studentId);
    }
    
    /**
     * Get parent notification settings as DTO
     */
    public Optional<ParentNotificationSettingsDto> getParentNotificationSettingsDto(String userId, Long studentId) {
        return getParentNotificationSettings(userId, studentId)
                .map(this::convertToDto);
    }
    
    /**
     * Delete parent notification settings
     */
    public void deleteParentNotificationSettings(String userId, Long studentId) {
        logger.info("Deleting parent notification settings for user {} student {}", userId, studentId);
        notificationRepository.deleteByUserIdAndStudentId(userId, studentId);
    }
    
    /**
     * Send assignment due notification to parent
     */
    public void sendAssignmentDueNotification(String userId, Long studentId, PlannerItem assignment) {
        Optional<ParentNotificationSettings> settingsOpt = 
                notificationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (settingsOpt.isEmpty() || !settingsOpt.get().getEmailVerified() || 
            !settingsOpt.get().getNotifyAssignmentDue()) {
            return;
        }
        
        ParentNotificationSettings settings = settingsOpt.get();
        
        try {
            String subject = "Assignment Due Reminder - " + assignment.getAssignmentTitle();
            String body = buildAssignmentDueEmailBody(settings, assignment);
            
            emailService.sendEmail(settings.getParentEmail(), subject, body);
            logger.info("Sent assignment due notification to parent: {}", settings.getParentEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send assignment due notification to parent: {}", 
                    settings.getParentEmail(), e);
        }
    }
    
    /**
     * Send assignment missing notification to parent
     */
    public void sendAssignmentMissingNotification(String userId, Long studentId, PlannerItem assignment) {
        Optional<ParentNotificationSettings> settingsOpt = 
                notificationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (settingsOpt.isEmpty() || !settingsOpt.get().getEmailVerified() || 
            !settingsOpt.get().getNotifyAssignmentMissing()) {
            return;
        }
        
        ParentNotificationSettings settings = settingsOpt.get();
        
        try {
            String subject = "Missing Assignment Alert - " + assignment.getAssignmentTitle();
            String body = buildAssignmentMissingEmailBody(settings, assignment);
            
            emailService.sendEmail(settings.getParentEmail(), subject, body);
            logger.info("Sent assignment missing notification to parent: {}", settings.getParentEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send assignment missing notification to parent: {}", 
                    settings.getParentEmail(), e);
        }
    }
    
    /**
     * Send assignment graded notification to parent
     */
    public void sendAssignmentGradedNotification(String userId, Long studentId, PlannerItem assignment) {
        Optional<ParentNotificationSettings> settingsOpt = 
                notificationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (settingsOpt.isEmpty() || !settingsOpt.get().getEmailVerified() || 
            !settingsOpt.get().getNotifyAssignmentGraded()) {
            return;
        }
        
        ParentNotificationSettings settings = settingsOpt.get();
        
        try {
            String subject = "Assignment Graded - " + assignment.getAssignmentTitle();
            String body = buildAssignmentGradedEmailBody(settings, assignment);
            
            emailService.sendEmail(settings.getParentEmail(), subject, body);
            logger.info("Sent assignment graded notification to parent: {}", settings.getParentEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send assignment graded notification to parent: {}", 
                    settings.getParentEmail(), e);
        }
    }
    
    /**
     * Send calendar sync notification to parent
     */
    public void sendCalendarSyncNotification(String userId, Long studentId, String syncMessage) {
        Optional<ParentNotificationSettings> settingsOpt = 
                notificationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (settingsOpt.isEmpty() || !settingsOpt.get().getEmailVerified() || 
            !settingsOpt.get().getNotifyCalendarSync()) {
            return;
        }
        
        ParentNotificationSettings settings = settingsOpt.get();
        
        try {
            String subject = "Calendar Sync Update - StudyTracker";
            String body = buildCalendarSyncEmailBody(settings, syncMessage);
            
            emailService.sendEmail(settings.getParentEmail(), subject, body);
            logger.info("Sent calendar sync notification to parent: {}", settings.getParentEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send calendar sync notification to parent: {}", 
                    settings.getParentEmail(), e);
        }
    }
    
    /**
     * Resend verification email
     */
    public boolean resendVerificationEmail(String userId, Long studentId) {
        Optional<ParentNotificationSettings> settingsOpt = 
                notificationRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (settingsOpt.isEmpty() || settingsOpt.get().getEmailVerified()) {
            return false;
        }
        
        ParentNotificationSettings settings = settingsOpt.get();
        
        // Generate new verification token
        String verificationToken = generateVerificationToken();
        settings.setVerificationToken(verificationToken);
        settings.setVerificationExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));
        
        notificationRepository.save(settings);
        sendVerificationEmail(settings);
        
        return true;
    }
    
    /**
     * Clean up expired verification tokens
     */
    @Transactional
    public int cleanupExpiredVerificationTokens() {
        return notificationRepository.cleanupExpiredVerificationTokens(LocalDateTime.now());
    }
    
    // Private helper methods
    
    private void updateNotificationPreferences(ParentNotificationSettings settings, 
                                             ParentNotificationSettingsDto dto) {
        settings.setNotifyAssignmentDue(dto.getNotifyAssignmentDue());
        settings.setNotifyAssignmentMissing(dto.getNotifyAssignmentMissing());
        settings.setNotifyAssignmentGraded(dto.getNotifyAssignmentGraded());
        settings.setNotifyCalendarSync(dto.getNotifyCalendarSync());
        settings.setDailySummaryEnabled(dto.getDailySummaryEnabled());
        settings.setDailySummaryTime(dto.getDailySummaryTime());
        settings.setWeeklySummaryEnabled(dto.getWeeklySummaryEnabled());
        settings.setWeeklySummaryDay(dto.getWeeklySummaryDay());
    }
    
    private ParentNotificationSettingsDto convertToDto(ParentNotificationSettings settings) {
        return new ParentNotificationSettingsDto(
                settings.getParentEmail(),
                settings.getParentName(),
                settings.getNotifyAssignmentDue(),
                settings.getNotifyAssignmentMissing(),
                settings.getNotifyAssignmentGraded(),
                settings.getNotifyCalendarSync(),
                settings.getDailySummaryEnabled(),
                settings.getDailySummaryTime(),
                settings.getWeeklySummaryEnabled(),
                settings.getWeeklySummaryDay()
        );
    }
    
    private String generateVerificationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    private void sendVerificationEmail(ParentNotificationSettings settings) {
        try {
            String verificationUrl = baseUrl + "/verify-parent-email?token=" + settings.getVerificationToken();
            
            String subject = "Verify Your Email - StudyTracker Parent Notifications";
            String body = buildVerificationEmailBody(settings, verificationUrl);
            
            emailService.sendEmail(settings.getParentEmail(), subject, body);
            logger.info("Sent verification email to parent: {}", settings.getParentEmail());
            
        } catch (Exception e) {
            logger.error("Failed to send verification email to parent: {}", settings.getParentEmail(), e);
        }
    }
    
    private String buildVerificationEmailBody(ParentNotificationSettings settings, String verificationUrl) {
        return String.format("""
                Dear %s,
                
                Thank you for setting up parent notifications for StudyTracker!
                
                To start receiving notifications about your student's assignments and calendar sync activities, 
                please verify your email address by clicking the link below:
                
                %s
                
                This verification link will expire in %d hours.
                
                If you didn't request this, please ignore this email.
                
                Best regards,
                StudyTracker Team
                """, 
                settings.getParentName() != null ? settings.getParentName() : "Parent",
                verificationUrl,
                verificationExpiryHours);
    }
    
    private String buildAssignmentDueEmailBody(ParentNotificationSettings settings, PlannerItem assignment) {
        return String.format("""
                Dear %s,
                
                This is a reminder that your student has an assignment due soon:
                
                Assignment: %s
                Course: %s
                Due Date: %s
                Points Possible: %s
                
                Please help your student stay on track with their assignments.
                
                Best regards,
                StudyTracker Team
                """,
                settings.getParentName() != null ? settings.getParentName() : "Parent",
                assignment.getAssignmentTitle(),
                assignment.getContextName() != null ? assignment.getContextName() : "Unknown Course",
                assignment.getDueAt() != null ? assignment.getDueAt().toString() : "Not specified",
                assignment.getPointsPossible() != null ? assignment.getPointsPossible().toString() : "Not specified");
    }
    
    private String buildAssignmentMissingEmailBody(ParentNotificationSettings settings, PlannerItem assignment) {
        return String.format("""
                Dear %s,
                
                IMPORTANT: Your student has a missing assignment that needs attention:
                
                Assignment: %s
                Course: %s
                Due Date: %s
                Points Possible: %s
                
                Please contact your student or their teacher to address this missing assignment.
                
                Best regards,
                StudyTracker Team
                """,
                settings.getParentName() != null ? settings.getParentName() : "Parent",
                assignment.getAssignmentTitle(),
                assignment.getContextName() != null ? assignment.getContextName() : "Unknown Course",
                assignment.getDueAt() != null ? assignment.getDueAt().toString() : "Not specified",
                assignment.getPointsPossible() != null ? assignment.getPointsPossible().toString() : "Not specified");
    }
    
    private String buildAssignmentGradedEmailBody(ParentNotificationSettings settings, PlannerItem assignment) {
        return String.format("""
                Dear %s,
                
                Good news! Your student's assignment has been graded:
                
                Assignment: %s
                Course: %s
                Grade: %s
                Points Possible: %s
                
                Keep up the great work!
                
                Best regards,
                StudyTracker Team
                """,
                settings.getParentName() != null ? settings.getParentName() : "Parent",
                assignment.getAssignmentTitle(),
                assignment.getContextName() != null ? assignment.getContextName() : "Unknown Course",
                assignment.getCurrentGrade() != null ? assignment.getCurrentGrade().toString() : "Not available",
                assignment.getPointsPossible() != null ? assignment.getPointsPossible().toString() : "Not specified");
    }
    
    private String buildCalendarSyncEmailBody(ParentNotificationSettings settings, String syncMessage) {
        return String.format("""
                Dear %s,
                
                Your student's calendar has been synchronized with the latest assignments:
                
                %s
                
                You can view the updated calendar in your Google Calendar if you have calendar integration enabled.
                
                Best regards,
                StudyTracker Team
                """,
                settings.getParentName() != null ? settings.getParentName() : "Parent",
                syncMessage);
    }
}