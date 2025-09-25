package com.studytracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.dto.SyncSettingsDto;
import com.studytracker.model.CalendarSyncSettings;
import com.studytracker.model.PlannerItem;
import com.studytracker.repository.CalendarSyncSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({CalendarSyncSettingsService.class, ObjectMapper.class})
class CalendarSyncSettingsServiceIntegrationTest {
    
    @Autowired
    private CalendarSyncSettingsService syncSettingsService;
    
    @Autowired
    private CalendarSyncSettingsRepository syncSettingsRepository;
    
    private String userId;
    private Long studentId;
    
    @BeforeEach
    void setUp() {
        userId = "user123";
        studentId = 456L;
        
        // Clean up any existing data
        syncSettingsRepository.deleteAll();
    }
    
    @Test
    void getSyncSettings_NoExistingSettings_CreatesDefaultSettings() {
        // Act
        CalendarSyncSettings result = syncSettingsService.getSyncSettings(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(studentId, result.getStudentId());
        assertTrue(result.getSyncEnabled());
        assertTrue(result.getAutoSyncEnabled());
        assertTrue(result.getSyncToParentCalendar());
        assertTrue(result.getSyncToStudentCalendar());
        assertFalse(result.getSyncCompletedAssignments());
        
        // Verify it was saved to database
        assertTrue(syncSettingsRepository.existsByUserIdAndStudentId(userId, studentId));
    }
    
    @Test
    void updateSyncSettings_ValidDto_UpdatesAndPersistsSettings() {
        // Arrange
        SyncSettingsDto updateDto = new SyncSettingsDto();
        updateDto.setSyncEnabled(false);
        updateDto.setSyncToParentCalendar(true);
        updateDto.setSyncToStudentCalendar(false);
        updateDto.setSyncCompletedAssignments(true);
        updateDto.setAutoSyncEnabled(false);
        updateDto.setParentReminderMinutes(Arrays.asList(60, 30));
        updateDto.setStudentReminderMinutes(Arrays.asList(15));
        updateDto.setIncludedCourses(Arrays.asList("Math", "Science"));
        updateDto.setExcludedAssignmentTypes(Arrays.asList("quiz", "discussion"));
        
        // Act
        CalendarSyncSettings result = syncSettingsService.updateSyncSettings(userId, studentId, updateDto);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.getSyncEnabled());
        assertTrue(result.getSyncToParentCalendar());
        assertFalse(result.getSyncToStudentCalendar());
        assertTrue(result.getSyncCompletedAssignments());
        assertFalse(result.getAutoSyncEnabled());
        
        // Verify persistence
        CalendarSyncSettings persisted = syncSettingsRepository.findByUserIdAndStudentId(userId, studentId).orElse(null);
        assertNotNull(persisted);
        assertFalse(persisted.getSyncEnabled());
        assertTrue(persisted.getSyncCompletedAssignments());
    }
    
    @Test
    void getSyncSettingsDto_WithRealObjectMapper_ReturnsCorrectDto() {
        // Arrange - Create settings with JSON data
        CalendarSyncSettings settings = new CalendarSyncSettings(userId, studentId);
        settings.setParentReminderMinutes("[1440,120]");
        settings.setStudentReminderMinutes("[120,30]");
        settings.setIncludedCourses("[\"Math\",\"Science\"]");
        settings.setExcludedAssignmentTypes("[\"quiz\"]");
        syncSettingsRepository.save(settings);
        
        // Act
        SyncSettingsDto result = syncSettingsService.getSyncSettingsDto(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getParentReminderMinutes().size());
        assertTrue(result.getParentReminderMinutes().contains(1440));
        assertTrue(result.getParentReminderMinutes().contains(120));
        
        assertEquals(2, result.getStudentReminderMinutes().size());
        assertTrue(result.getStudentReminderMinutes().contains(120));
        assertTrue(result.getStudentReminderMinutes().contains(30));
        
        assertEquals(2, result.getIncludedCourses().size());
        assertTrue(result.getIncludedCourses().contains("Math"));
        assertTrue(result.getIncludedCourses().contains("Science"));
        
        assertEquals(1, result.getExcludedAssignmentTypes().size());
        assertTrue(result.getExcludedAssignmentTypes().contains("quiz"));
    }
    
    @Test
    void shouldSyncAssignment_WithRealFiltering_WorksCorrectly() {
        // Arrange
        SyncSettingsDto settingsDto = new SyncSettingsDto();
        settingsDto.setSyncEnabled(true);
        settingsDto.setSyncToParentCalendar(true);
        settingsDto.setSyncToStudentCalendar(true);
        settingsDto.setSyncCompletedAssignments(false);
        settingsDto.setAutoSyncEnabled(true);
        settingsDto.setIncludedCourses(Arrays.asList("Math", "Science"));
        settingsDto.setExcludedAssignmentTypes(Arrays.asList("quiz"));
        
        syncSettingsService.updateSyncSettings(userId, studentId, settingsDto);
        
        // Test cases
        PlannerItem mathHomework = createTestAssignment("Math Homework 1", "Math");
        PlannerItem scienceQuiz = createTestAssignment("Science Quiz 1", "Science");
        PlannerItem historyAssignment = createTestAssignment("History Essay", "History");
        PlannerItem completedAssignment = createTestAssignment("Math Test", "Math");
        completedAssignment.setSubmitted(true);
        
        // Act & Assert
        assertTrue(syncSettingsService.shouldSyncAssignment(userId, studentId, mathHomework));
        assertFalse(syncSettingsService.shouldSyncAssignment(userId, studentId, scienceQuiz)); // Quiz excluded
        assertFalse(syncSettingsService.shouldSyncAssignment(userId, studentId, historyAssignment)); // Course not included
        assertFalse(syncSettingsService.shouldSyncAssignment(userId, studentId, completedAssignment)); // Completed
    }
    
    @Test
    void reminderSettings_WithRealObjectMapper_WorksCorrectly() {
        // Arrange
        List<Integer> parentReminders = Arrays.asList(2880, 1440, 60); // 2 days, 1 day, 1 hour
        List<Integer> studentReminders = Arrays.asList(120, 30, 15); // 2 hours, 30 min, 15 min
        
        // Act
        syncSettingsService.updateParentReminderSettings(userId, studentId, parentReminders);
        syncSettingsService.updateStudentReminderSettings(userId, studentId, studentReminders);
        
        // Assert
        List<Integer> retrievedParentReminders = syncSettingsService.getParentReminderMinutes(userId, studentId);
        List<Integer> retrievedStudentReminders = syncSettingsService.getStudentReminderMinutes(userId, studentId);
        
        assertEquals(parentReminders.size(), retrievedParentReminders.size());
        assertTrue(retrievedParentReminders.containsAll(parentReminders));
        
        assertEquals(studentReminders.size(), retrievedStudentReminders.size());
        assertTrue(retrievedStudentReminders.containsAll(studentReminders));
    }
    
    @Test
    void courseFiltering_WithRealObjectMapper_WorksCorrectly() {
        // Arrange
        List<String> includedCourses = Arrays.asList("Mathematics", "Physics", "Chemistry");
        
        // Act
        syncSettingsService.updateCourseFiltering(userId, studentId, includedCourses);
        
        // Assert
        SyncSettingsDto settings = syncSettingsService.getSyncSettingsDto(userId, studentId);
        assertEquals(includedCourses.size(), settings.getIncludedCourses().size());
        assertTrue(settings.getIncludedCourses().containsAll(includedCourses));
    }
    
    @Test
    void assignmentTypeExclusion_WithRealObjectMapper_WorksCorrectly() {
        // Arrange
        List<String> excludedTypes = Arrays.asList("quiz", "discussion", "practice");
        
        // Act
        syncSettingsService.updateAssignmentTypeExclusion(userId, studentId, excludedTypes);
        
        // Assert
        SyncSettingsDto settings = syncSettingsService.getSyncSettingsDto(userId, studentId);
        assertEquals(excludedTypes.size(), settings.getExcludedAssignmentTypes().size());
        assertTrue(settings.getExcludedAssignmentTypes().containsAll(excludedTypes));
    }
    
    @Test
    void getUsersWithAutoSyncEnabled_ReturnsCorrectUsers() {
        // Arrange
        CalendarSyncSettings user1Settings = new CalendarSyncSettings("user1", 100L);
        user1Settings.setAutoSyncEnabled(true);
        
        CalendarSyncSettings user2Settings = new CalendarSyncSettings("user2", 200L);
        user2Settings.setAutoSyncEnabled(false);
        
        CalendarSyncSettings user3Settings = new CalendarSyncSettings("user3", 300L);
        user3Settings.setAutoSyncEnabled(true);
        
        syncSettingsRepository.saveAll(Arrays.asList(user1Settings, user2Settings, user3Settings));
        
        // Act
        List<CalendarSyncSettings> result = syncSettingsService.getUsersWithAutoSyncEnabled();
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.getUserId().equals("user1")));
        assertTrue(result.stream().anyMatch(s -> s.getUserId().equals("user3")));
        assertFalse(result.stream().anyMatch(s -> s.getUserId().equals("user2")));
    }
    
    private PlannerItem createTestAssignment(String title, String contextName) {
        PlannerItem assignment = new PlannerItem();
        assignment.setPlannableId(System.currentTimeMillis()); // Unique ID
        assignment.setAssignmentTitle(title);
        assignment.setContextName(contextName);
        assignment.setDueAt(LocalDateTime.now().plusDays(1));
        assignment.setPointsPossible(new BigDecimal("100.0"));
        assignment.setSubmitted(false);
        return assignment;
    }
}