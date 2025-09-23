package com.studytracker.controller;

import com.studytracker.model.StudentCalendarInvitation;
import com.studytracker.service.StudentInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling calendar invitation acceptance flow.
 * This controller handles public invitation links that don't require authentication.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/calendar/invitation")
public class CalendarInvitationController {
    
    private final StudentInvitationService invitationService;
    
    /**
     * Display invitation acceptance page
     */
    @GetMapping("/accept")
    public String showInvitationAcceptance(@RequestParam String token,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        try {
            // Validate invitation token
            StudentCalendarInvitation invitation = invitationService.validateInvitationToken(token);
            
            model.addAttribute("invitation", invitation);
            model.addAttribute("token", token);
            
            log.info("Displaying invitation acceptance page for token: {}...", 
                    token.substring(0, Math.min(8, token.length())));
            
            return "calendar-invitation-accept";
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid invitation token provided: {}", e.getMessage());
            model.addAttribute("error", "Invalid invitation link. Please check the link and try again.");
            return "calendar-invitation-error";
            
        } catch (IllegalStateException e) {
            log.warn("Invitation not valid for acceptance: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "calendar-invitation-error";
            
        } catch (Exception e) {
            log.error("Error validating invitation token", e);
            model.addAttribute("error", "An error occurred while processing your invitation. Please try again later.");
            return "calendar-invitation-error";
        }
    }
    
    /**
     * Handle invitation acceptance and redirect to OAuth flow
     */
    @GetMapping("/proceed")
    public String proceedWithInvitation(@RequestParam String token,
                                      RedirectAttributes redirectAttributes) {
        try {
            // Validate invitation token again
            StudentCalendarInvitation invitation = invitationService.validateInvitationToken(token);
            
            log.info("Proceeding with invitation acceptance for student {} user {}", 
                    invitation.getStudentId(), invitation.getUserId());
            
            // Redirect to student OAuth flow
            return "redirect:/students/" + invitation.getStudentId() + "/calendar/connect/student?token=" + token;
            
        } catch (Exception e) {
            log.error("Error proceeding with invitation", e);
            redirectAttributes.addFlashAttribute("error", "Failed to process invitation. Please try again.");
            return "redirect:/calendar/invitation/accept?token=" + token;
        }
    }
}