package com.studytracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.dto.SyncSettingsDto;
import com.studytracker.model.CalendarSyncSettings;
import com.studytracker.model.PlannerItem;
import com.studytracker.repository.CalendarSyncSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class CalendarSyncSettingsServiceTest {
    
    @Mock
    private CalendarSyncSettingsRepository syncSettingsRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private CalendarSyncSettingsService syncSettingsService;
    
    private String userId;
    private Long studentId;
    private CalendarSyncSettings testSettings;
    private SyncSettingsDto testSettingsDto;
    
    @BeforeEach
    void setUp() {
        userId = "user123";
        studentId = 456L;
        
        testSettings = new CalendarSyncSettings(userId, studentId);
        testSettings.setId(1L);
        testSettings.setSyncEnabled(true);
        testSettings.setSyncToParentCalendar(true);
        testSettings.setSyncToStudentCalendar(true);
        testSettings.setParentReminderMinutes("[1440,120]");
        testSettings.setStudentReminderMinutes("[120,30]");
        testSettings.setIncludedCourses("[\"Math\",\"Science\"]");
        testSettings.setExcludedAssignmentTypes("[\"quiz\"]");
        testSettings.setSyncCompletedAssignments(false);
        testSettings.setAutoSyncEnabled(true);
        
        testSettingsDto = new SyncSettingsDto();
        testSettingsDto.setSyncEnabled(true);
        testSettingsDto.setSyncToParentCalendar(true);
        testSettingsDto.setSyncToStudentCalendar(true);
        testSettingsDto.setParentReminderMinutes(Arrays.asList(1440, 120));
        testSettingsDto.setStudentReminderMinutes(Arrays.asList(120, 30));
        testSettingsDto.setIncludedCourses(Arrays.asList("Math", "Science"));
        testSettingsDto.setExcludedAssignmentTypes(Arrays.asList("quiz"));
        testSettingsDto.setSyncCompletedAssignments(false);
        testSettingsDto.setAutoSyncEnabled(true);
    }
    
    @Test
    void getSyncSettings_ExistingSettings_ReturnsSettings() {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        
        // Act
        CalendarSyncSettings result = syncSettingsService.getSyncSettings(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testSettings.getId(), result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(studentId, result.getStudentId());
        verify(syncSettingsRepository).findByUserIdAndStudentId(userId, studentId);
    }
    
    @Test
    void getSyncSettings_NoExistingSettings_CreatesDefaultSettings() {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.empty());
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        
        // Act
        CalendarSyncSettings result = syncSettingsService.getSyncSettings(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(studentId, result.getStudentId());
        assertTrue(result.getSyncEnabled());
        assertTrue(result.getAutoSyncEnabled());
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void getSyncSettingsDto_ReturnsCorrectDto() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(objectMapper.readValue(eq("[1440,120]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList(1440, 120));
        when(objectMapper.readValue(eq("[120,30]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList(120, 30));
        when(objectMapper.readValue(eq("[\"Math\",\"Science\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("Math", "Science"));
        when(objectMapper.readValue(eq("[\"quiz\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("quiz"));
        
        // Act
        SyncSettingsDto result = syncSettingsService.getSyncSettingsDto(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getSyncEnabled());
        assertTrue(result.getSyncToParentCalendar());
        assertTrue(result.getSyncToStudentCalendar());
        assertFalse(result.getSyncCompletedAssignments());
        assertTrue(result.getAutoSyncEnabled());
    }
    
    @Test
    void updateSyncSettings_ValidDto_UpdatesSettings() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[1440,120]");
        
        SyncSettingsDto updateDto = new SyncSettingsDto();
        updateDto.setSyncEnabled(false);
        updateDto.setSyncToParentCalendar(true);
        updateDto.setSyncToStudentCalendar(false);
        updateDto.setSyncCompletedAssignments(true);
        updateDto.setAutoSyncEnabled(false);
        updateDto.setParentReminderMinutes(Arrays.asList(1440, 120));
        
        // Act
        CalendarSyncSettings result = syncSettingsService.updateSyncSettings(userId, studentId, updateDto);
        
        // Assert
        assertNotNull(result);
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void updateSyncSettings_InvalidDto_ThrowsException() {
        // Arrange
        SyncSettingsDto invalidDto = new SyncSettingsDto();
        invalidDto.setSyncEnabled(null); // Invalid - null value
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                syncSettingsService.updateSyncSettings(userId, studentId, invalidDto));
    }
    
    @Test
    void setSyncEnabled_UpdatesSettings() {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        
        // Act
        syncSettingsService.setSyncEnabled(userId, studentId, false);
        
        // Assert
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void setAutoSyncEnabled_UpdatesSettings() {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        
        // Act
        syncSettingsService.setAutoSyncEnabled(userId, studentId, false);
        
        // Assert
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void updateParentReminderSettings_ValidMinutes_UpdatesSettings() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[60,30]");
        
        List<Integer> newReminders = Arrays.asList(60, 30);
        
        // Act
        syncSettingsService.updateParentReminderSettings(userId, studentId, newReminders);
        
        // Assert
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void updateParentReminderSettings_InvalidMinutes_ThrowsException() {
        // Arrange
        List<Integer> invalidReminders = Arrays.asList(-10, 30); // Negative value
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                syncSettingsService.updateParentReminderSettings(userId, studentId, invalidReminders));
    }
    
    @Test
    void updateParentReminderSettings_ExcessiveMinutes_ThrowsException() {
        // Arrange
        List<Integer> excessiveReminders = Arrays.asList(50000, 30); // > 4 weeks
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                syncSettingsService.updateParentReminderSettings(userId, studentId, excessiveReminders));
    }
    
    @Test
    void updateStudentReminderSettings_ValidMinutes_UpdatesSettings() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[60,15]");
        
        List<Integer> newReminders = Arrays.asList(60, 15);
        
        // Act
        syncSettingsService.updateStudentReminderSettings(userId, studentId, newReminders);
        
        // Assert
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void updateCourseFiltering_UpdatesSettings() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[\"English\",\"History\"]");
        
        List<String> newCourses = Arrays.asList("English", "History");
        
        // Act
        syncSettingsService.updateCourseFiltering(userId, studentId, newCourses);
        
        // Assert
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void updateAssignmentTypeExclusion_UpdatesSettings() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(testSettings);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[\"discussion\",\"practice\"]");
        
        List<String> excludedTypes = Arrays.asList("discussion", "practice");
        
        // Act
        syncSettingsService.updateAssignmentTypeExclusion(userId, studentId, excludedTypes);
        
        // Assert
        verify(syncSettingsRepository).save(any(CalendarSyncSettings.class));
    }
    
    @Test
    void shouldSyncAssignment_SyncDisabled_ReturnsFalse() {
        // Arrange
        testSettings.setSyncEnabled(false);
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        
        PlannerItem assignment = createTestAssignment();
        
        // Act
        boolean result = syncSettingsService.shouldSyncAssignment(userId, studentId, assignment);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void shouldSyncAssignment_CompletedAssignmentWithSyncDisabled_ReturnsFalse() throws Exception {
        // Arrange
        testSettings.setSyncCompletedAssignments(false);
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        
        PlannerItem assignment = createTestAssignment();
        assignment.setSubmitted(true); // Has been submitted (completed)
        
        // Act
        boolean result = syncSettingsService.shouldSyncAssignment(userId, studentId, assignment);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void shouldSyncAssignment_CourseNotIncluded_ReturnsFalse() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(objectMapper.readValue(eq("[\"Math\",\"Science\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("Math", "Science"));
        
        PlannerItem assignment = createTestAssignment();
        assignment.setContextName("History"); // Not in included courses
        
        // Act
        boolean result = syncSettingsService.shouldSyncAssignment(userId, studentId, assignment);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void shouldSyncAssignment_AssignmentTypeExcluded_ReturnsFalse() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(objectMapper.readValue(eq("[\"Math\",\"Science\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("Math", "Science"));
        when(objectMapper.readValue(eq("[\"quiz\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("quiz"));
        
        PlannerItem assignment = createTestAssignment();
        assignment.setContextName("Math");
        assignment.setAssignmentTitle("Math Quiz 1"); // Title contains "quiz" which will be excluded
        
        // Act
        boolean result = syncSettingsService.shouldSyncAssignment(userId, studentId, assignment);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void shouldSyncAssignment_ValidAssignment_ReturnsTrue() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(objectMapper.readValue(eq("[\"Math\",\"Science\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("Math", "Science"));
        when(objectMapper.readValue(eq("[\"quiz\"]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList("quiz"));
        
        PlannerItem assignment = createTestAssignment();
        assignment.setContextName("Math");
        assignment.setAssignmentTitle("Math Homework 1"); // Title contains "homework" which is not excluded
        
        // Act
        boolean result = syncSettingsService.shouldSyncAssignment(userId, studentId, assignment);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void getParentReminderMinutes_ReturnsCorrectList() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(objectMapper.readValue(eq("[1440,120]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList(1440, 120));
        
        // Act
        List<Integer> result = syncSettingsService.getParentReminderMinutes(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(1440));
        assertTrue(result.contains(120));
    }
    
    @Test
    void getStudentReminderMinutes_ReturnsCorrectList() throws Exception {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(userId, studentId))
                .thenReturn(Optional.of(testSettings));
        when(objectMapper.readValue(eq("[120,30]"), any(TypeReference.class)))
                .thenReturn(Arrays.asList(120, 30));
        
        // Act
        List<Integer> result = syncSettingsService.getStudentReminderMinutes(userId, studentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(120));
        assertTrue(result.contains(30));
    }
    
    @Test
    void deleteSyncSettings_CallsRepository() {
        // Act
        syncSettingsService.deleteSyncSettings(userId, studentId);
        
        // Assert
        verify(syncSettingsRepository).deleteByUserIdAndStudentId(userId, studentId);
    }
    
    @Test
    void getUsersWithAutoSyncEnabled_CallsRepository() {
        // Arrange
        List<CalendarSyncSettings> expectedSettings = Arrays.asList(testSettings);
        when(syncSettingsRepository.findByAutoSyncEnabledTrue())
                .thenReturn(expectedSettings);
        
        // Act
        List<CalendarSyncSettings> result = syncSettingsService.getUsersWithAutoSyncEnabled();
        
        // Assert
        assertEquals(expectedSettings, result);
        verify(syncSettingsRepository).findByAutoSyncEnabledTrue();
    }
    
    @Test
    void validateReminderMinutes_NullList_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                syncSettingsService.updateParentReminderSettings(userId, studentId, null));
    }
    
    @Test
    void validateReminderMinutes_EmptyList_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                syncSettingsService.updateParentReminderSettings(userId, studentId, Arrays.asList()));
    }
    
    @Test
    void validateReminderMinutes_NullValue_ThrowsException() {
        // Arrange
        List<Integer> invalidList = Arrays.asList(60, null, 30);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                syncSettingsService.updateParentReminderSettings(userId, studentId, invalidList));
    }
    
    private PlannerItem createTestAssignment() {
        PlannerItem assignment = new PlannerItem();
        assignment.setPlannableId(123L);
        assignment.setAssignmentTitle("Test Assignment");
        assignment.setContextName("Math");
        assignment.setDueAt(LocalDateTime.now().plusDays(1));
        assignment.setPointsPossible(new java.math.BigDecimal("100.0"));
        assignment.setSubmitted(false);
        return assignment;
    }
}