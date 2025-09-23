package com.studytracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.exception.GoogleCalendarException;
import com.studytracker.exception.SyncTimeoutException;
import com.studytracker.model.*;
import com.studytracker.repository.CalendarEventMappingRepository;
import com.studytracker.repository.CalendarSyncSettingsRepository;
import com.studytracker.repository.PlannerItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for orchestrating calendar synchronization between Canvas assignments and Google Calendar.
 * Handles filtering, dual calendar support, incremental sync, and batch operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarSyncService {
    
    private final PlannerItemRepository plannerItemRepository;
    private final CalendarSyncSettingsRepository syncSettingsRepository;
    private final CalendarEventMappingRepository eventMappingRepository;
    private final GoogleCalendarService googleCalendarService;
    private final CalendarTokenService tokenService;
    private final ObjectMapper objectMapper;
    
    private static final int SYNC_TIMEOUT_MINUTES = 10;
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Synchronize all assignments for a student to connected calendars
     */
    @Transactional
    public SyncResult syncStudentAssignments(String userId, Long studentId) {
        log.info("Starting assignment sync for user {} student {}", userId, studentId);
        
        try {
            // Get sync settings
            CalendarSyncSettings settings = getOrCreateSyncSettings(userId, studentId);
            
            if (!settings.getSyncEnabled()) {
                log.info("Sync disabled for user {} student {}", userId, studentId);
                return SyncResult.disabled();
            }
            
            // Check calendar connections
            CalendarTokenService.CalendarConnectionStatus connectionStatus = 
                    tokenService.getConnectionStatus(userId, studentId);
            
            if (!connectionStatus.isAnyConnected()) {
                log.warn("No calendars connected for user {} student {}", userId, studentId);
                return SyncResult.noCalendarsConnected();
            }
            
            // Get assignments to sync
            List<PlannerItem> assignments = getAssignmentsToSync(studentId, settings);
            log.info("Found {} assignments to sync for user {} student {}", 
                    assignments.size(), userId, studentId);
            
            // Perform sync for each connected calendar
            SyncResult result = new SyncResult();
            
            if (settings.getSyncToParentCalendar() && connectionStatus.isParentConnected()) {
                SyncResult parentResult = syncToCalendar(userId, studentId, AccountType.PARENT, 
                        assignments, settings);
                result.merge(parentResult);
            }
            
            if (settings.getSyncToStudentCalendar() && connectionStatus.isStudentConnected()) {
                SyncResult studentResult = syncToCalendar(userId, studentId, AccountType.STUDENT, 
                        assignments, settings);
                result.merge(studentResult);
            }
            
            log.info("Completed assignment sync for user {} student {} - Created: {}, Updated: {}, Deleted: {}, Errors: {}", 
                    userId, studentId, result.getCreatedCount(), result.getUpdatedCount(), 
                    result.getDeletedCount(), result.getErrorCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to sync assignments for user {} student {}", userId, studentId, e);
            return SyncResult.error("Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Synchronize a single assignment to connected calendars
     */
    @Transactional
    public SyncResult syncSingleAssignment(String userId, Long studentId, PlannerItem assignment) {
        log.info("Starting single assignment sync for user {} student {} assignment {}", 
                userId, studentId, assignment.getPlannableId());
        
        try {
            CalendarSyncSettings settings = getOrCreateSyncSettings(userId, studentId);
            
            if (!settings.getSyncEnabled()) {
                return SyncResult.disabled();
            }
            
            // Check if assignment should be synced based on settings
            if (!shouldSyncAssignment(assignment, settings)) {
                log.debug("Assignment {} filtered out by sync settings", assignment.getPlannableId());
                return SyncResult.filtered();
            }
            
            CalendarTokenService.CalendarConnectionStatus connectionStatus = 
                    tokenService.getConnectionStatus(userId, studentId);
            
            if (!connectionStatus.isAnyConnected()) {
                return SyncResult.noCalendarsConnected();
            }
            
            SyncResult result = new SyncResult();
            
            if (settings.getSyncToParentCalendar() && connectionStatus.isParentConnected()) {
                SyncResult parentResult = syncSingleAssignmentToCalendar(userId, studentId, 
                        AccountType.PARENT, assignment, settings);
                result.merge(parentResult);
            }
            
            if (settings.getSyncToStudentCalendar() && connectionStatus.isStudentConnected()) {
                SyncResult studentResult = syncSingleAssignmentToCalendar(userId, studentId, 
                        AccountType.STUDENT, assignment, settings);
                result.merge(studentResult);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to sync single assignment for user {} student {} assignment {}", 
                    userId, studentId, assignment.getPlannableId(), e);
            return SyncResult.error("Single assignment sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform incremental sync to handle assignment updates and deletions
     */
    @Transactional
    public SyncResult performIncrementalSync(String userId, Long studentId, LocalDateTime lastSyncTime) {
        log.info("Starting incremental sync for user {} student {} since {}", 
                userId, studentId, lastSyncTime);
        
        try {
            CalendarSyncSettings settings = getOrCreateSyncSettings(userId, studentId);
            
            if (!settings.getSyncEnabled()) {
                return SyncResult.disabled();
            }
            
            // Get assignments updated since last sync
            List<PlannerItem> updatedAssignments = getAssignmentsUpdatedSince(studentId, lastSyncTime);
            
            // Get existing event mappings that might need cleanup
            List<CalendarEventMapping> existingMappings = eventMappingRepository.findByStudentId(studentId);
            
            // Find assignments that were deleted (have mappings but no current assignment)
            Set<Long> currentAssignmentIds = updatedAssignments.stream()
                    .map(PlannerItem::getPlannableId)
                    .collect(Collectors.toSet());
            
            List<CalendarEventMapping> orphanedMappings = existingMappings.stream()
                    .filter(mapping -> !currentAssignmentIds.contains(mapping.getAssignmentId()))
                    .collect(Collectors.toList());
            
            SyncResult result = new SyncResult();
            
            // Sync updated assignments
            if (!updatedAssignments.isEmpty()) {
                SyncResult updateResult = syncStudentAssignments(userId, studentId);
                result.merge(updateResult);
            }
            
            // Clean up orphaned events
            if (!orphanedMappings.isEmpty()) {
                SyncResult cleanupResult = cleanupOrphanedEvents(userId, studentId, orphanedMappings);
                result.merge(cleanupResult);
            }
            
            log.info("Completed incremental sync for user {} student {} - Updated: {}, Cleaned: {}", 
                    userId, studentId, updatedAssignments.size(), orphanedMappings.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed incremental sync for user {} student {}", userId, studentId, e);
            return SyncResult.error("Incremental sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform batch synchronization for improved performance
     */
    @Async
    public CompletableFuture<SyncResult> performBatchSync(String userId, Long studentId, 
                                                         List<PlannerItem> assignments) {
        log.info("Starting batch sync for user {} student {} with {} assignments", 
                userId, studentId, assignments.size());
        
        try {
            CalendarSyncSettings settings = getOrCreateSyncSettings(userId, studentId);
            
            if (!settings.getSyncEnabled()) {
                return CompletableFuture.completedFuture(SyncResult.disabled());
            }
            
            // Process assignments in batches
            SyncResult result = new SyncResult();
            List<List<PlannerItem>> batches = partitionList(assignments, BATCH_SIZE);
            
            for (int i = 0; i < batches.size(); i++) {
                List<PlannerItem> batch = batches.get(i);
                log.debug("Processing batch {}/{} with {} assignments", i + 1, batches.size(), batch.size());
                
                SyncResult batchResult = processBatch(userId, studentId, batch, settings);
                result.merge(batchResult);
                
                // Add small delay between batches to avoid rate limiting
                if (i < batches.size() - 1) {
                    Thread.sleep(100);
                }
            }
            
            log.info("Completed batch sync for user {} student {} - Total processed: {}", 
                    userId, studentId, assignments.size());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Failed batch sync for user {} student {}", userId, studentId, e);
            return CompletableFuture.completedFuture(SyncResult.error("Batch sync failed: " + e.getMessage()));
        }
    }
    
    /**
     * Schedule automatic sync for a student
     */
    public void scheduleAutomaticSync(String userId, Long studentId) {
        log.info("Scheduling automatic sync for user {} student {}", userId, studentId);
        
        CalendarSyncSettings settings = getOrCreateSyncSettings(userId, studentId);
        
        if (!settings.getAutoSyncEnabled()) {
            log.info("Auto sync disabled for user {} student {}", userId, studentId);
            return;
        }
        
        // This would integrate with Spring's @Scheduled or a job scheduler
        // For now, we'll just log the scheduling request
        log.info("Auto sync scheduled for user {} student {}", userId, studentId);
    }
    
    /**
     * Apply sync settings and update existing events if needed
     */
    @Transactional
    public SyncResult applySyncSettings(String userId, Long studentId, CalendarSyncSettings newSettings) {
        log.info("Applying sync settings for user {} student {}", userId, studentId);
        
        try {
            CalendarSyncSettings existingSettings = getOrCreateSyncSettings(userId, studentId);
            
            // Check if settings changed in a way that requires re-sync
            boolean requiresResync = settingsRequireResync(existingSettings, newSettings);
            
            // Save new settings
            newSettings.setUserId(userId);
            newSettings.setStudentId(studentId);
            syncSettingsRepository.save(newSettings);
            
            if (requiresResync && newSettings.getSyncEnabled()) {
                log.info("Settings change requires re-sync for user {} student {}", userId, studentId);
                return syncStudentAssignments(userId, studentId);
            }
            
            return SyncResult.success();
            
        } catch (Exception e) {
            log.error("Failed to apply sync settings for user {} student {}", userId, studentId, e);
            return SyncResult.error("Failed to apply settings: " + e.getMessage());
        }
    }
    
    /**
     * Get or create sync settings with defaults
     */
    private CalendarSyncSettings getOrCreateSyncSettings(String userId, Long studentId) {
        return syncSettingsRepository.findByUserIdAndStudentId(userId, studentId)
                .orElseGet(() -> {
                    CalendarSyncSettings settings = new CalendarSyncSettings(userId, studentId);
                    return syncSettingsRepository.save(settings);
                });
    }
    
    /**
     * Get assignments that should be synced based on settings
     */
    private List<PlannerItem> getAssignmentsToSync(Long studentId, CalendarSyncSettings settings) {
        List<PlannerItem> allAssignments = plannerItemRepository.findByStudentIdOrderByDueAtDesc(studentId);
        
        return allAssignments.stream()
                .filter(assignment -> shouldSyncAssignment(assignment, settings))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if an assignment should be synced based on settings
     */
    private boolean shouldSyncAssignment(PlannerItem assignment, CalendarSyncSettings settings) {
        // Check if completed assignments should be synced
        if (assignment.getSubmitted() && !settings.getSyncCompletedAssignments()) {
            return false;
        }
        
        // Check course filtering
        if (settings.getIncludedCourses() != null && !settings.getIncludedCourses().trim().isEmpty()) {
            List<String> includedCourses = parseJsonStringList(settings.getIncludedCourses());
            if (!includedCourses.isEmpty() && 
                !includedCourses.contains(assignment.getContextName())) {
                return false;
            }
        }
        
        // Check assignment type filtering
        if (settings.getExcludedAssignmentTypes() != null && !settings.getExcludedAssignmentTypes().trim().isEmpty()) {
            List<String> excludedTypes = parseJsonStringList(settings.getExcludedAssignmentTypes());
            // This would need assignment type information from Canvas API
            // For now, we'll assume all assignments are included
        }
        
        // Check if assignment has a due date (required for calendar events)
        return assignment.getDueAt() != null;
    }
    
    /**
     * Sync assignments to a specific calendar (parent or student)
     */
    private SyncResult syncToCalendar(String userId, Long studentId, AccountType accountType,
                                    List<PlannerItem> assignments, CalendarSyncSettings settings) {
        SyncResult result = new SyncResult();
        
        for (PlannerItem assignment : assignments) {
            try {
                SyncResult assignmentResult = syncSingleAssignmentToCalendar(userId, studentId, 
                        accountType, assignment, settings);
                result.merge(assignmentResult);
                
            } catch (Exception e) {
                log.error("Failed to sync assignment {} to {} calendar for user {} student {}", 
                        assignment.getPlannableId(), accountType, userId, studentId, e);
                result.addError("Failed to sync assignment " + assignment.getPlannableId() + ": " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Sync a single assignment to a specific calendar
     */
    private SyncResult syncSingleAssignmentToCalendar(String userId, Long studentId, AccountType accountType,
                                                    PlannerItem assignment, CalendarSyncSettings settings) {
        try {
            // Get custom reminder settings for this account type
            List<Integer> customReminders = getReminderSettings(settings, accountType);
            
            // Check if event mapping already exists
            Optional<CalendarEventMapping> existingMapping = eventMappingRepository
                    .findByAssignmentIdAndStudentIdAndAccountType(assignment.getPlannableId(), 
                            studentId, accountType);
            
            if (existingMapping.isPresent()) {
                // Handle existing event based on assignment status
                CalendarEventMapping mapping = existingMapping.get();
                
                if (assignment.getSubmitted() && !settings.getSyncCompletedAssignments()) {
                    // Assignment completed and user doesn't want completed assignments synced
                    boolean deleted = googleCalendarService.deleteAssignmentEvent(userId, studentId, 
                            accountType, mapping.getGoogleEventId());
                    
                    if (deleted) {
                        eventMappingRepository.delete(mapping);
                        return SyncResult.deleted();
                    } else {
                        return SyncResult.error("Failed to delete completed assignment event");
                    }
                } else if (assignment.getSubmitted()) {
                    // Mark as completed but keep the event
                    boolean updated = googleCalendarService.markEventAsCompleted(userId, studentId, 
                            accountType, mapping.getGoogleEventId(), assignment);
                    
                    if (updated) {
                        mapping.setLastSyncedAt(LocalDateTime.now());
                        eventMappingRepository.save(mapping);
                        return SyncResult.updated();
                    } else {
                        return SyncResult.error("Failed to mark event as completed");
                    }
                } else {
                    // Update existing event with current assignment data
                    boolean updated = googleCalendarService.updateAssignmentEvent(userId, studentId, 
                            accountType, mapping.getGoogleEventId(), assignment, customReminders);
                    
                    if (updated) {
                        mapping.setLastSyncedAt(LocalDateTime.now());
                        eventMappingRepository.save(mapping);
                        return SyncResult.updated();
                    } else {
                        return SyncResult.error("Failed to update event");
                    }
                }
                
            } else {
                // Create new event only if assignment is not completed or user wants completed assignments
                if (assignment.getSubmitted() && !settings.getSyncCompletedAssignments()) {
                    log.debug("Skipping completed assignment {} as sync completed assignments is disabled", 
                            assignment.getPlannableId());
                    return SyncResult.filtered();
                }
                
                String eventId = googleCalendarService.createAssignmentEvent(userId, studentId, 
                        accountType, assignment, customReminders);
                
                if (eventId != null) {
                    // Create mapping
                    CalendarEventMapping mapping = new CalendarEventMapping();
                    mapping.setAssignmentId(assignment.getPlannableId());
                    mapping.setStudentId(studentId);
                    mapping.setAccountType(accountType);
                    mapping.setGoogleEventId(eventId);
                    mapping.setGoogleCalendarId(tokenService.getCalendarId(userId, studentId, accountType).orElse(""));
                    mapping.setLastSyncedAt(LocalDateTime.now());
                    eventMappingRepository.save(mapping);
                    
                    return SyncResult.created();
                } else {
                    return SyncResult.error("Failed to create event");
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to sync assignment {} to {} calendar", assignment.getPlannableId(), accountType, e);
            return SyncResult.error("Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Get reminder settings for specific account type from sync settings
     */
    private List<Integer> getReminderSettings(CalendarSyncSettings settings, AccountType accountType) {
        String reminderJson = (accountType == AccountType.PARENT) ? 
                settings.getParentReminderMinutes() : settings.getStudentReminderMinutes();
        
        return GoogleCalendarService.parseReminderMinutes(reminderJson);
    }
    
    /**
     * Handle assignment status changes (completion, deletion, etc.)
     */
    @Transactional
    public SyncResult handleAssignmentStatusChange(String userId, Long studentId, PlannerItem assignment, 
                                                 String statusChange) {
        log.info("Handling assignment status change for user {} student {} assignment {} status {}", 
                userId, studentId, assignment.getPlannableId(), statusChange);
        
        try {
            CalendarSyncSettings settings = getOrCreateSyncSettings(userId, studentId);
            
            if (!settings.getSyncEnabled()) {
                return SyncResult.disabled();
            }
            
            CalendarTokenService.CalendarConnectionStatus connectionStatus = 
                    tokenService.getConnectionStatus(userId, studentId);
            
            if (!connectionStatus.isAnyConnected()) {
                return SyncResult.noCalendarsConnected();
            }
            
            SyncResult result = new SyncResult();
            
            // Handle status change for each connected calendar
            if (settings.getSyncToParentCalendar() && connectionStatus.isParentConnected()) {
                SyncResult parentResult = handleStatusChangeForCalendar(userId, studentId, 
                        AccountType.PARENT, assignment, statusChange, settings);
                result.merge(parentResult);
            }
            
            if (settings.getSyncToStudentCalendar() && connectionStatus.isStudentConnected()) {
                SyncResult studentResult = handleStatusChangeForCalendar(userId, studentId, 
                        AccountType.STUDENT, assignment, statusChange, settings);
                result.merge(studentResult);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to handle assignment status change for user {} student {} assignment {}", 
                    userId, studentId, assignment.getPlannableId(), e);
            return SyncResult.error("Status change handling failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle assignment status change for a specific calendar
     */
    private SyncResult handleStatusChangeForCalendar(String userId, Long studentId, AccountType accountType,
                                                   PlannerItem assignment, String statusChange, 
                                                   CalendarSyncSettings settings) {
        try {
            Optional<CalendarEventMapping> mappingOpt = eventMappingRepository
                    .findByAssignmentIdAndStudentIdAndAccountType(assignment.getPlannableId(), 
                            studentId, accountType);
            
            if (mappingOpt.isEmpty()) {
                log.debug("No event mapping found for assignment {} student {} account type {}", 
                        assignment.getPlannableId(), studentId, accountType);
                return SyncResult.success();
            }
            
            CalendarEventMapping mapping = mappingOpt.get();
            
            switch (statusChange.toLowerCase()) {
                case "completed":
                case "submitted":
                    if (settings.getSyncCompletedAssignments()) {
                        boolean marked = googleCalendarService.markEventAsCompleted(userId, studentId, 
                                accountType, mapping.getGoogleEventId(), assignment);
                        if (marked) {
                            mapping.setLastSyncedAt(LocalDateTime.now());
                            eventMappingRepository.save(mapping);
                            return SyncResult.updated();
                        }
                    } else {
                        boolean deleted = googleCalendarService.deleteAssignmentEvent(userId, studentId, 
                                accountType, mapping.getGoogleEventId());
                        if (deleted) {
                            eventMappingRepository.delete(mapping);
                            return SyncResult.deleted();
                        }
                    }
                    break;
                    
                case "deleted":
                case "removed":
                    boolean deleted = googleCalendarService.deleteAssignmentEvent(userId, studentId, 
                            accountType, mapping.getGoogleEventId());
                    if (deleted) {
                        eventMappingRepository.delete(mapping);
                        return SyncResult.deleted();
                    }
                    break;
                    
                case "updated":
                case "modified":
                    List<Integer> customReminders = getReminderSettings(settings, accountType);
                    boolean updated = googleCalendarService.updateAssignmentEvent(userId, studentId, 
                            accountType, mapping.getGoogleEventId(), assignment, customReminders);
                    if (updated) {
                        mapping.setLastSyncedAt(LocalDateTime.now());
                        eventMappingRepository.save(mapping);
                        return SyncResult.updated();
                    }
                    break;
                    
                default:
                    log.warn("Unknown status change: {}", statusChange);
                    return SyncResult.success();
            }
            
            return SyncResult.error("Failed to handle status change: " + statusChange);
            
        } catch (Exception e) {
            log.error("Failed to handle status change {} for assignment {} account type {}", 
                    statusChange, assignment.getPlannableId(), accountType, e);
            return SyncResult.error("Status change failed: " + e.getMessage());
        }
    }
    
    /**
     * Get assignments updated since a specific time
     */
    private List<PlannerItem> getAssignmentsUpdatedSince(Long studentId, LocalDateTime since) {
        // This would ideally use an updatedAt field, but for now we'll get all assignments
        // In a real implementation, you'd filter by updatedAt >= since
        return plannerItemRepository.findByStudentIdOrderByDueAtDesc(studentId);
    }
    
    /**
     * Clean up orphaned calendar events
     */
    private SyncResult cleanupOrphanedEvents(String userId, Long studentId, 
                                           List<CalendarEventMapping> orphanedMappings) {
        SyncResult result = new SyncResult();
        
        for (CalendarEventMapping mapping : orphanedMappings) {
            try {
                boolean deleted = googleCalendarService.deleteAssignmentEvent(userId, studentId, 
                        mapping.getAccountType(), mapping.getGoogleEventId());
                
                if (deleted) {
                    eventMappingRepository.delete(mapping);
                    result.incrementDeleted();
                } else {
                    result.addError("Failed to delete orphaned event " + mapping.getGoogleEventId());
                }
                
            } catch (Exception e) {
                log.error("Failed to cleanup orphaned event {}", mapping.getGoogleEventId(), e);
                result.addError("Failed to cleanup event: " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Process a batch of assignments
     */
    private SyncResult processBatch(String userId, Long studentId, List<PlannerItem> batch, 
                                  CalendarSyncSettings settings) {
        SyncResult result = new SyncResult();
        
        CalendarTokenService.CalendarConnectionStatus connectionStatus = 
                tokenService.getConnectionStatus(userId, studentId);
        
        for (PlannerItem assignment : batch) {
            if (!shouldSyncAssignment(assignment, settings)) {
                continue;
            }
            
            if (settings.getSyncToParentCalendar() && connectionStatus.isParentConnected()) {
                SyncResult parentResult = syncSingleAssignmentToCalendar(userId, studentId, 
                        AccountType.PARENT, assignment, settings);
                result.merge(parentResult);
            }
            
            if (settings.getSyncToStudentCalendar() && connectionStatus.isStudentConnected()) {
                SyncResult studentResult = syncSingleAssignmentToCalendar(userId, studentId, 
                        AccountType.STUDENT, assignment, settings);
                result.merge(studentResult);
            }
        }
        
        return result;
    }
    
    /**
     * Check if settings changes require a re-sync
     */
    private boolean settingsRequireResync(CalendarSyncSettings existing, CalendarSyncSettings newSettings) {
        if (!Objects.equals(existing.getSyncEnabled(), newSettings.getSyncEnabled())) {
            return true;
        }
        
        if (!Objects.equals(existing.getSyncToParentCalendar(), newSettings.getSyncToParentCalendar())) {
            return true;
        }
        
        if (!Objects.equals(existing.getSyncToStudentCalendar(), newSettings.getSyncToStudentCalendar())) {
            return true;
        }
        
        if (!Objects.equals(existing.getIncludedCourses(), newSettings.getIncludedCourses())) {
            return true;
        }
        
        if (!Objects.equals(existing.getExcludedAssignmentTypes(), newSettings.getExcludedAssignmentTypes())) {
            return true;
        }
        
        if (!Objects.equals(existing.getSyncCompletedAssignments(), newSettings.getSyncCompletedAssignments())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Parse JSON string list
     */
    private List<String> parseJsonStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON string list: {}", json, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Partition a list into smaller batches
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
    
    /**
     * Result class for sync operations
     */
    public static class SyncResult {
        private int createdCount = 0;
        private int updatedCount = 0;
        private int deletedCount = 0;
        private int errorCount = 0;
        private List<String> errors = new ArrayList<>();
        private String status = "success";
        
        public static SyncResult success() {
            return new SyncResult();
        }
        
        public static SyncResult created() {
            SyncResult result = new SyncResult();
            result.createdCount = 1;
            return result;
        }
        
        public static SyncResult updated() {
            SyncResult result = new SyncResult();
            result.updatedCount = 1;
            return result;
        }
        
        public static SyncResult deleted() {
            SyncResult result = new SyncResult();
            result.deletedCount = 1;
            return result;
        }
        
        public static SyncResult error(String message) {
            SyncResult result = new SyncResult();
            result.status = "error";
            result.errorCount = 1;
            result.errors.add(message);
            return result;
        }
        
        public static SyncResult disabled() {
            SyncResult result = new SyncResult();
            result.status = "disabled";
            return result;
        }
        
        public static SyncResult noCalendarsConnected() {
            SyncResult result = new SyncResult();
            result.status = "no_calendars_connected";
            return result;
        }
        
        public static SyncResult filtered() {
            SyncResult result = new SyncResult();
            result.status = "filtered";
            return result;
        }
        
        public void merge(SyncResult other) {
            this.createdCount += other.createdCount;
            this.updatedCount += other.updatedCount;
            this.deletedCount += other.deletedCount;
            this.errorCount += other.errorCount;
            this.errors.addAll(other.errors);
            
            if ("error".equals(other.status)) {
                this.status = "error";
            }
        }
        
        public void addError(String error) {
            this.errors.add(error);
            this.errorCount++;
            this.status = "error";
        }
        
        public void incrementCreated() {
            this.createdCount++;
        }
        
        public void incrementUpdated() {
            this.updatedCount++;
        }
        
        public void incrementDeleted() {
            this.deletedCount++;
        }
        
        // Getters
        public int getCreatedCount() { return createdCount; }
        public int getUpdatedCount() { return updatedCount; }
        public int getDeletedCount() { return deletedCount; }
        public int getErrorCount() { return errorCount; }
        public List<String> getErrors() { return errors; }
        public String getStatus() { return status; }
        
        public boolean isSuccess() {
            return "success".equals(status) && errorCount == 0;
        }
        
        public int getTotalProcessed() {
            return createdCount + updatedCount + deletedCount;
        }
        
        @Override
        public String toString() {
            return "SyncResult{" +
                    "status='" + status + '\'' +
                    ", created=" + createdCount +
                    ", updated=" + updatedCount +
                    ", deleted=" + deletedCount +
                    ", errors=" + errorCount +
                    '}';
        }
    }
}