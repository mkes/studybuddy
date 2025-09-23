package com.studytracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.model.*;
import com.studytracker.repository.CalendarEventMappingRepository;
import com.studytracker.repository.CalendarSyncSettingsRepository;
import com.studytracker.repository.PlannerItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTest {
    
    @Mock
    private PlannerItemRepository plannerItemRepository;
    
    @Mock
    private CalendarSyncSettingsRepository syncSettingsRepository;
    
    @Mock
    private CalendarEventMappingRepository eventMappingRepository;
    
    @Mock
    private GoogleCalendarService googleCalendarService;
    
    @Mock
    private CalendarTokenService tokenService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private CalendarSyncService calendarSyncService;
    
    private static final String USER_ID = "user123";
    private static final Long STUDENT_ID = 456L;
    private static final Long ASSIGNMENT_ID = 789L;
    
    private PlannerItem testAssignment;
    private CalendarSyncSettings testSettings;
    private CalendarTokenService.CalendarConnectionStatus connectionStatus;
    
    @BeforeEach
    void setUp() {
        testAssignment = PlannerItem.builder()
                .id(1L)
                .studentId(STUDENT_ID)
                .plannableId(ASSIGNMENT_ID)
                .assignmentTitle("Test Assignment")
                .contextName("Math 101")
                .dueAt(LocalDateTime.now().plusDays(1))
                .pointsPossible(BigDecimal.valueOf(100))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();
        
        testSettings = new CalendarSyncSettings(USER_ID, STUDENT_ID);
        testSettings.setSyncEnabled(true);
        testSettings.setSyncToParentCalendar(true);
        testSettings.setSyncToStudentCalendar(true);
        testSettings.setSyncCompletedAssignments(false);
        testSettings.setAutoSyncEnabled(true);
        
        connectionStatus = new CalendarTokenService.CalendarConnectionStatus(true, true);
    }
    
    @Test
    void syncStudentAssignments_WithValidSettings_ShouldSyncSuccessfully() {
        // Arrange
        List<PlannerItem> assignments = Arrays.asList(testAssignment);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID))
                .thenReturn(assignments);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq(testAssignment), any()))
                .thenReturn("parent-event-id");
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), eq(AccountType.STUDENT), eq(testAssignment), any()))
                .thenReturn("student-event-id");
        when(tokenService.getCalendarId(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of("parent-calendar-id"));
        when(tokenService.getCalendarId(USER_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.of("student-calendar-id"));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncStudentAssignments(USER_ID, STUDENT_ID);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreatedCount()).isEqualTo(2); // Parent and student calendars
        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(result.getDeletedCount()).isEqualTo(0);
        assertThat(result.getErrorCount()).isEqualTo(0);
        
        verify(eventMappingRepository, times(2)).save(any(CalendarEventMapping.class));
    }
    
    @Test
    void syncStudentAssignments_WithSyncDisabled_ShouldReturnDisabled() {
        // Arrange
        testSettings.setSyncEnabled(false);
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncStudentAssignments(USER_ID, STUDENT_ID);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo("disabled");
        verify(plannerItemRepository, never()).findByStudentIdOrderByDueAtDesc(any());
    }
    
    @Test
    void syncStudentAssignments_WithNoCalendarsConnected_ShouldReturnNoCalendarsConnected() {
        // Arrange
        CalendarTokenService.CalendarConnectionStatus noConnection = 
                new CalendarTokenService.CalendarConnectionStatus(false, false);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(noConnection);
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncStudentAssignments(USER_ID, STUDENT_ID);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo("no_calendars_connected");
        verify(plannerItemRepository, never()).findByStudentIdOrderByDueAtDesc(any());
    }
    
    @Test
    void syncStudentAssignments_WithOnlyParentCalendarConnected_ShouldSyncOnlyToParent() {
        // Arrange
        List<PlannerItem> assignments = Arrays.asList(testAssignment);
        CalendarTokenService.CalendarConnectionStatus parentOnlyConnection = 
                new CalendarTokenService.CalendarConnectionStatus(true, false);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(parentOnlyConnection);
        when(plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID))
                .thenReturn(assignments);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq(testAssignment), any()))
                .thenReturn("parent-event-id");
        when(tokenService.getCalendarId(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of("parent-calendar-id"));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncStudentAssignments(USER_ID, STUDENT_ID);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreatedCount()).isEqualTo(1); // Only parent calendar
        
        verify(googleCalendarService, times(1)).createAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq(testAssignment), any());
        verify(googleCalendarService, never()).createAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.STUDENT), eq(testAssignment), any());
    }
    
    @Test
    void syncSingleAssignment_WithExistingEvent_ShouldUpdateEvent() {
        // Arrange
        CalendarEventMapping existingMapping = new CalendarEventMapping();
        existingMapping.setAssignmentId(ASSIGNMENT_ID);
        existingMapping.setStudentId(STUDENT_ID);
        existingMapping.setAccountType(AccountType.PARENT);
        existingMapping.setGoogleEventId("existing-event-id");
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of(existingMapping));
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        when(googleCalendarService.updateAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq("existing-event-id"), eq(testAssignment), any()))
                .thenReturn(true);
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), eq(AccountType.STUDENT), eq(testAssignment), any()))
                .thenReturn("student-event-id");
        when(tokenService.getCalendarId(USER_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.of("student-calendar-id"));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncSingleAssignment(USER_ID, STUDENT_ID, testAssignment);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdatedCount()).isEqualTo(1); // Parent event updated
        assertThat(result.getCreatedCount()).isEqualTo(1); // Student event created
        
        verify(googleCalendarService).updateAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq("existing-event-id"), eq(testAssignment), any());
        verify(eventMappingRepository).save(existingMapping); // Updated mapping
    }
    
    @Test
    void syncSingleAssignment_WithCompletedAssignmentAndSyncDisabled_ShouldReturnFiltered() {
        // Arrange
        testAssignment.setSubmitted(true);
        testSettings.setSyncCompletedAssignments(false);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncSingleAssignment(USER_ID, STUDENT_ID, testAssignment);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo("filtered");
        verify(googleCalendarService, never()).createAssignmentEvent(any(), any(), any(), any(), any());
    }
    
    @Test
    void syncSingleAssignment_WithNoDueDate_ShouldReturnFiltered() {
        // Arrange
        testAssignment.setDueAt(null);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncSingleAssignment(USER_ID, STUDENT_ID, testAssignment);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo("filtered");
        verify(googleCalendarService, never()).createAssignmentEvent(any(), any(), any(), any(), any());
    }
    
    @Test
    void performIncrementalSync_WithOrphanedMappings_ShouldCleanupEvents() {
        // Arrange
        LocalDateTime lastSyncTime = LocalDateTime.now().minusHours(1);
        List<PlannerItem> updatedAssignments = Arrays.asList(testAssignment);
        
        CalendarEventMapping orphanedMapping = new CalendarEventMapping();
        orphanedMapping.setAssignmentId(999L); // Different assignment ID
        orphanedMapping.setStudentId(STUDENT_ID);
        orphanedMapping.setAccountType(AccountType.PARENT);
        orphanedMapping.setGoogleEventId("orphaned-event-id");
        
        List<CalendarEventMapping> existingMappings = Arrays.asList(orphanedMapping);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID))
                .thenReturn(updatedAssignments);
        when(eventMappingRepository.findByStudentId(STUDENT_ID))
                .thenReturn(existingMappings);
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(googleCalendarService.deleteAssignmentEvent(
                USER_ID, STUDENT_ID, AccountType.PARENT, "orphaned-event-id"))
                .thenReturn(true);
        
        // Mock the sync of updated assignments
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq(testAssignment), any()))
                .thenReturn("parent-event-id");
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), eq(AccountType.STUDENT), eq(testAssignment), any()))
                .thenReturn("student-event-id");
        when(tokenService.getCalendarId(USER_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of("parent-calendar-id"));
        when(tokenService.getCalendarId(USER_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.of("student-calendar-id"));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.performIncrementalSync(USER_ID, STUDENT_ID, lastSyncTime);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeletedCount()).isEqualTo(1); // Orphaned event deleted
        assertThat(result.getCreatedCount()).isEqualTo(2); // New events created
        
        verify(googleCalendarService).deleteAssignmentEvent(
                USER_ID, STUDENT_ID, AccountType.PARENT, "orphaned-event-id");
        verify(eventMappingRepository).delete(orphanedMapping);
    }
    
    @Test
    void performBatchSync_WithMultipleAssignments_ShouldProcessInBatches() throws Exception {
        // Arrange
        List<PlannerItem> assignments = new ArrayList<>();
        for (int i = 0; i < 75; i++) { // More than batch size (50)
            PlannerItem assignment = PlannerItem.builder()
                    .id((long) i)
                    .studentId(STUDENT_ID)
                    .plannableId((long) (1000 + i))
                    .assignmentTitle("Assignment " + i)
                    .contextName("Course " + i)
                    .dueAt(LocalDateTime.now().plusDays(i))
                    .pointsPossible(BigDecimal.valueOf(100))
                    .submitted(false)
                    .build();
            assignments.add(assignment);
        }
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                anyLong(), eq(STUDENT_ID), any(AccountType.class)))
                .thenReturn(Optional.empty());
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), any(AccountType.class), any(PlannerItem.class), any()))
                .thenReturn("event-id");
        when(tokenService.getCalendarId(eq(USER_ID), eq(STUDENT_ID), any(AccountType.class)))
                .thenReturn(Optional.of("calendar-id"));
        
        // Act
        CompletableFuture<CalendarSyncService.SyncResult> futureResult = 
                calendarSyncService.performBatchSync(USER_ID, STUDENT_ID, assignments);
        CalendarSyncService.SyncResult result = futureResult.get();
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreatedCount()).isEqualTo(150); // 75 assignments * 2 calendars
        
        // Verify that Google Calendar service was called for each assignment and calendar
        verify(googleCalendarService, times(75)).createAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), any(PlannerItem.class), any());
        verify(googleCalendarService, times(75)).createAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.STUDENT), any(PlannerItem.class), any());
    }
    
    @Test
    void applySyncSettings_WithSettingsRequiringResync_ShouldTriggerResync() {
        // Arrange
        CalendarSyncSettings existingSettings = new CalendarSyncSettings(USER_ID, STUDENT_ID);
        existingSettings.setSyncEnabled(true);
        existingSettings.setSyncToParentCalendar(true);
        existingSettings.setSyncToStudentCalendar(false);
        
        CalendarSyncSettings newSettings = new CalendarSyncSettings(USER_ID, STUDENT_ID);
        newSettings.setSyncEnabled(true);
        newSettings.setSyncToParentCalendar(true);
        newSettings.setSyncToStudentCalendar(true); // Changed - requires resync
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(existingSettings));
        when(syncSettingsRepository.save(any(CalendarSyncSettings.class)))
                .thenReturn(newSettings);
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID))
                .thenReturn(Arrays.asList(testAssignment));
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                anyLong(), eq(STUDENT_ID), any(AccountType.class)))
                .thenReturn(Optional.empty());
        when(googleCalendarService.createAssignmentEvent(eq(USER_ID), eq(STUDENT_ID), any(AccountType.class), any(PlannerItem.class), any()))
                .thenReturn("event-id");
        when(tokenService.getCalendarId(eq(USER_ID), eq(STUDENT_ID), any(AccountType.class)))
                .thenReturn(Optional.of("calendar-id"));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.applySyncSettings(USER_ID, STUDENT_ID, newSettings);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreatedCount()).isGreaterThan(0); // Resync occurred
        
        verify(syncSettingsRepository).save(newSettings);
    }
    
    @Test
    void scheduleAutomaticSync_WithAutoSyncEnabled_ShouldScheduleSync() {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        
        // Act & Assert - Should not throw exception
        assertThatCode(() -> calendarSyncService.scheduleAutomaticSync(USER_ID, STUDENT_ID))
                .doesNotThrowAnyException();
    }
    
    @Test
    void scheduleAutomaticSync_WithAutoSyncDisabled_ShouldNotSchedule() {
        // Arrange
        testSettings.setAutoSyncEnabled(false);
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        
        // Act & Assert - Should not throw exception
        assertThatCode(() -> calendarSyncService.scheduleAutomaticSync(USER_ID, STUDENT_ID))
                .doesNotThrowAnyException();
    }
    
    @Test
    void syncResult_MergeOperation_ShouldCombineResults() {
        // Arrange
        CalendarSyncService.SyncResult result1 = CalendarSyncService.SyncResult.created();
        result1.incrementUpdated();
        
        CalendarSyncService.SyncResult result2 = CalendarSyncService.SyncResult.updated();
        result2.incrementDeleted();
        result2.addError("Test error");
        
        // Act
        result1.merge(result2);
        
        // Assert
        assertThat(result1.getCreatedCount()).isEqualTo(1);
        assertThat(result1.getUpdatedCount()).isEqualTo(2);
        assertThat(result1.getDeletedCount()).isEqualTo(1);
        assertThat(result1.getErrorCount()).isEqualTo(1);
        assertThat(result1.getErrors()).contains("Test error");
        assertThat(result1.getStatus()).isEqualTo("error");
    }
    
    @Test
    void syncResult_StaticFactoryMethods_ShouldCreateCorrectResults() {
        // Test success result
        CalendarSyncService.SyncResult success = CalendarSyncService.SyncResult.success();
        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getStatus()).isEqualTo("success");
        
        // Test created result
        CalendarSyncService.SyncResult created = CalendarSyncService.SyncResult.created();
        assertThat(created.getCreatedCount()).isEqualTo(1);
        
        // Test updated result
        CalendarSyncService.SyncResult updated = CalendarSyncService.SyncResult.updated();
        assertThat(updated.getUpdatedCount()).isEqualTo(1);
        
        // Test deleted result
        CalendarSyncService.SyncResult deleted = CalendarSyncService.SyncResult.deleted();
        assertThat(deleted.getDeletedCount()).isEqualTo(1);
        
        // Test error result
        CalendarSyncService.SyncResult error = CalendarSyncService.SyncResult.error("Test error");
        assertThat(error.getStatus()).isEqualTo("error");
        assertThat(error.getErrorCount()).isEqualTo(1);
        assertThat(error.getErrors()).contains("Test error");
        
        // Test disabled result
        CalendarSyncService.SyncResult disabled = CalendarSyncService.SyncResult.disabled();
        assertThat(disabled.getStatus()).isEqualTo("disabled");
        
        // Test no calendars connected result
        CalendarSyncService.SyncResult noCalendars = CalendarSyncService.SyncResult.noCalendarsConnected();
        assertThat(noCalendars.getStatus()).isEqualTo("no_calendars_connected");
        
        // Test filtered result
        CalendarSyncService.SyncResult filtered = CalendarSyncService.SyncResult.filtered();
        assertThat(filtered.getStatus()).isEqualTo("filtered");
    }
    
    @Test
    void handleAssignmentStatusChange_WithCompletedStatus_ShouldMarkEventAsCompleted() {
        // Arrange
        testAssignment.setSubmitted(true);
        testSettings.setSyncCompletedAssignments(true);
        
        CalendarEventMapping existingMapping = new CalendarEventMapping();
        existingMapping.setAssignmentId(ASSIGNMENT_ID);
        existingMapping.setStudentId(STUDENT_ID);
        existingMapping.setAccountType(AccountType.PARENT);
        existingMapping.setGoogleEventId("existing-event-id");
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of(existingMapping));
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.of(existingMapping));
        when(googleCalendarService.markEventAsCompleted(
                USER_ID, STUDENT_ID, AccountType.PARENT, "existing-event-id", testAssignment))
                .thenReturn(true);
        when(googleCalendarService.markEventAsCompleted(
                USER_ID, STUDENT_ID, AccountType.STUDENT, "existing-event-id", testAssignment))
                .thenReturn(true);
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.handleAssignmentStatusChange(
                USER_ID, STUDENT_ID, testAssignment, "completed");
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdatedCount()).isEqualTo(2); // Both parent and student events marked as completed
        
        verify(googleCalendarService, times(2)).markEventAsCompleted(
                eq(USER_ID), eq(STUDENT_ID), any(AccountType.class), eq("existing-event-id"), eq(testAssignment));
        verify(eventMappingRepository, times(2)).save(any(CalendarEventMapping.class));
    }
    
    @Test
    void handleAssignmentStatusChange_WithDeletedStatus_ShouldDeleteEvents() {
        // Arrange
        CalendarEventMapping existingMapping = new CalendarEventMapping();
        existingMapping.setAssignmentId(ASSIGNMENT_ID);
        existingMapping.setStudentId(STUDENT_ID);
        existingMapping.setAccountType(AccountType.PARENT);
        existingMapping.setGoogleEventId("existing-event-id");
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of(existingMapping));
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.of(existingMapping));
        when(googleCalendarService.deleteAssignmentEvent(
                USER_ID, STUDENT_ID, AccountType.PARENT, "existing-event-id"))
                .thenReturn(true);
        when(googleCalendarService.deleteAssignmentEvent(
                USER_ID, STUDENT_ID, AccountType.STUDENT, "existing-event-id"))
                .thenReturn(true);
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.handleAssignmentStatusChange(
                USER_ID, STUDENT_ID, testAssignment, "deleted");
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeletedCount()).isEqualTo(2); // Both parent and student events deleted
        
        verify(googleCalendarService, times(2)).deleteAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), any(AccountType.class), eq("existing-event-id"));
        verify(eventMappingRepository, times(2)).delete(any(CalendarEventMapping.class));
    }
    
    @Test
    void handleAssignmentStatusChange_WithUpdatedStatus_ShouldUpdateEvents() {
        // Arrange
        CalendarEventMapping existingMapping = new CalendarEventMapping();
        existingMapping.setAssignmentId(ASSIGNMENT_ID);
        existingMapping.setStudentId(STUDENT_ID);
        existingMapping.setAccountType(AccountType.PARENT);
        existingMapping.setGoogleEventId("existing-event-id");
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of(existingMapping));
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.of(existingMapping));
        when(googleCalendarService.updateAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.PARENT), eq("existing-event-id"), 
                eq(testAssignment), any()))
                .thenReturn(true);
        when(googleCalendarService.updateAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), eq(AccountType.STUDENT), eq("existing-event-id"), 
                eq(testAssignment), any()))
                .thenReturn(true);
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.handleAssignmentStatusChange(
                USER_ID, STUDENT_ID, testAssignment, "updated");
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdatedCount()).isEqualTo(2); // Both parent and student events updated
        
        verify(googleCalendarService, times(2)).updateAssignmentEvent(
                eq(USER_ID), eq(STUDENT_ID), any(AccountType.class), eq("existing-event-id"), 
                eq(testAssignment), any());
        verify(eventMappingRepository, times(2)).save(any(CalendarEventMapping.class));
    }
    
    @Test
    void handleAssignmentStatusChange_WithSyncDisabled_ShouldReturnDisabled() {
        // Arrange
        testSettings.setSyncEnabled(false);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.handleAssignmentStatusChange(
                USER_ID, STUDENT_ID, testAssignment, "completed");
        
        // Assert
        assertThat(result.getStatus()).isEqualTo("disabled");
        verify(googleCalendarService, never()).markEventAsCompleted(any(), any(), any(), any(), any());
    }
    
    @Test
    void handleAssignmentStatusChange_WithNoEventMapping_ShouldReturnSuccess() {
        // Arrange
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(connectionStatus);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.empty());
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.handleAssignmentStatusChange(
                USER_ID, STUDENT_ID, testAssignment, "completed");
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalProcessed()).isEqualTo(0);
        
        verify(googleCalendarService, never()).markEventAsCompleted(any(), any(), any(), any(), any());
    }
    
    @Test
    void syncSingleAssignmentToCalendar_WithCompletedAssignmentAndSyncCompletedDisabled_ShouldDeleteEvent() {
        // Arrange
        testAssignment.setSubmitted(true);
        testSettings.setSyncCompletedAssignments(false);
        testSettings.setSyncToStudentCalendar(false); // Only sync to parent calendar
        
        CalendarEventMapping existingMapping = new CalendarEventMapping();
        existingMapping.setAssignmentId(ASSIGNMENT_ID);
        existingMapping.setStudentId(STUDENT_ID);
        existingMapping.setAccountType(AccountType.PARENT);
        existingMapping.setGoogleEventId("existing-event-id");
        
        CalendarTokenService.CalendarConnectionStatus parentOnlyConnection = 
                new CalendarTokenService.CalendarConnectionStatus(true, false);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(parentOnlyConnection);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of(existingMapping));
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.STUDENT))
                .thenReturn(Optional.empty());
        when(googleCalendarService.deleteAssignmentEvent(
                USER_ID, STUDENT_ID, AccountType.PARENT, "existing-event-id"))
                .thenReturn(true);
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncSingleAssignment(
                USER_ID, STUDENT_ID, testAssignment);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeletedCount()).isEqualTo(1); // Parent event deleted
        
        verify(googleCalendarService).deleteAssignmentEvent(
                USER_ID, STUDENT_ID, AccountType.PARENT, "existing-event-id");
        verify(eventMappingRepository).delete(existingMapping);
    }
    
    @Test
    void syncSingleAssignmentToCalendar_WithCompletedAssignmentAndSyncCompletedEnabled_ShouldMarkAsCompleted() {
        // Arrange
        testAssignment.setSubmitted(true);
        testSettings.setSyncCompletedAssignments(true);
        testSettings.setSyncToStudentCalendar(false); // Only sync to parent calendar
        
        CalendarEventMapping existingMapping = new CalendarEventMapping();
        existingMapping.setAssignmentId(ASSIGNMENT_ID);
        existingMapping.setStudentId(STUDENT_ID);
        existingMapping.setAccountType(AccountType.PARENT);
        existingMapping.setGoogleEventId("existing-event-id");
        
        CalendarTokenService.CalendarConnectionStatus parentOnlyConnection = 
                new CalendarTokenService.CalendarConnectionStatus(true, false);
        
        when(syncSettingsRepository.findByUserIdAndStudentId(USER_ID, STUDENT_ID))
                .thenReturn(Optional.of(testSettings));
        when(tokenService.getConnectionStatus(USER_ID, STUDENT_ID))
                .thenReturn(parentOnlyConnection);
        when(eventMappingRepository.findByAssignmentIdAndStudentIdAndAccountType(
                ASSIGNMENT_ID, STUDENT_ID, AccountType.PARENT))
                .thenReturn(Optional.of(existingMapping));
        when(googleCalendarService.markEventAsCompleted(
                USER_ID, STUDENT_ID, AccountType.PARENT, "existing-event-id", testAssignment))
                .thenReturn(true);
        
        // Act
        CalendarSyncService.SyncResult result = calendarSyncService.syncSingleAssignment(
                USER_ID, STUDENT_ID, testAssignment);
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdatedCount()).isEqualTo(1); // Parent event marked as completed
        
        verify(googleCalendarService).markEventAsCompleted(
                USER_ID, STUDENT_ID, AccountType.PARENT, "existing-event-id", testAssignment);
        verify(eventMappingRepository).save(existingMapping);
    }
}