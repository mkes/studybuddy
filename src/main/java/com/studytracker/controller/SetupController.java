package com.studytracker.controller;

import com.studytracker.config.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for application setup and configuration
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SetupController {
    
    private final GoogleOAuthProperties oAuthProperties;
    
    /**
     * Display setup page for Google Calendar integration
     */
    @GetMapping("/setup")
    public String setupPage(Model model) {
        model.addAttribute("currentClientId", oAuthProperties.getClientId());
        model.addAttribute("currentClientSecret", maskSecret(oAuthProperties.getClientSecret()));
        model.addAttribute("currentRedirectUri", oAuthProperties.getRedirectUri());
        model.addAttribute("currentScopes", oAuthProperties.getScopes());
        
        // Check if configuration is valid
        boolean isConfigured = isOAuthConfigured();
        model.addAttribute("isConfigured", isConfigured);
        
        if (!isConfigured) {
            model.addAttribute("setupRequired", true);
            model.addAttribute("configurationIssues", getConfigurationIssues());
        }
        
        return "setup";
    }
    
    /**
     * Test Google OAuth configuration
     */
    @PostMapping("/setup/test")
    public String testConfiguration(RedirectAttributes redirectAttributes) {
        try {
            if (isOAuthConfigured()) {
                redirectAttributes.addFlashAttribute("success", 
                    "Google OAuth configuration is valid! You can now use calendar integration.");
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "Configuration is incomplete. Please check the issues listed above.");
            }
        } catch (Exception e) {
            log.error("Error testing configuration", e);
            redirectAttributes.addFlashAttribute("error", 
                "Error testing configuration: " + e.getMessage());
        }
        
        return "redirect:/setup";
    }
    
    private boolean isOAuthConfigured() {
        return oAuthProperties.getClientId() != null && 
               !oAuthProperties.getClientId().equals("your-google-client-id") &&
               !oAuthProperties.getClientId().trim().isEmpty() &&
               oAuthProperties.getClientSecret() != null && 
               !oAuthProperties.getClientSecret().equals("your-google-client-secret") &&
               !oAuthProperties.getClientSecret().trim().isEmpty() &&
               oAuthProperties.getScopes() != null && 
               !oAuthProperties.getScopes().isEmpty() &&
               oAuthProperties.getRedirectUri() != null && 
               !oAuthProperties.getRedirectUri().trim().isEmpty();
    }
    
    private java.util.List<String> getConfigurationIssues() {
        java.util.List<String> issues = new java.util.ArrayList<>();
        
        if (oAuthProperties.getClientId() == null || 
            oAuthProperties.getClientId().equals("your-google-client-id") ||
            oAuthProperties.getClientId().trim().isEmpty()) {
            issues.add("Google OAuth Client ID is not configured");
        }
        
        if (oAuthProperties.getClientSecret() == null || 
            oAuthProperties.getClientSecret().equals("your-google-client-secret") ||
            oAuthProperties.getClientSecret().trim().isEmpty()) {
            issues.add("Google OAuth Client Secret is not configured");
        }
        
        if (oAuthProperties.getScopes() == null || oAuthProperties.getScopes().isEmpty()) {
            issues.add("Google OAuth Scopes are not configured");
        }
        
        if (oAuthProperties.getRedirectUri() == null || oAuthProperties.getRedirectUri().trim().isEmpty()) {
            issues.add("Google OAuth Redirect URI is not configured");
        }
        
        return issues;
    }
    
    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return secret;
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}