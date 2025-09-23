package com.studytracker.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalendarTokenTest {
    
    private CalendarToken calendarToken;
    private LocalDateTime futureTime;
    private LocalDateTime pastTime;
    
    @BeforeEach
    void setUp() {
        futureTime = LocalDateTime.now().plusHours(1);
        pastTime = LocalDateTime.now().minusHours(1);
        
        calendarToken = new CalendarToken(
                "user123",
                1L,
                AccountType.PARENT,
                "encrypted_access_token",
                "encrypted_refresh_token",
                futureTime
        );
    }
    
    @Test
    void testDefaultConstructor() {
        CalendarToken token = new CalendarToken();
        assertNotNull(token);
        assertNull(token.getId());
        assertNull(token.getUserId());
        assertNull(token.getStudentId());
        assertNull(token.getAccountType());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        assertEquals("user123", calendarToken.getUserId());
        assertEquals(1L, calendarToken.getStudentId());
        assertEquals(AccountType.PARENT, calendarToken.getAccountType());
        assertEquals("encrypted_access_token", calendarToken.getEncryptedAccessToken());
        assertEquals("encrypted_refresh_token", calendarToken.getEncryptedRefreshToken());
        assertEquals(futureTime, calendarToken.getTokenExpiresAt());
    }
    
    @Test
    void testGettersAndSetters() {
        calendarToken.setId(1L);
        calendarToken.setGoogleEmail("parent@example.com");
        calendarToken.setCalendarId("calendar123");
        
        assertEquals(1L, calendarToken.getId());
        assertEquals("parent@example.com", calendarToken.getGoogleEmail());
        assertEquals("calendar123", calendarToken.getCalendarId());
    }
    
    @Test
    void testIsTokenExpired_WithFutureTime() {
        calendarToken.setTokenExpiresAt(futureTime);
        assertFalse(calendarToken.isTokenExpired());
    }
    
    @Test
    void testIsTokenExpired_WithPastTime() {
        calendarToken.setTokenExpiresAt(pastTime);
        assertTrue(calendarToken.isTokenExpired());
    }
    
    @Test
    void testIsTokenExpired_WithNullTime() {
        calendarToken.setTokenExpiresAt(null);
        assertFalse(calendarToken.isTokenExpired());
    }
    
    @Test
    void testToString() {
        calendarToken.setId(1L);
        calendarToken.setGoogleEmail("parent@example.com");
        calendarToken.setCalendarId("calendar123");
        
        String toString = calendarToken.toString();
        assertTrue(toString.contains("CalendarToken{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("userId='user123'"));
        assertTrue(toString.contains("studentId=1"));
        assertTrue(toString.contains("accountType=PARENT"));
        assertTrue(toString.contains("googleEmail='parent@example.com'"));
        assertTrue(toString.contains("calendarId='calendar123'"));
    }
    
    @Test
    void testAccountTypeEnum() {
        assertEquals(2, AccountType.values().length);
        assertEquals(AccountType.PARENT, AccountType.valueOf("PARENT"));
        assertEquals(AccountType.STUDENT, AccountType.valueOf("STUDENT"));
    }
    
    @Test
    void testTimestampFields() {
        LocalDateTime now = LocalDateTime.now();
        calendarToken.setCreatedAt(now);
        calendarToken.setUpdatedAt(now);
        
        assertEquals(now, calendarToken.getCreatedAt());
        assertEquals(now, calendarToken.getUpdatedAt());
    }
}