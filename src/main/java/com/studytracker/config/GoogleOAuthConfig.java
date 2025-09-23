package com.studytracker.config;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Collections;

/**
 * Configuration class for Google OAuth 2.0 flow.
 * Sets up the authorization code flow for Google Calendar API access.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleOAuthConfig {
    
    private final GoogleOAuthProperties oAuthProperties;
    
    /**
     * Google client secrets configuration
     */
    @Bean
    public GoogleClientSecrets googleClientSecrets() {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(oAuthProperties.getClientId());
        details.setClientSecret(oAuthProperties.getClientSecret());
        
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);
        
        return clientSecrets;
    }
    
    /**
     * Google Authorization Code Flow for OAuth 2.0
     */
    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(
            HttpTransport httpTransport,
            JsonFactory jsonFactory,
            GoogleClientSecrets clientSecrets) throws IOException {
        
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                oAuthProperties.getScopes())
                .setDataStoreFactory(new MemoryDataStoreFactory())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }
}