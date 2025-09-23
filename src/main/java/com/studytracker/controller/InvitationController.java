package com.studytracker.controller;

import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.service.StudentInvitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for managing student calendar invitations
 */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);
    
    private final StudentInvitationService invitationService;
    
    @Autowired
    public InvitationController(StudentInvitationService invitationService) {
        this.invitationService = invitationService;
    }
    
    /**
     * Create and send invitation to student
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendInvitation(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Long studentId = Long.valueOf(request.get("studentId").toString());
            String studentEmail = (String) request.get("studentEmail");
            String studentName = (String) request.get("studentName");
            
            StudentCalendarInvitation invitation = invitationService.createAndSendInvitation(
                    userId, studentId, studentEmail, studentName);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invitation sent successfully",
                    "invitationId", invitation.getId(),
                    "expiresAt", invitation.getExpiresAt()
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error sending invitation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to send invitation"
            ));
        }
    }
    
    /**
     * Accept invitation using token
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvitation(@RequestParam String token) {
        try {
            StudentCalendarInvitation invitation = invitationService.acceptInvitation(token);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invitation accepted successfully",
                    "invitationId", invitation.getId(),
                    "acceptedAt", invitation.getAcceptedAt()
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error accepting invitation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to accept invitation"
            ));
        }
    }
    
    /**
     * Validate invitation token
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateInvitation(@RequestParam String token) {
        try {
            StudentCalendarInvitation invitation = invitationService.validateInvitationToken(token);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", true,
                    "studentEmail", invitation.getStudentEmail(),
                    "expiresAt", invitation.getExpiresAt()
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error validating invitation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to validate invitation"
            ));
        }
    }
    
    /**
     * Get invitations for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserInvitations(@PathVariable String userId) {
        try {
            List<StudentCalendarInvitation> invitations = invitationService.getUserInvitations(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "invitations", invitations
            ));
            
        } catch (Exception e) {
            logger.error("Error getting user invitations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to get invitations"
            ));
        }
    }
    
    /**
     * Revoke invitation
     */
    @DeleteMapping("/revoke")
    public ResponseEntity<?> revokeInvitation(@RequestParam String userId, @RequestParam Long studentId) {
        try {
            invitationService.revokeInvitation(userId, studentId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invitation revoked successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error revoking invitation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to revoke invitation"
            ));
        }
    }
    
    /**
     * Check if user has active invitation for student
     */
    @GetMapping("/active")
    public ResponseEntity<?> hasActiveInvitation(@RequestParam String userId, @RequestParam Long studentId) {
        try {
            boolean hasActive = invitationService.hasActiveInvitation(userId, studentId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasActiveInvitation", hasActive
            ));
            
        } catch (Exception e) {
            logger.error("Error checking active invitation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to check invitation status"
            ));
        }
    }
}