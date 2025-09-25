package com.studytracker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.dto.ParentNotificationSettingsDto;
import com.studytracker.dto.StudentDto;
import com.studytracker.exception.GoogleCalendarException;
import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarSyncSettings;
import com.studytracker.model.ParentNotificationSettings;
import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.*;

/**
 * Controller for Google Calendar integration functionality.
 * Handles OAuth flows, calendar connections, sync settings, and student invitations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CalendarController {
    
    private final GoogleCalendarService googleCalendarService;
    private final CalendarTokenService tokenService;
    private final CalendarSyncService syncService;
    private final StudentInvitationService invitationService;
    private final CanvasApiService canvasApiService;
    private final CalendarSyncSettingsService syncSettingsService;
    private final ParentNotificationService parentNotificationService;
    private final ObjectMapper objectMapper;
    

    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Display calendar integration page with connection status for both accounts
     */
    @GetMapping("/students/{studentId}/calendar")
    public String calendarIntegrationPage(@PathVariable Long studentId,
                                        HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            log.warn("Unauthenticated access attempt to calendar integration page");
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            // Get student information
            List<StudentDto> students = canvasApiService.getObservedStudents(canvasToken);
            StudentDto currentStudent = students.stream()
                    .filter(student -> student.getId().equals(studentId))
                    .findFirst()
                    .orElse(null);
            
            if (currentStudent == null) {
                redirectAttributes.addFlashAttribute("error", "Student not found or access denied.");
                return "redirect:/students";
            }
            
            // Get calendar connection status
            CalendarTokenService.CalendarConnectionStatus connectionStatus = 
                    tokenService.getConnectionStatus(userId, studentId);
            
            // Get sync settings
            CalendarSyncSettings syncSettings = getSyncSettings(userId, studentId);
            
            // Get invitation status
            Optional<StudentCalendarInvitation> invitation = 
                    invitationService.getInvitation(userId, studentId);
            
            // Get parent notification settings
            Optional<ParentNotificationSettings> parentNotificationSettings = 
                    parentNotificationService.getParentNotificationSettings(userId, studentId);
            
            // Add model attributes
            model.addAttribute("studentId", studentId);
            model.addAttribute("student", currentStudent);
            model.addAttribute("parentConnected", connectionStatus.isParentConnected());
            model.addAttribute("studentConnected", connectionStatus.isStudentConnected());
            model.addAttribute("syncSettings", syncSettings);
            model.addAttribute("invitation", invitation.orElse(null));
            model.addAttribute("hasActiveInvitation", 
                    invitation.isPresent() && invitation.get().isPending());
            model.addAttribute("parentNotificationSettings", parentNotificationSettings.orElse(null));
            model.addAttribute("hasParentNotifications", parentNotificationSettings.isPresent());
            model.addAttribute("parentEmailVerified", 
                    parentNotificationSettings.map(ParentNotificationSettings::getEmailVerified).orElse(false));
            
            // Parse JSON settings for display
            model.addAttribute("includedCoursesList", parseJsonStringList(syncSettings.getIncludedCourses()));
            model.addAttribute("excludedTypesList", parseJsonStringList(syncSettings.getExcludedAssignmentTypes()));
            model.addAttribute("parentRemindersList", parseJsonIntegerList(syncSettings.getParentReminderMinutes()));
            model.addAttribute("studentRemindersList", parseJsonIntegerList(syncSettings.getStudentReminderMinutes()));
            
            return "calendar-integration";
            
        } catch (Exception e) {
            log.error("Error loading calendar integration page for student {}", studentId, e);
            model.addAttribute("error", "Unable to load calendar integration page. Please try again later.");
            model.addAttribute("studentId", studentId);
            return "calendar-integration";
        }
    }
    
    /**
     * Initiate parent Google OAuth flow
     */
    @GetMapping("/students/{studentId}/calendar/connect/parent")
    public String initiateParentOAuth(@PathVariable Long studentId,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        try {
            String userId = getUserId(session);
            String state = generateStateParameter(userId, studentId, AccountType.PARENT);
            
            // Store state in session for validation
            session.setAttribute("oauth_state", state);
            session.setAttribute("oauth_account_type", AccountType.PARENT.name());
            session.setAttribute("oauth_student_id", studentId);
            
            String authUrl = googleCalendarService.initiateOAuthFlow(AccountType.PARENT, state);
            
            log.info("Initiating parent OAuth flow for user {} student {}", userId, studentId);
            return "redirect:" + authUrl;
            
        } catch (Exception e) {
            log.error("Failed to initiate parent OAuth flow for student {}", studentId, e);
            
            // Check if it's a configuration issue
            if (e.getMessage() != null && e.getMessage().contains("not configured")) {
                redirectAttributes.addFlashAttribute("error", 
                    "Google Calendar is not configured. Please complete the setup first.");
                redirectAttributes.addAttribute("studentId", studentId);
                return "redirect:/setup";
            }
            
            redirectAttributes.addFlashAttribute("error", "Failed to connect to Google Calendar. Please try again.");
            return "redirect:/students/" + studentId + "/calendar";
        }
    }
    
    /**
     * Initiate student Google OAuth flow (from invitation)
     */
    @GetMapping("/students/{studentId}/calendar/connect/student")
    public String initiateStudentOAuth(@PathVariable Long studentId,
                                     @RequestParam String token,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            // Validate invitation token
            StudentCalendarInvitation invitation = invitationService.validateInvitationToken(token);
            
            if (!invitation.getStudentId().equals(studentId)) {
                redirectAttributes.addFlashAttribute("error", "Invalid invitation token.");
                return "redirect:/";
            }
            
            String userId = invitation.getUserId();
            String state = generateStateParameter(userId, studentId, AccountType.STUDENT);
            
            // Store state and invitation info in session
            session.setAttribute("oauth_state", state);
            session.setAttribute("oauth_account_type", AccountType.STUDENT.name());
            session.setAttribute("oauth_student_id", studentId);
            session.setAttribute("oauth_user_id", userId);
            session.setAttribute("invitation_token", token);
            
            String authUrl = googleCalendarService.initiateOAuthFlow(AccountType.STUDENT, state);
            
            log.info("Initiating student OAuth flow for user {} student {}", userId, studentId);
            return "redirect:" + authUrl;
            
        } catch (Exception e) {
            log.error("Failed to initiate student OAuth flow for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to connect to Google Calendar. Please try again.");
            return "redirect:/";
        }
    }
    
    /**
     * Handle OAuth callback for both parent and student accounts
     */
    @GetMapping("/auth/google/callback")
    public String handleOAuthCallback(@RequestParam(required = false) String code,
                                    @RequestParam String state,
                                    @RequestParam(required = false) String error,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Handle OAuth error
            if (error != null) {
                log.warn("OAuth error received: {}", error);
                redirectAttributes.addFlashAttribute("error", "Google Calendar authorization was denied or failed.");
                
                // Try to get student ID from session for redirect
                Long studentId = (Long) session.getAttribute("oauth_student_id");
                clearOAuthSession(session);
                
                if (studentId != null) {
                    return "redirect:/students/" + studentId + "/calendar";
                } else {
                    return "redirect:/students";
                }
            }
            
            // Validate state parameter
            String sessionState = (String) session.getAttribute("oauth_state");
            if (sessionState == null || !sessionState.equals(state)) {
                log.warn("Invalid OAuth state parameter");
                redirectAttributes.addFlashAttribute("error", "Invalid authorization request. Please try again.");
                return "redirect:/students";
            }
            
            // Get OAuth session data
            String accountTypeStr = (String) session.getAttribute("oauth_account_type");
            Long studentId = (Long) session.getAttribute("oauth_student_id");
            String userId = (String) session.getAttribute("oauth_user_id");
            
            if (accountTypeStr == null || studentId == null) {
                log.warn("Missing OAuth session data");
                redirectAttributes.addFlashAttribute("error", "Invalid authorization session. Please try again.");
                return "redirect:/students";
            }
            
            AccountType accountType = AccountType.valueOf(accountTypeStr);
            
            // For parent OAuth, get userId from current session
            if (accountType == AccountType.PARENT) {
                userId = getUserId(session);
            }
            
            // Handle OAuth callback
            GoogleCalendarService.OAuthTokenResult tokenResult = googleCalendarService.handleOAuthCallback(
                    code, state, accountType, userId, studentId);
            
            // For student OAuth, accept the invitation
            if (accountType == AccountType.STUDENT) {
                String invitationToken = (String) session.getAttribute("invitation_token");
                if (invitationToken != null) {
                    invitationService.acceptInvitation(invitationToken);
                }
            }
            
            // Clear OAuth session data
            clearOAuthSession(session);
            
            log.info("Successfully completed OAuth flow for user {} student {} account type {}", 
                    userId, studentId, accountType);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Google Calendar connected successfully for " + accountType.name().toLowerCase() + " account!");
            
            return "redirect:/students/" + studentId + "/calendar";
            
        } catch (Exception e) {
            log.error("Failed to handle OAuth callback", e);
            clearOAuthSession(session);
            redirectAttributes.addFlashAttribute("error", "Failed to connect Google Calendar. Please try again.");
            return "redirect:/students";
        }
    }
    
    /**
     * Send calendar sync invitation to student
     */
    @PostMapping("/students/{studentId}/calendar/invite-student")
    public String inviteStudent(@PathVariable Long studentId,
                               @RequestParam String studentEmail,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            // Validate email format
            if (studentEmail == null || studentEmail.trim().isEmpty() || !isValidEmail(studentEmail)) {
                redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
                return "redirect:/students/" + studentId + "/calendar";
            }
            
            // Get student name for email
            List<StudentDto> students = canvasApiService.getObservedStudents(canvasToken);
            StudentDto currentStudent = students.stream()
                    .filter(student -> student.getId().equals(studentId))
                    .findFirst()
                    .orElse(null);
            
            if (currentStudent == null) {
                redirectAttributes.addFlashAttribute("error", "Student not found.");
                return "redirect:/students/" + studentId + "/calendar";
            }
            
            // Create and send invitation
            StudentCalendarInvitation invitation = invitationService.createAndSendInvitation(
                    userId, studentId, studentEmail.trim(), currentStudent.getName());
            
            log.info("Calendar invitation sent to {} for user {} student {}", 
                    studentEmail, userId, studentId);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Calendar sync invitation sent to " + studentEmail + ". The invitation will expire in 72 hours.");
            
        } catch (IllegalStateException e) {
            log.warn("Failed to send invitation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            
        } catch (Exception e) {
            log.error("Failed to send calendar invitation for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to send invitation. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Trigger manual sync
     */
    @PostMapping("/students/{studentId}/calendar/sync")
    public ResponseEntity<Map<String, Object>> triggerManualSync(@PathVariable Long studentId,
                                                               HttpSession session) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        String userId = getUserId(session);
        
        try {
            CalendarSyncService.SyncResult result = syncService.syncStudentAssignments(userId, studentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("status", result.getStatus());
            response.put("created", result.getCreatedCount());
            response.put("updated", result.getUpdatedCount());
            response.put("deleted", result.getDeletedCount());
            response.put("errors", result.getErrors());
            
            if (result.isSuccess()) {
                response.put("message", String.format("Sync completed successfully. Created: %d, Updated: %d, Deleted: %d", 
                        result.getCreatedCount(), result.getUpdatedCount(), result.getDeletedCount()));
            } else {
                response.put("message", "Sync completed with errors. Check the error details.");
            }
            
            log.info("Manual sync triggered for user {} student {} - Result: {}", 
                    userId, studentId, result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to trigger manual sync for student {}", studentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Sync failed: " + e.getMessage(),
                    "success", false
            ));
        }
    }
    
    /**
     * Disconnect parent or student calendar
     */
    @PostMapping("/students/{studentId}/calendar/disconnect")
    public String disconnectCalendar(@PathVariable Long studentId,
                                   @RequestParam AccountType accountType,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            boolean revoked = tokenService.revokeTokens(userId, studentId, accountType);
            
            if (revoked) {
                log.info("Disconnected {} calendar for user {} student {}", 
                        accountType, userId, studentId);
                redirectAttributes.addFlashAttribute("success", 
                        accountType.name().toLowerCase() + " calendar disconnected successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to disconnect calendar.");
            }
            
        } catch (Exception e) {
            log.error("Failed to disconnect {} calendar for student {}", accountType, studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to disconnect calendar. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Update sync settings
     */
    @PostMapping("/students/{studentId}/calendar/settings")
    public String updateSyncSettings(@PathVariable Long studentId,
                                   @ModelAttribute CalendarSyncSettings syncSettings,
                                   BindingResult bindingResult,
                                   @RequestParam(required = false) List<String> includedCourses,
                                   @RequestParam(required = false) List<String> excludedAssignmentTypes,
                                   @RequestParam(required = false) List<Integer> parentReminders,
                                   @RequestParam(required = false) List<Integer> studentReminders,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Invalid settings. Please check your input.");
            return "redirect:/students/" + studentId + "/calendar";
        }
        
        String userId = getUserId(session);
        
        try {
            // Set required fields that aren't bound from form
            syncSettings.setUserId(userId);
            syncSettings.setStudentId(studentId);
            
            // Convert lists to JSON strings
            syncSettings.setIncludedCourses(listToJson(includedCourses));
            syncSettings.setExcludedAssignmentTypes(listToJson(excludedAssignmentTypes));
            syncSettings.setParentReminderMinutes(listToJson(parentReminders));
            syncSettings.setStudentReminderMinutes(listToJson(studentReminders));
            
            // Apply settings
            CalendarSyncService.SyncResult result = syncService.applySyncSettings(userId, studentId, syncSettings);
            
            if (result.isSuccess()) {
                log.info("Updated sync settings for user {} student {}", userId, studentId);
                redirectAttributes.addFlashAttribute("success", "Sync settings updated successfully.");
                
                if (result.getTotalProcessed() > 0) {
                    redirectAttributes.addFlashAttribute("info", 
                            String.format("Settings applied and %d assignments re-synced.", result.getTotalProcessed()));
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Settings updated but sync failed: " + 
                        String.join(", ", result.getErrors()));
            }
            
        } catch (Exception e) {
            log.error("Failed to update sync settings for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to update settings. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Revoke student invitation
     */
    @PostMapping("/students/{studentId}/calendar/revoke-invitation")
    public String revokeInvitation(@PathVariable Long studentId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            invitationService.revokeInvitation(userId, studentId);
            
            log.info("Revoked invitation for user {} student {}", userId, studentId);
            redirectAttributes.addFlashAttribute("success", "Student invitation revoked successfully.");
            
        } catch (Exception e) {
            log.error("Failed to revoke invitation for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to revoke invitation. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Set up parent email notifications
     */
    @PostMapping("/students/{studentId}/calendar/setup-parent-notifications")
    public String setupParentNotifications(@PathVariable Long studentId,
                                         @RequestParam String parentEmail,
                                         @RequestParam(required = false) String parentName,
                                         @RequestParam(defaultValue = "true") Boolean notifyAssignmentDue,
                                         @RequestParam(defaultValue = "true") Boolean notifyAssignmentMissing,
                                         @RequestParam(defaultValue = "true") Boolean notifyAssignmentGraded,
                                         @RequestParam(defaultValue = "false") Boolean notifyCalendarSync,
                                         @RequestParam(defaultValue = "false") Boolean dailySummaryEnabled,
                                         @RequestParam(defaultValue = "08:00") String dailySummaryTime,
                                         @RequestParam(defaultValue = "false") Boolean weeklySummaryEnabled,
                                         @RequestParam(defaultValue = "SUNDAY") String weeklySummaryDay,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            // Validate email format
            if (parentEmail == null || parentEmail.trim().isEmpty() || !isValidEmail(parentEmail)) {
                redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
                return "redirect:/students/" + studentId + "/calendar";
            }
            
            // Create DTO
            ParentNotificationSettingsDto settingsDto = new ParentNotificationSettingsDto();
            settingsDto.setParentEmail(parentEmail.trim());
            settingsDto.setParentName(parentName != null ? parentName.trim() : null);
            settingsDto.setNotifyAssignmentDue(notifyAssignmentDue);
            settingsDto.setNotifyAssignmentMissing(notifyAssignmentMissing);
            settingsDto.setNotifyAssignmentGraded(notifyAssignmentGraded);
            settingsDto.setNotifyCalendarSync(notifyCalendarSync);
            settingsDto.setDailySummaryEnabled(dailySummaryEnabled);
            settingsDto.setDailySummaryTime(dailySummaryTime);
            settingsDto.setWeeklySummaryEnabled(weeklySummaryEnabled);
            settingsDto.setWeeklySummaryDay(weeklySummaryDay);
            
            // Set up notifications
            ParentNotificationSettings settings = parentNotificationService.setupParentNotifications(
                    userId, studentId, settingsDto);
            
            log.info("Parent notification settings created for user {} student {} email {}", 
                    userId, studentId, parentEmail);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Parent notification settings saved! Please check " + parentEmail + 
                    " for a verification email to activate notifications.");
            
        } catch (Exception e) {
            log.error("Failed to setup parent notifications for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to setup notifications. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Update parent email notifications
     */
    @PostMapping("/students/{studentId}/calendar/update-parent-notifications")
    public String updateParentNotifications(@PathVariable Long studentId,
                                          @RequestParam String parentEmail,
                                          @RequestParam(required = false) String parentName,
                                          @RequestParam(defaultValue = "true") Boolean notifyAssignmentDue,
                                          @RequestParam(defaultValue = "true") Boolean notifyAssignmentMissing,
                                          @RequestParam(defaultValue = "true") Boolean notifyAssignmentGraded,
                                          @RequestParam(defaultValue = "false") Boolean notifyCalendarSync,
                                          @RequestParam(defaultValue = "false") Boolean dailySummaryEnabled,
                                          @RequestParam(defaultValue = "08:00") String dailySummaryTime,
                                          @RequestParam(defaultValue = "false") Boolean weeklySummaryEnabled,
                                          @RequestParam(defaultValue = "SUNDAY") String weeklySummaryDay,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            // Validate email format
            if (parentEmail == null || parentEmail.trim().isEmpty() || !isValidEmail(parentEmail)) {
                redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
                return "redirect:/students/" + studentId + "/calendar";
            }
            
            // Create DTO
            ParentNotificationSettingsDto settingsDto = new ParentNotificationSettingsDto();
            settingsDto.setParentEmail(parentEmail.trim());
            settingsDto.setParentName(parentName != null ? parentName.trim() : null);
            settingsDto.setNotifyAssignmentDue(notifyAssignmentDue);
            settingsDto.setNotifyAssignmentMissing(notifyAssignmentMissing);
            settingsDto.setNotifyAssignmentGraded(notifyAssignmentGraded);
            settingsDto.setNotifyCalendarSync(notifyCalendarSync);
            settingsDto.setDailySummaryEnabled(dailySummaryEnabled);
            settingsDto.setDailySummaryTime(dailySummaryTime);
            settingsDto.setWeeklySummaryEnabled(weeklySummaryEnabled);
            settingsDto.setWeeklySummaryDay(weeklySummaryDay);
            
            // Update notifications
            ParentNotificationSettings settings = parentNotificationService.updateParentNotifications(
                    userId, studentId, settingsDto);
            
            log.info("Parent notification settings updated for user {} student {} email {}", 
                    userId, studentId, parentEmail);
            
            redirectAttributes.addFlashAttribute("success", "Parent notification settings updated successfully!");
            
        } catch (Exception e) {
            log.error("Failed to update parent notifications for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to update notifications. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Delete parent email notifications
     */
    @PostMapping("/students/{studentId}/calendar/delete-parent-notifications")
    public String deleteParentNotifications(@PathVariable Long studentId,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            parentNotificationService.deleteParentNotificationSettings(userId, studentId);
            
            log.info("Parent notification settings deleted for user {} student {}", userId, studentId);
            redirectAttributes.addFlashAttribute("success", "Parent notification settings removed successfully.");
            
        } catch (Exception e) {
            log.error("Failed to delete parent notifications for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to remove notifications. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    /**
     * Resend parent email verification
     */
    @PostMapping("/students/{studentId}/calendar/resend-parent-verification")
    public String resendParentVerification(@PathVariable Long studentId,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }
        
        String userId = getUserId(session);
        
        try {
            boolean sent = parentNotificationService.resendVerificationEmail(userId, studentId);
            
            if (sent) {
                redirectAttributes.addFlashAttribute("success", 
                        "Verification email sent! Please check your email to activate notifications.");
            } else {
                redirectAttributes.addFlashAttribute("error", 
                        "Unable to send verification email. Email may already be verified.");
            }
            
        } catch (Exception e) {
            log.error("Failed to resend parent verification for student {}", studentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to send verification email. Please try again.");
        }
        
        return "redirect:/students/" + studentId + "/calendar";
    }
    
    // Helper methods
    
    private String getUserId(HttpSession session) {
        // For now, use session ID as user ID
        // In a real implementation, this would be the authenticated user's ID
        return session.getId();
    }
    
    private CalendarSyncSettings getSyncSettings(String userId, Long studentId) {
        // Get settings from sync service - it will create defaults if none exist
        return syncSettingsService.getSyncSettings(userId, studentId);
    }
    
    private String generateStateParameter(String userId, Long studentId, AccountType accountType) {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        String randomPart = Base64.getEncoder().encodeToString(randomBytes);
        return String.format("%s_%s_%s_%s", userId, studentId, accountType.name(), randomPart);
    }
    
    private void clearOAuthSession(HttpSession session) {
        session.removeAttribute("oauth_state");
        session.removeAttribute("oauth_account_type");
        session.removeAttribute("oauth_student_id");
        session.removeAttribute("oauth_user_id");
        session.removeAttribute("invitation_token");
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    private List<String> parseJsonStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON string list: {}", json, e);
            return new ArrayList<>();
        }
    }
    
    private List<Integer> parseJsonIntegerList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON integer list: {}", json, e);
            return new ArrayList<>();
        }
    }
    
    private String listToJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert list to JSON: {}", list, e);
            return "[]";
        }
    }
}