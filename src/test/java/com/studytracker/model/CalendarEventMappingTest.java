package com.studytracker.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalendarEventMappingTest {
    
    private CalendarEventMapping eventMapping;
    
    @BeforeEach
    void setUp() {
        eventMapping = new CalendarEventMapping(
                123L,
                1L,
                AccountType.PARENT,
                "google_event_123",
                "google_calendar_456"
        );
    }
    
    @Test
    void testDefaultConstructor() {
        CalendarEventMapping mapping = new CalendarEventMapping();
        assertNotNull(mapping);
        assertNull(mapping.getId());
        assertNull(mapping.getAssignmentId());
        assertNull(mapping.getStudentId());
        assertNull(mapping.getAccountType());
        assertNull(mapping.getGoogleEventId());
        assertNull(mapping.getGoogleCalendarId());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        assertEquals(123L, eventMapping.getAssignmentId());
        assertEquals(1L, eventMapping.getStudentId());
        assertEquals(AccountType.PARENT, eventMapping.getAccountType());
        assertEquals("google_event_123", eventMapping.getGoogleEventId());
        assertEquals("google_calendar_456", eventMapping.getGoogleCalendarId());
    }
    
    @Test
    void testGettersAndSetters() {
        eventMapping.setId(1L);
        eventMapping.setAssignmentId(456L);
        eventMapping.setStudentId(2L);
        eventMapping.setAccountType(AccountType.STUDENT);
        eventMapping.setGoogleEventId("new_event_id");
        eventMapping.setGoogleCalendarId("new_calendar_id");
        
        LocalDateTime syncTime = LocalDateTime.now();
        eventMapping.setLastSyncedAt(syncTime);
        
        assertEquals(1L, eventMapping.getId());
        assertEquals(456L, eventMapping.getAssignmentId());
        assertEquals(2L, eventMapping.getStudentId());
        assertEquals(AccountType.STUDENT, eventMapping.getAccountType());
        assertEquals("new_event_id", eventMapping.getGoogleEventId());
        assertEquals("new_calendar_id", eventMapping.getGoogleCalendarId());
        assertEquals(syncTime, eventMapping.getLastSyncedAt());
    }
    
    @Test
    void testMarkAsSynced() {
        assertNull(eventMapping.getLastSyncedAt());
        
        LocalDateTime beforeMark = LocalDateTime.now();
        eventMapping.markAsSynced();
        LocalDateTime afterMark = LocalDateTime.now();
        
        assertNotNull(eventMapping.getLastSyncedAt());
        assertTrue(eventMapping.getLastSyncedAt().isAfter(beforeMark) || 
                  eventMapping.getLastSyncedAt().isEqual(beforeMark));
        assertTrue(eventMapping.getLastSyncedAt().isBefore(afterMark) || 
                  eventMapping.getLastSyncedAt().isEqual(afterMark));
    }
    
    @Test
    void testTimestampFields() {
        LocalDateTime now = LocalDateTime.now();
        eventMapping.setCreatedAt(now);
        eventMapping.setUpdatedAt(now);
        
        assertEquals(now, eventMapping.getCreatedAt());
        assertEquals(now, eventMapping.getUpdatedAt());
    }
    
    @Test
    void testToString() {
        eventMapping.setId(1L);
        LocalDateTime syncTime = LocalDateTime.now();
        eventMapping.setLastSyncedAt(syncTime);
        
        String toString = eventMapping.toString();
        assertTrue(toString.contains("CalendarEventMapping{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("assignmentId=123"));
        assertTrue(toString.contains("studentId=1"));
        assertTrue(toString.contains("accountType=PARENT"));
        assertTrue(toString.contains("googleEventId='google_event_123'"));
        assertTrue(toString.contains("googleCalendarId='google_calendar_456'"));
        assertTrue(toString.contains("lastSyncedAt=" + syncTime));
    }
    
    @Test
    void testAccountTypeValues() {
        eventMapping.setAccountType(AccountType.STUDENT);
        assertEquals(AccountType.STUDENT, eventMapping.getAccountType());
        
        eventMapping.setAccountType(AccountType.PARENT);
        assertEquals(AccountType.PARENT, eventMapping.getAccountType());
    }
    
    @Test
    void testNullLastSyncedAt() {
        assertNull(eventMapping.getLastSyncedAt());
        
        eventMapping.setLastSyncedAt(null);
        assertNull(eventMapping.getLastSyncedAt());
    }
}