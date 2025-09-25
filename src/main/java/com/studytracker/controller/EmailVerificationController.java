package com.studytracker.controller;

import com.studytracker.service.ParentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for handling email verification
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class EmailVerificationController {
    
    private final ParentNotificationService parentNotificationService;
    
    /**
     * Verify parent email address
     */
    @GetMapping("/verify-parent-email")
    public String verifyParentEmail(@RequestParam String token, Model model) {
        try {
            boolean verified = parentNotificationService.verifyParentEmail(token);
            
            if (verified) {
                model.addAttribute("success", true);
                model.addAttribute("message", 
                    "Email verified successfully! You will now receive notifications about your student's assignments.");
            } else {
                model.addAttribute("success", false);
                model.addAttribute("message", 
                    "Email verification failed. The link may be invalid or expired. Please request a new verification email.");
            }
            
        } catch (Exception e) {
            log.error("Error verifying parent email with token: {}", token, e);
            model.addAttribute("success", false);
            model.addAttribute("message", 
                "An error occurred during verification. Please try again or contact support.");
        }
        
        return "email-verification-result";
    }
}