package com.studytracker.service;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarToken;
import com.studytracker.repository.CalendarTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarTokenServiceTest {
    
    @Mock
    private CalendarTokenRepository tokenRepository;
    
    private CalendarTokenService tokenService;
    
    private final String testUserId = "user123";
    private final Long testStudentId = 1L;
    private final String testAccessToken = "access_token_123";
    private final String testRefreshToken = "refresh_token_123";
    private final String testGoogleEmail = "test@gmail.com";
    private final String testCalendarId = "calendar_123";
    private final LocalDateTime futureExpiry = LocalDateTime.now().plusHours(1);
    private final LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1);
    
    @BeforeEach
    void setUp() {
        // Use empty encryption key to trigger key generation for testing
        tokenService = new CalendarTokenService(tokenRepository, "");
    }
    
    @Test
    void testStoreTokens_NewToken() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        CalendarToken savedToken = createTestToken();
        when(tokenRepository.save(any(CalendarToken.class))).thenReturn(savedToken);
        
        // When
        CalendarToken result = tokenService.storeTokens(
                testUserId, testStudentId, AccountType.PARENT,
                testAccessToken, testRefreshToken, futureExpiry,
                testGoogleEmail, testCalendarId
        );
        
        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testStudentId, result.getStudentId());
        assertEquals(AccountType.PARENT, result.getAccountType());
        assertEquals(testGoogleEmail, result.getGoogleEmail());
        assertEquals(testCalendarId, result.getCalendarId());
        assertEquals(futureExpiry, result.getTokenExpiresAt());
        
        // Verify tokens are encrypted (should not match original)
        assertNotEquals(testAccessToken, result.getEncryptedAccessToken());
        assertNotEquals(testRefreshToken, result.getEncryptedRefreshToken());
        
        verify(tokenRepository).save(any(CalendarToken.class));
    }
    
    @Test
    void testStoreTokens_UpdateExistingToken() {
        // Given
        CalendarToken existingToken = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(existingToken));
        when(tokenRepository.save(any(CalendarToken.class))).thenReturn(existingToken);
        
        // When
        CalendarToken result = tokenService.storeTokens(
                testUserId, testStudentId, AccountType.PARENT,
                testAccessToken, testRefreshToken, futureExpiry,
                testGoogleEmail, testCalendarId
        );
        
        // Then
        assertNotNull(result);
        verify(tokenRepository).save(existingToken);
    }
    
    @Test
    void testGetValidAccessToken_ValidToken() {
        // Given
        CalendarToken token = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(token));
        
        // First store a token to encrypt it properly
        when(tokenRepository.save(any(CalendarToken.class))).thenReturn(token);
        CalendarToken storedToken = tokenService.storeTokens(
                testUserId, testStudentId, AccountType.PARENT,
                testAccessToken, testRefreshToken, futureExpiry,
                testGoogleEmail, testCalendarId
        );
        
        // Update mock to return the stored token with encrypted data
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(storedToken));
        
        // When
        Optional<String> result = tokenService.getValidAccessToken(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testAccessToken, result.get());
    }
    
    @Test
    void testGetValidAccessToken_NoToken() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When
        Optional<String> result = tokenService.getValidAccessToken(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void testGetValidAccessToken_ExpiredToken() {
        // Given
        CalendarToken expiredToken = createTestToken();
        expiredToken.setTokenExpiresAt(pastExpiry);
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(expiredToken));
        
        // When
        Optional<String> result = tokenService.getValidAccessToken(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void testGetRefreshToken_Success() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        CalendarToken savedToken = createTestToken();
        when(tokenRepository.save(any(CalendarToken.class))).thenAnswer(invocation -> {
            CalendarToken token = invocation.getArgument(0);
            savedToken.setEncryptedAccessToken(token.getEncryptedAccessToken());
            savedToken.setEncryptedRefreshToken(token.getEncryptedRefreshToken());
            return savedToken;
        });
        
        // Store token first to encrypt it
        CalendarToken storedToken = tokenService.storeTokens(
                testUserId, testStudentId, AccountType.PARENT,
                testAccessToken, testRefreshToken, futureExpiry,
                testGoogleEmail, testCalendarId
        );
        
        // Update mock to return stored token
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(storedToken));
        
        // When
        Optional<String> result = tokenService.getRefreshToken(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
    }
    
    @Test
    void testUpdateTokenExpiration_Success() {
        // Given
        CalendarToken token = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(token));
        when(tokenRepository.save(any(CalendarToken.class))).thenReturn(token);
        
        LocalDateTime newExpiry = LocalDateTime.now().plusHours(2);
        String newAccessToken = "new_access_token";
        
        // When
        boolean result = tokenService.updateTokenExpiration(
                testUserId, testStudentId, AccountType.PARENT, newAccessToken, newExpiry
        );
        
        // Then
        assertTrue(result);
        verify(tokenRepository).save(token);
        assertEquals(newExpiry, token.getTokenExpiresAt());
    }
    
    @Test
    void testUpdateTokenExpiration_TokenNotFound() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When
        boolean result = tokenService.updateTokenExpiration(
                testUserId, testStudentId, AccountType.PARENT, "new_token", futureExpiry
        );
        
        // Then
        assertFalse(result);
        verify(tokenRepository, never()).save(any());
    }
    
    @Test
    void testIsCalendarConnected_Connected() {
        // Given
        CalendarToken token = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(token));
        
        // When
        boolean result = tokenService.isCalendarConnected(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testIsCalendarConnected_NotConnected() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When
        boolean result = tokenService.isCalendarConnected(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsCalendarConnected_ExpiredToken() {
        // Given
        CalendarToken expiredToken = createTestToken();
        expiredToken.setTokenExpiresAt(pastExpiry);
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(expiredToken));
        
        // When
        boolean result = tokenService.isCalendarConnected(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsAnyCalendarConnected_ParentConnected() {
        // Given
        CalendarToken parentToken = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(parentToken));
        
        // When
        boolean result = tokenService.isAnyCalendarConnected(testUserId, testStudentId);
        
        // Then
        assertTrue(result);
        
        // Verify parent call was made (student call may not be made due to short-circuit evaluation)
        verify(tokenRepository).findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT);
    }
    
    @Test
    void testIsAnyCalendarConnected_StudentConnected() {
        // Given
        CalendarToken studentToken = createTestToken();
        studentToken.setAccountType(AccountType.STUDENT);
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.STUDENT))
                .thenReturn(Optional.of(studentToken));
        
        // When
        boolean result = tokenService.isAnyCalendarConnected(testUserId, testStudentId);
        
        // Then
        assertTrue(result);
        
        // Verify both calls were made (parent returns empty, so student is checked)
        verify(tokenRepository).findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT);
        verify(tokenRepository).findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.STUDENT);
    }
    
    @Test
    void testIsAnyCalendarConnected_NoneConnected() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        
        // When
        boolean result = tokenService.isAnyCalendarConnected(testUserId, testStudentId);
        
        // Then
        assertFalse(result);
        
        // Verify both calls were made
        verify(tokenRepository).findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT);
        verify(tokenRepository).findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.STUDENT);
    }
    
    @Test
    void testGetConnectionStatus_BothConnected() {
        // Given
        CalendarToken parentToken = createTestToken();
        CalendarToken studentToken = createTestToken();
        studentToken.setAccountType(AccountType.STUDENT);
        
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(parentToken));
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.STUDENT))
                .thenReturn(Optional.of(studentToken));
        
        // When
        CalendarTokenService.CalendarConnectionStatus status = tokenService.getConnectionStatus(testUserId, testStudentId);
        
        // Then
        assertTrue(status.isParentConnected());
        assertTrue(status.isStudentConnected());
        assertTrue(status.isAnyConnected());
        assertTrue(status.areBothConnected());
    }
    
    @Test
    void testGetConnectionStatus_OnlyParentConnected() {
        // Given
        CalendarToken parentToken = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(parentToken));
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        
        // When
        CalendarTokenService.CalendarConnectionStatus status = tokenService.getConnectionStatus(testUserId, testStudentId);
        
        // Then
        assertTrue(status.isParentConnected());
        assertFalse(status.isStudentConnected());
        assertTrue(status.isAnyConnected());
        assertFalse(status.areBothConnected());
    }
    
    @Test
    void testRevokeTokens_Success() {
        // When
        boolean result = tokenService.revokeTokens(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertTrue(result);
        verify(tokenRepository).deleteByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT);
    }
    
    @Test
    void testRevokeAllTokens_Success() {
        // When
        boolean result = tokenService.revokeAllTokens(testUserId, testStudentId);
        
        // Then
        assertTrue(result);
        verify(tokenRepository).deleteByUserIdAndStudentId(testUserId, testStudentId);
    }
    
    @Test
    void testCleanupExpiredTokens_Success() {
        // Given
        when(tokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(3);
        
        // When
        int result = tokenService.cleanupExpiredTokens();
        
        // Then
        assertEquals(3, result);
        verify(tokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
    
    @Test
    void testGetTokensNeedingRefresh() {
        // Given
        CalendarToken token1 = createTestToken();
        CalendarToken token2 = createTestToken();
        List<CalendarToken> tokens = Arrays.asList(token1, token2);
        
        when(tokenRepository.findTokensExpiringWithin(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(tokens);
        
        // When
        List<CalendarToken> result = tokenService.getTokensNeedingRefresh();
        
        // Then
        assertEquals(2, result.size());
        verify(tokenRepository).findTokensExpiringWithin(any(LocalDateTime.class), any(LocalDateTime.class));
    }
    
    @Test
    void testUpdateCalendarId_Success() {
        // Given
        when(tokenRepository.updateCalendarId(testUserId, testStudentId, AccountType.PARENT, testCalendarId))
                .thenReturn(1);
        
        // When
        boolean result = tokenService.updateCalendarId(testUserId, testStudentId, AccountType.PARENT, testCalendarId);
        
        // Then
        assertTrue(result);
        verify(tokenRepository).updateCalendarId(testUserId, testStudentId, AccountType.PARENT, testCalendarId);
    }
    
    @Test
    void testUpdateCalendarId_NotFound() {
        // Given
        when(tokenRepository.updateCalendarId(testUserId, testStudentId, AccountType.PARENT, testCalendarId))
                .thenReturn(0);
        
        // When
        boolean result = tokenService.updateCalendarId(testUserId, testStudentId, AccountType.PARENT, testCalendarId);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testGetCalendarId_Success() {
        // Given
        CalendarToken token = createTestToken();
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(token));
        
        // When
        Optional<String> result = tokenService.getCalendarId(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testCalendarId, result.get());
    }
    
    @Test
    void testGetCalendarId_NotFound() {
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        // When
        Optional<String> result = tokenService.getCalendarId(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void testEncryptionDecryption_RoundTrip() {
        // This test verifies that encryption and decryption work correctly
        // by storing and retrieving tokens
        
        // Given
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.empty());
        
        CalendarToken savedToken = createTestToken();
        when(tokenRepository.save(any(CalendarToken.class))).thenAnswer(invocation -> {
            CalendarToken token = invocation.getArgument(0);
            savedToken.setEncryptedAccessToken(token.getEncryptedAccessToken());
            savedToken.setEncryptedRefreshToken(token.getEncryptedRefreshToken());
            return savedToken;
        });
        
        // When - Store tokens
        CalendarToken storedToken = tokenService.storeTokens(
                testUserId, testStudentId, AccountType.PARENT,
                testAccessToken, testRefreshToken, futureExpiry,
                testGoogleEmail, testCalendarId
        );
        
        // Update mock to return stored token
        when(tokenRepository.findByUserIdAndStudentIdAndAccountType(testUserId, testStudentId, AccountType.PARENT))
                .thenReturn(Optional.of(storedToken));
        
        // When - Retrieve tokens
        Optional<String> retrievedAccessToken = tokenService.getValidAccessToken(testUserId, testStudentId, AccountType.PARENT);
        Optional<String> retrievedRefreshToken = tokenService.getRefreshToken(testUserId, testStudentId, AccountType.PARENT);
        
        // Then
        assertTrue(retrievedAccessToken.isPresent());
        assertTrue(retrievedRefreshToken.isPresent());
        assertEquals(testAccessToken, retrievedAccessToken.get());
        assertEquals(testRefreshToken, retrievedRefreshToken.get());
        
        // Verify tokens were encrypted (different from original)
        assertNotEquals(testAccessToken, storedToken.getEncryptedAccessToken());
        assertNotEquals(testRefreshToken, storedToken.getEncryptedRefreshToken());
    }
    
    @Test
    void testCalendarConnectionStatus_ToString() {
        // Given
        CalendarTokenService.CalendarConnectionStatus status = 
                new CalendarTokenService.CalendarConnectionStatus(true, false);
        
        // When
        String result = status.toString();
        
        // Then
        assertTrue(result.contains("parentConnected=true"));
        assertTrue(result.contains("studentConnected=false"));
    }
    
    private CalendarToken createTestToken() {
        CalendarToken token = new CalendarToken(
                testUserId, testStudentId, AccountType.PARENT,
                "encrypted_access", "encrypted_refresh", futureExpiry
        );
        token.setGoogleEmail(testGoogleEmail);
        token.setCalendarId(testCalendarId);
        token.setId(1L);
        return token;
    }
}