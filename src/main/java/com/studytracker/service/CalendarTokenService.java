package com.studytracker.service;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarToken;
import com.studytracker.repository.CalendarTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing encrypted calendar tokens with AES-256 encryption
 */
@Service
@Transactional
public class CalendarTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(CalendarTokenService.class);
    
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int TOKEN_REFRESH_BUFFER_MINUTES = 5; // Refresh tokens 5 minutes before expiry
    
    private final CalendarTokenRepository tokenRepository;
    private final SecretKey encryptionKey;
    
    public CalendarTokenService(CalendarTokenRepository tokenRepository,
                               @Value("${app.encryption.key:}") String encryptionKeyString) {
        this.tokenRepository = tokenRepository;
        this.encryptionKey = initializeEncryptionKey(encryptionKeyString);
    }
    
    /**
     * Initialize encryption key from configuration or generate a new one
     */
    private SecretKey initializeEncryptionKey(String keyString) {
        if (keyString != null && !keyString.trim().isEmpty()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(keyString);
                return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
            } catch (Exception e) {
                logger.warn("Invalid encryption key provided, generating new key", e);
            }
        }
        
        // Generate a new key if none provided or invalid
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            keyGenerator.init(256); // AES-256
            SecretKey key = keyGenerator.generateKey();
            String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
            logger.warn("Generated new encryption key. Please set app.encryption.key={} in your configuration", encodedKey);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }
    
    /**
     * Store encrypted tokens for a user
     */
    public CalendarToken storeTokens(String userId, Long studentId, AccountType accountType,
                                   String accessToken, String refreshToken, LocalDateTime expiresAt,
                                   String googleEmail, String calendarId) {
        try {
            String encryptedAccessToken = encrypt(accessToken);
            String encryptedRefreshToken = encrypt(refreshToken);
            
            // Check if token already exists
            Optional<CalendarToken> existingToken = tokenRepository
                    .findByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            
            CalendarToken token;
            if (existingToken.isPresent()) {
                // Update existing token
                token = existingToken.get();
                token.setEncryptedAccessToken(encryptedAccessToken);
                token.setEncryptedRefreshToken(encryptedRefreshToken);
                token.setTokenExpiresAt(expiresAt);
                token.setGoogleEmail(googleEmail);
                if (calendarId != null) {
                    token.setCalendarId(calendarId);
                }
            } else {
                // Create new token
                token = new CalendarToken(userId, studentId, accountType, 
                                        encryptedAccessToken, encryptedRefreshToken, expiresAt);
                token.setGoogleEmail(googleEmail);
                token.setCalendarId(calendarId);
            }
            
            CalendarToken savedToken = tokenRepository.save(token);
            logger.info("Stored encrypted tokens for user {} student {} account type {}", 
                       userId, studentId, accountType);
            return savedToken;
            
        } catch (Exception e) {
            logger.error("Failed to store encrypted tokens for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            throw new RuntimeException("Failed to store encrypted tokens", e);
        }
    }
    
    /**
     * Get valid access token, refreshing if necessary
     */
    public Optional<String> getValidAccessToken(String userId, Long studentId, AccountType accountType) {
        try {
            Optional<CalendarToken> tokenOpt = tokenRepository
                    .findByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            
            if (tokenOpt.isEmpty()) {
                logger.debug("No token found for user {} student {} account type {}", 
                           userId, studentId, accountType);
                return Optional.empty();
            }
            
            CalendarToken token = tokenOpt.get();
            
            // Check if token needs refresh
            if (needsRefresh(token)) {
                logger.info("Token needs refresh for user {} student {} account type {}", 
                           userId, studentId, accountType);
                // Note: Actual token refresh would require Google Calendar API integration
                // For now, we'll return empty to indicate re-authentication is needed
                return Optional.empty();
            }
            
            String decryptedAccessToken = decrypt(token.getEncryptedAccessToken());
            return Optional.of(decryptedAccessToken);
            
        } catch (Exception e) {
            logger.error("Failed to get valid access token for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return Optional.empty();
        }
    }
    
    /**
     * Get refresh token for token refresh operations
     */
    public Optional<String> getRefreshToken(String userId, Long studentId, AccountType accountType) {
        try {
            Optional<CalendarToken> tokenOpt = tokenRepository
                    .findByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            
            if (tokenOpt.isEmpty()) {
                return Optional.empty();
            }
            
            String decryptedRefreshToken = decrypt(tokenOpt.get().getEncryptedRefreshToken());
            return Optional.of(decryptedRefreshToken);
            
        } catch (Exception e) {
            logger.error("Failed to get refresh token for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return Optional.empty();
        }
    }
    
    /**
     * Update token expiration after refresh
     */
    public boolean updateTokenExpiration(String userId, Long studentId, AccountType accountType,
                                       String newAccessToken, LocalDateTime newExpiresAt) {
        try {
            Optional<CalendarToken> tokenOpt = tokenRepository
                    .findByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            
            if (tokenOpt.isEmpty()) {
                return false;
            }
            
            CalendarToken token = tokenOpt.get();
            token.setEncryptedAccessToken(encrypt(newAccessToken));
            token.setTokenExpiresAt(newExpiresAt);
            tokenRepository.save(token);
            
            logger.info("Updated token expiration for user {} student {} account type {}", 
                       userId, studentId, accountType);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to update token expiration for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return false;
        }
    }
    
    /**
     * Check if calendar is connected for specific account type
     */
    public boolean isCalendarConnected(String userId, Long studentId, AccountType accountType) {
        try {
            Optional<CalendarToken> tokenOpt = tokenRepository
                    .findByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            
            if (tokenOpt.isEmpty()) {
                return false;
            }
            
            CalendarToken token = tokenOpt.get();
            // Consider connected if token exists and is not expired
            return !token.isTokenExpired();
            
        } catch (Exception e) {
            logger.error("Failed to check calendar connection for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return false;
        }
    }
    
    /**
     * Check if any calendar is connected for user and student
     */
    public boolean isAnyCalendarConnected(String userId, Long studentId) {
        return isCalendarConnected(userId, studentId, AccountType.PARENT) ||
               isCalendarConnected(userId, studentId, AccountType.STUDENT);
    }
    
    /**
     * Get calendar connection status for both account types
     */
    public CalendarConnectionStatus getConnectionStatus(String userId, Long studentId) {
        boolean parentConnected = isCalendarConnected(userId, studentId, AccountType.PARENT);
        boolean studentConnected = isCalendarConnected(userId, studentId, AccountType.STUDENT);
        
        return new CalendarConnectionStatus(parentConnected, studentConnected);
    }
    
    /**
     * Revoke and delete tokens for specific account type
     */
    public boolean revokeTokens(String userId, Long studentId, AccountType accountType) {
        try {
            tokenRepository.deleteByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            logger.info("Revoked tokens for user {} student {} account type {}", 
                       userId, studentId, accountType);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to revoke tokens for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return false;
        }
    }
    
    /**
     * Revoke and delete all tokens for user and student
     */
    public boolean revokeAllTokens(String userId, Long studentId) {
        try {
            tokenRepository.deleteByUserIdAndStudentId(userId, studentId);
            logger.info("Revoked all tokens for user {} student {}", userId, studentId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to revoke all tokens for user {} student {}", userId, studentId, e);
            return false;
        }
    }
    
    /**
     * Clean up expired tokens
     */
    public int cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired tokens", deletedCount);
            }
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired tokens", e);
            return 0;
        }
    }
    
    /**
     * Get all tokens that need refresh (expiring within buffer time)
     */
    public List<CalendarToken> getTokensNeedingRefresh() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refreshThreshold = now.plusMinutes(TOKEN_REFRESH_BUFFER_MINUTES);
        return tokenRepository.findTokensExpiringWithin(now, refreshThreshold);
    }
    
    /**
     * Update calendar ID for a token
     */
    public boolean updateCalendarId(String userId, Long studentId, AccountType accountType, String calendarId) {
        try {
            int updated = tokenRepository.updateCalendarId(userId, studentId, accountType, calendarId);
            if (updated > 0) {
                logger.info("Updated calendar ID for user {} student {} account type {}", 
                           userId, studentId, accountType);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            logger.error("Failed to update calendar ID for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return false;
        }
    }
    
    /**
     * Get calendar ID for a token
     */
    public Optional<String> getCalendarId(String userId, Long studentId, AccountType accountType) {
        try {
            Optional<CalendarToken> tokenOpt = tokenRepository
                    .findByUserIdAndStudentIdAndAccountType(userId, studentId, accountType);
            
            return tokenOpt.map(CalendarToken::getCalendarId);
            
        } catch (Exception e) {
            logger.error("Failed to get calendar ID for user {} student {} account type {}", 
                        userId, studentId, accountType, e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if token needs refresh
     */
    private boolean needsRefresh(CalendarToken token) {
        if (token.getTokenExpiresAt() == null) {
            return true;
        }
        
        LocalDateTime refreshTime = LocalDateTime.now().plusMinutes(TOKEN_REFRESH_BUFFER_MINUTES);
        return token.getTokenExpiresAt().isBefore(refreshTime);
    }
    
    /**
     * Encrypt a string using AES-256-GCM
     */
    private String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);
        
        byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // Combine IV and encrypted data
        byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
        
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }
    
    /**
     * Decrypt a string using AES-256-GCM
     */
    private String decrypt(String encryptedText) throws Exception {
        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
        
        System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
        
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec);
        
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
    
    /**
     * Inner class to represent calendar connection status
     */
    public static class CalendarConnectionStatus {
        private final boolean parentConnected;
        private final boolean studentConnected;
        
        public CalendarConnectionStatus(boolean parentConnected, boolean studentConnected) {
            this.parentConnected = parentConnected;
            this.studentConnected = studentConnected;
        }
        
        public boolean isParentConnected() {
            return parentConnected;
        }
        
        public boolean isStudentConnected() {
            return studentConnected;
        }
        
        public boolean isAnyConnected() {
            return parentConnected || studentConnected;
        }
        
        public boolean areBothConnected() {
            return parentConnected && studentConnected;
        }
        
        @Override
        public String toString() {
            return "CalendarConnectionStatus{" +
                    "parentConnected=" + parentConnected +
                    ", studentConnected=" + studentConnected +
                    '}';
        }
    }
}