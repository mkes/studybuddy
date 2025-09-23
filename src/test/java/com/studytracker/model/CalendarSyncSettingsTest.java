package com.studytracker.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalendarSyncSettingsTest {
    
    private CalendarSyncSettings syncSettings;
    
    @BeforeEach
    void setUp() {
        syncSettings = new CalendarSyncSettings("user123", 1L);
    }
    
    @Test
    void testDefaultConstructor() {
        CalendarSyncSettings settings = new CalendarSyncSettings();
        assertNotNull(settings);
        assertNull(settings.getId());
        assertNull(settings.getUserId());
        assertNull(settings.getStudentId());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        assertEquals("user123", syncSettings.getUserId());
        assertEquals(1L, syncSettings.getStudentId());
    }
    
    @Test
    void testDefaultValues() {
        assertTrue(syncSettings.getSyncEnabled());
        assertTrue(syncSettings.getSyncToParentCalendar());
        assertTrue(syncSettings.getSyncToStudentCalendar());
        assertEquals("[1440,120]", syncSettings.getParentReminderMinutes());
        assertEquals("[120,30]", syncSettings.getStudentReminderMinutes());
        assertFalse(syncSettings.getSyncCompletedAssignments());
        assertTrue(syncSettings.getAutoSyncEnabled());
    }
    
    @Test
    void testGettersAndSetters() {
        syncSettings.setId(1L);
        syncSettings.setSyncEnabled(false);
        syncSettings.setSyncToParentCalendar(false);
        syncSettings.setSyncToStudentCalendar(false);
        syncSettings.setParentReminderMinutes("[60,15]");
        syncSettings.setStudentReminderMinutes("[30,10]");
        syncSettings.setIncludedCourses("[\"Math\",\"Science\"]");
        syncSettings.setExcludedAssignmentTypes("[\"quiz\"]");
        syncSettings.setSyncCompletedAssignments(true);
        syncSettings.setAutoSyncEnabled(false);
        
        assertEquals(1L, syncSettings.getId());
        assertFalse(syncSettings.getSyncEnabled());
        assertFalse(syncSettings.getSyncToParentCalendar());
        assertFalse(syncSettings.getSyncToStudentCalendar());
        assertEquals("[60,15]", syncSettings.getParentReminderMinutes());
        assertEquals("[30,10]", syncSettings.getStudentReminderMinutes());
        assertEquals("[\"Math\",\"Science\"]", syncSettings.getIncludedCourses());
        assertEquals("[\"quiz\"]", syncSettings.getExcludedAssignmentTypes());
        assertTrue(syncSettings.getSyncCompletedAssignments());
        assertFalse(syncSettings.getAutoSyncEnabled());
    }
    
    @Test
    void testTimestampFields() {
        LocalDateTime now = LocalDateTime.now();
        syncSettings.setCreatedAt(now);
        syncSettings.setUpdatedAt(now);
        
        assertEquals(now, syncSettings.getCreatedAt());
        assertEquals(now, syncSettings.getUpdatedAt());
    }
    
    @Test
    void testToString() {
        syncSettings.setId(1L);
        syncSettings.setIncludedCourses("[\"Math\"]");
        
        String toString = syncSettings.toString();
        assertTrue(toString.contains("CalendarSyncSettings{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("userId='user123'"));
        assertTrue(toString.contains("studentId=1"));
        assertTrue(toString.contains("syncEnabled=true"));
        assertTrue(toString.contains("syncToParentCalendar=true"));
        assertTrue(toString.contains("syncToStudentCalendar=true"));
    }
    
    @Test
    void testJsonFields() {
        String courses = "[\"Mathematics\",\"Physics\",\"Chemistry\"]";
        String assignmentTypes = "[\"quiz\",\"discussion\"]";
        
        syncSettings.setIncludedCourses(courses);
        syncSettings.setExcludedAssignmentTypes(assignmentTypes);
        
        assertEquals(courses, syncSettings.getIncludedCourses());
        assertEquals(assignmentTypes, syncSettings.getExcludedAssignmentTypes());
    }
    
    @Test
    void testReminderMinutesFormat() {
        // Test default reminder formats
        assertEquals("[1440,120]", syncSettings.getParentReminderMinutes());
        assertEquals("[120,30]", syncSettings.getStudentReminderMinutes());
        
        // Test custom reminder formats
        syncSettings.setParentReminderMinutes("[2880,1440,60]"); // 2 days, 1 day, 1 hour
        syncSettings.setStudentReminderMinutes("[60,15]"); // 1 hour, 15 minutes
        
        assertEquals("[2880,1440,60]", syncSettings.getParentReminderMinutes());
        assertEquals("[60,15]", syncSettings.getStudentReminderMinutes());
    }
}