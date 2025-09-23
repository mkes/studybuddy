package com.studytracker.service;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarToken;
import com.studytracker.repository.CalendarTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for CalendarTokenService to verify encryption works end-to-end
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CalendarTokenServiceIntegrationTest {
    
    @Autowired
    private CalendarTokenService tokenService;
    
    @Autowired
    private CalendarTokenRepository tokenRepository;
    
    @Test
    void testEncryptionDecryptionIntegration() {
        // Given
        String userId = "integration_user";
        Long studentId = 999L;
        String originalAccessToken = "original_access_token_12345";
        String originalRefreshToken = "original_refresh_token_67890";
        String googleEmail = "integration@test.com";
        String calendarId = "integration_calendar_id";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        // When - Store tokens
        CalendarToken storedToken = tokenService.storeTokens(
                userId, studentId, AccountType.PARENT,
                originalAccessToken, originalRefreshToken, expiresAt,
                googleEmail, calendarId
        );
        
        // Then - Verify token was stored
        assertNotNull(storedToken);
        assertNotNull(storedToken.getId());
        
        // Verify tokens are encrypted in database
        Optional<CalendarToken> dbToken = tokenRepository.findById(storedToken.getId());
        assertTrue(dbToken.isPresent());
        assertNotEquals(originalAccessToken, dbToken.get().getEncryptedAccessToken());
        assertNotEquals(originalRefreshToken, dbToken.get().getEncryptedRefreshToken());
        
        // When - Retrieve decrypted tokens
        Optional<String> retrievedAccessToken = tokenService.getValidAccessToken(userId, studentId, AccountType.PARENT);
        Optional<String> retrievedRefreshToken = tokenService.getRefreshToken(userId, studentId, AccountType.PARENT);
        
        // Then - Verify decryption works correctly
        assertTrue(retrievedAccessToken.isPresent());
        assertTrue(retrievedRefreshToken.isPresent());
        assertEquals(originalAccessToken, retrievedAccessToken.get());
        assertEquals(originalRefreshToken, retrievedRefreshToken.get());
        
        // Verify connection status
        assertTrue(tokenService.isCalendarConnected(userId, studentId, AccountType.PARENT));
        assertFalse(tokenService.isCalendarConnected(userId, studentId, AccountType.STUDENT));
        assertTrue(tokenService.isAnyCalendarConnected(userId, studentId));
        
        // Verify calendar ID retrieval
        Optional<String> retrievedCalendarId = tokenService.getCalendarId(userId, studentId, AccountType.PARENT);
        assertTrue(retrievedCalendarId.isPresent());
        assertEquals(calendarId, retrievedCalendarId.get());
    }
    
    @Test
    void testTokenCleanup() {
        // Given - Create expired token
        String userId = "cleanup_user";
        Long studentId = 888L;
        LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1);
        
        tokenService.storeTokens(
                userId, studentId, AccountType.PARENT,
                "expired_access", "expired_refresh", pastExpiry,
                "cleanup@test.com", "cleanup_calendar"
        );
        
        // Verify token exists but is expired
        assertFalse(tokenService.isCalendarConnected(userId, studentId, AccountType.PARENT));
        
        // When - Cleanup expired tokens
        int cleanedUp = tokenService.cleanupExpiredTokens();
        
        // Then - Verify cleanup worked
        assertTrue(cleanedUp >= 1);
        
        // Verify token no longer exists
        Optional<String> token = tokenService.getValidAccessToken(userId, studentId, AccountType.PARENT);
        assertFalse(token.isPresent());
    }
    
    @Test
    void testTokenRevocation() {
        // Given
        String userId = "revoke_user";
        Long studentId = 777L;
        LocalDateTime futureExpiry = LocalDateTime.now().plusHours(1);
        
        // Store tokens for both parent and student
        tokenService.storeTokens(
                userId, studentId, AccountType.PARENT,
                "parent_access", "parent_refresh", futureExpiry,
                "parent@test.com", "parent_calendar"
        );
        
        tokenService.storeTokens(
                userId, studentId, AccountType.STUDENT,
                "student_access", "student_refresh", futureExpiry,
                "student@test.com", "student_calendar"
        );
        
        // Verify both are connected
        assertTrue(tokenService.isCalendarConnected(userId, studentId, AccountType.PARENT));
        assertTrue(tokenService.isCalendarConnected(userId, studentId, AccountType.STUDENT));
        
        // When - Revoke parent token only
        boolean revoked = tokenService.revokeTokens(userId, studentId, AccountType.PARENT);
        
        // Then - Verify only parent was revoked
        assertTrue(revoked);
        assertFalse(tokenService.isCalendarConnected(userId, studentId, AccountType.PARENT));
        assertTrue(tokenService.isCalendarConnected(userId, studentId, AccountType.STUDENT));
        
        // When - Revoke all tokens
        boolean allRevoked = tokenService.revokeAllTokens(userId, studentId);
        
        // Then - Verify all tokens revoked
        assertTrue(allRevoked);
        assertFalse(tokenService.isCalendarConnected(userId, studentId, AccountType.PARENT));
        assertFalse(tokenService.isCalendarConnected(userId, studentId, AccountType.STUDENT));
        assertFalse(tokenService.isAnyCalendarConnected(userId, studentId));
    }
    
    @Test
    void testConnectionStatus() {
        // Given
        String userId = "status_user";
        Long studentId = 666L;
        LocalDateTime futureExpiry = LocalDateTime.now().plusHours(1);
        
        // Initially no connections
        CalendarTokenService.CalendarConnectionStatus initialStatus = 
                tokenService.getConnectionStatus(userId, studentId);
        assertFalse(initialStatus.isParentConnected());
        assertFalse(initialStatus.isStudentConnected());
        assertFalse(initialStatus.isAnyConnected());
        assertFalse(initialStatus.areBothConnected());
        
        // Connect parent only
        tokenService.storeTokens(
                userId, studentId, AccountType.PARENT,
                "parent_access", "parent_refresh", futureExpiry,
                "parent@test.com", "parent_calendar"
        );
        
        CalendarTokenService.CalendarConnectionStatus parentOnlyStatus = 
                tokenService.getConnectionStatus(userId, studentId);
        assertTrue(parentOnlyStatus.isParentConnected());
        assertFalse(parentOnlyStatus.isStudentConnected());
        assertTrue(parentOnlyStatus.isAnyConnected());
        assertFalse(parentOnlyStatus.areBothConnected());
        
        // Connect student as well
        tokenService.storeTokens(
                userId, studentId, AccountType.STUDENT,
                "student_access", "student_refresh", futureExpiry,
                "student@test.com", "student_calendar"
        );
        
        CalendarTokenService.CalendarConnectionStatus bothConnectedStatus = 
                tokenService.getConnectionStatus(userId, studentId);
        assertTrue(bothConnectedStatus.isParentConnected());
        assertTrue(bothConnectedStatus.isStudentConnected());
        assertTrue(bothConnectedStatus.isAnyConnected());
        assertTrue(bothConnectedStatus.areBothConnected());
    }
}