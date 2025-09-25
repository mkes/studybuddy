package com.studytracker.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncSettingsDtoTest {
    
    private SyncSettingsDto syncSettingsDto;
    
    @BeforeEach
    void setUp() {
        syncSettingsDto = new SyncSettingsDto();
    }
    
    @Test
    void defaultConstructor_CreatesEmptyDto() {
        // Assert
        assertNotNull(syncSettingsDto);
        assertNull(syncSettingsDto.getSyncEnabled());
        assertNull(syncSettingsDto.getSyncToParentCalendar());
        assertNull(syncSettingsDto.getSyncToStudentCalendar());
        assertNull(syncSettingsDto.getParentReminderMinutes());
        assertNull(syncSettingsDto.getStudentReminderMinutes());
        assertNull(syncSettingsDto.getIncludedCourses());
        assertNull(syncSettingsDto.getExcludedAssignmentTypes());
        assertNull(syncSettingsDto.getSyncCompletedAssignments());
        assertNull(syncSettingsDto.getAutoSyncEnabled());
    }
    
    @Test
    void constructorWithAllFields_SetsAllProperties() {
        // Arrange
        Boolean syncEnabled = true;
        Boolean syncToParentCalendar = true;
        Boolean syncToStudentCalendar = false;
        List<Integer> parentReminderMinutes = Arrays.asList(1440, 120);
        List<Integer> studentReminderMinutes = Arrays.asList(120, 30);
        List<String> includedCourses = Arrays.asList("Math", "Science");
        List<String> excludedAssignmentTypes = Arrays.asList("quiz", "discussion");
        Boolean syncCompletedAssignments = false;
        Boolean autoSyncEnabled = true;
        
        // Act
        SyncSettingsDto dto = new SyncSettingsDto(
                syncEnabled, syncToParentCalendar, syncToStudentCalendar,
                parentReminderMinutes, studentReminderMinutes,
                includedCourses, excludedAssignmentTypes,
                syncCompletedAssignments, autoSyncEnabled
        );
        
        // Assert
        assertEquals(syncEnabled, dto.getSyncEnabled());
        assertEquals(syncToParentCalendar, dto.getSyncToParentCalendar());
        assertEquals(syncToStudentCalendar, dto.getSyncToStudentCalendar());
        assertEquals(parentReminderMinutes, dto.getParentReminderMinutes());
        assertEquals(studentReminderMinutes, dto.getStudentReminderMinutes());
        assertEquals(includedCourses, dto.getIncludedCourses());
        assertEquals(excludedAssignmentTypes, dto.getExcludedAssignmentTypes());
        assertEquals(syncCompletedAssignments, dto.getSyncCompletedAssignments());
        assertEquals(autoSyncEnabled, dto.getAutoSyncEnabled());
    }
    
    @Test
    void setSyncEnabled_SetsValue() {
        // Act
        syncSettingsDto.setSyncEnabled(true);
        
        // Assert
        assertTrue(syncSettingsDto.getSyncEnabled());
    }
    
    @Test
    void setSyncToParentCalendar_SetsValue() {
        // Act
        syncSettingsDto.setSyncToParentCalendar(false);
        
        // Assert
        assertFalse(syncSettingsDto.getSyncToParentCalendar());
    }
    
    @Test
    void setSyncToStudentCalendar_SetsValue() {
        // Act
        syncSettingsDto.setSyncToStudentCalendar(true);
        
        // Assert
        assertTrue(syncSettingsDto.getSyncToStudentCalendar());
    }
    
    @Test
    void setParentReminderMinutes_SetsValue() {
        // Arrange
        List<Integer> reminderMinutes = Arrays.asList(60, 30, 15);
        
        // Act
        syncSettingsDto.setParentReminderMinutes(reminderMinutes);
        
        // Assert
        assertEquals(reminderMinutes, syncSettingsDto.getParentReminderMinutes());
        assertEquals(3, syncSettingsDto.getParentReminderMinutes().size());
        assertTrue(syncSettingsDto.getParentReminderMinutes().contains(60));
        assertTrue(syncSettingsDto.getParentReminderMinutes().contains(30));
        assertTrue(syncSettingsDto.getParentReminderMinutes().contains(15));
    }
    
    @Test
    void setStudentReminderMinutes_SetsValue() {
        // Arrange
        List<Integer> reminderMinutes = Arrays.asList(120, 60);
        
        // Act
        syncSettingsDto.setStudentReminderMinutes(reminderMinutes);
        
        // Assert
        assertEquals(reminderMinutes, syncSettingsDto.getStudentReminderMinutes());
        assertEquals(2, syncSettingsDto.getStudentReminderMinutes().size());
        assertTrue(syncSettingsDto.getStudentReminderMinutes().contains(120));
        assertTrue(syncSettingsDto.getStudentReminderMinutes().contains(60));
    }
    
    @Test
    void setIncludedCourses_SetsValue() {
        // Arrange
        List<String> courses = Arrays.asList("Mathematics", "Physics", "Chemistry");
        
        // Act
        syncSettingsDto.setIncludedCourses(courses);
        
        // Assert
        assertEquals(courses, syncSettingsDto.getIncludedCourses());
        assertEquals(3, syncSettingsDto.getIncludedCourses().size());
        assertTrue(syncSettingsDto.getIncludedCourses().contains("Mathematics"));
        assertTrue(syncSettingsDto.getIncludedCourses().contains("Physics"));
        assertTrue(syncSettingsDto.getIncludedCourses().contains("Chemistry"));
    }
    
    @Test
    void setExcludedAssignmentTypes_SetsValue() {
        // Arrange
        List<String> excludedTypes = Arrays.asList("quiz", "discussion", "practice");
        
        // Act
        syncSettingsDto.setExcludedAssignmentTypes(excludedTypes);
        
        // Assert
        assertEquals(excludedTypes, syncSettingsDto.getExcludedAssignmentTypes());
        assertEquals(3, syncSettingsDto.getExcludedAssignmentTypes().size());
        assertTrue(syncSettingsDto.getExcludedAssignmentTypes().contains("quiz"));
        assertTrue(syncSettingsDto.getExcludedAssignmentTypes().contains("discussion"));
        assertTrue(syncSettingsDto.getExcludedAssignmentTypes().contains("practice"));
    }
    
    @Test
    void setSyncCompletedAssignments_SetsValue() {
        // Act
        syncSettingsDto.setSyncCompletedAssignments(true);
        
        // Assert
        assertTrue(syncSettingsDto.getSyncCompletedAssignments());
    }
    
    @Test
    void setAutoSyncEnabled_SetsValue() {
        // Act
        syncSettingsDto.setAutoSyncEnabled(false);
        
        // Assert
        assertFalse(syncSettingsDto.getAutoSyncEnabled());
    }
    
    @Test
    void toString_ReturnsFormattedString() {
        // Arrange
        syncSettingsDto.setSyncEnabled(true);
        syncSettingsDto.setSyncToParentCalendar(true);
        syncSettingsDto.setSyncToStudentCalendar(false);
        syncSettingsDto.setParentReminderMinutes(Arrays.asList(1440, 120));
        syncSettingsDto.setStudentReminderMinutes(Arrays.asList(120, 30));
        syncSettingsDto.setIncludedCourses(Arrays.asList("Math"));
        syncSettingsDto.setExcludedAssignmentTypes(Arrays.asList("quiz"));
        syncSettingsDto.setSyncCompletedAssignments(false);
        syncSettingsDto.setAutoSyncEnabled(true);
        
        // Act
        String result = syncSettingsDto.toString();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("SyncSettingsDto{"));
        assertTrue(result.contains("syncEnabled=true"));
        assertTrue(result.contains("syncToParentCalendar=true"));
        assertTrue(result.contains("syncToStudentCalendar=false"));
        assertTrue(result.contains("parentReminderMinutes=[1440, 120]"));
        assertTrue(result.contains("studentReminderMinutes=[120, 30]"));
        assertTrue(result.contains("includedCourses=[Math]"));
        assertTrue(result.contains("excludedAssignmentTypes=[quiz]"));
        assertTrue(result.contains("syncCompletedAssignments=false"));
        assertTrue(result.contains("autoSyncEnabled=true"));
    }
    
    @Test
    void setNullValues_AcceptsNullValues() {
        // Act
        syncSettingsDto.setSyncEnabled(null);
        syncSettingsDto.setSyncToParentCalendar(null);
        syncSettingsDto.setSyncToStudentCalendar(null);
        syncSettingsDto.setParentReminderMinutes(null);
        syncSettingsDto.setStudentReminderMinutes(null);
        syncSettingsDto.setIncludedCourses(null);
        syncSettingsDto.setExcludedAssignmentTypes(null);
        syncSettingsDto.setSyncCompletedAssignments(null);
        syncSettingsDto.setAutoSyncEnabled(null);
        
        // Assert
        assertNull(syncSettingsDto.getSyncEnabled());
        assertNull(syncSettingsDto.getSyncToParentCalendar());
        assertNull(syncSettingsDto.getSyncToStudentCalendar());
        assertNull(syncSettingsDto.getParentReminderMinutes());
        assertNull(syncSettingsDto.getStudentReminderMinutes());
        assertNull(syncSettingsDto.getIncludedCourses());
        assertNull(syncSettingsDto.getExcludedAssignmentTypes());
        assertNull(syncSettingsDto.getSyncCompletedAssignments());
        assertNull(syncSettingsDto.getAutoSyncEnabled());
    }
    
    @Test
    void setEmptyLists_AcceptsEmptyLists() {
        // Arrange
        List<Integer> emptyIntegerList = Arrays.asList();
        List<String> emptyStringList = Arrays.asList();
        
        // Act
        syncSettingsDto.setParentReminderMinutes(emptyIntegerList);
        syncSettingsDto.setStudentReminderMinutes(emptyIntegerList);
        syncSettingsDto.setIncludedCourses(emptyStringList);
        syncSettingsDto.setExcludedAssignmentTypes(emptyStringList);
        
        // Assert
        assertNotNull(syncSettingsDto.getParentReminderMinutes());
        assertNotNull(syncSettingsDto.getStudentReminderMinutes());
        assertNotNull(syncSettingsDto.getIncludedCourses());
        assertNotNull(syncSettingsDto.getExcludedAssignmentTypes());
        assertTrue(syncSettingsDto.getParentReminderMinutes().isEmpty());
        assertTrue(syncSettingsDto.getStudentReminderMinutes().isEmpty());
        assertTrue(syncSettingsDto.getIncludedCourses().isEmpty());
        assertTrue(syncSettingsDto.getExcludedAssignmentTypes().isEmpty());
    }
}