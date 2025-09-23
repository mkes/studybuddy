package com.studytracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for Google OAuth 2.0 integration.
 * Binds properties from application.yml under the 'google.oauth' prefix.
 */
@Data
@Component
@ConfigurationProperties(prefix = "google.oauth")
public class GoogleOAuthProperties {
    
    /**
     * Google OAuth 2.0 client ID
     */
    private String clientId;
    
    /**
     * Google OAuth 2.0 client secret
     */
    private String clientSecret;
    
    /**
     * OAuth redirect URI for handling callbacks
     */
    private String redirectUri;
    
    /**
     * List of OAuth scopes required for calendar access
     */
    private List<String> scopes;
}