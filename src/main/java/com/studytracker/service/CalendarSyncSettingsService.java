package com.studytracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.dto.SyncSettingsDto;
import com.studytracker.model.CalendarSyncSettings;
import com.studytracker.model.PlannerItem;
import com.studytracker.repository.CalendarSyncSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing calendar sync settings and preferences
 */
@Service
@Transactional
public class CalendarSyncSettingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CalendarSyncSettingsService.class);
    
    private final CalendarSyncSettingsRepository syncSettingsRepository;
    private final ObjectMapper objectMapper;
    
    // Default reminder settings
    private static final List<Integer> DEFAULT_PARENT_REMINDERS = Arrays.asList(1440, 120); // 24h, 2h
    private static final List<Integer> DEFAULT_STUDENT_REMINDERS = Arrays.asList(120, 30);  // 2h, 30min
    
    @Autowired
    public CalendarSyncSettingsService(CalendarSyncSettingsRepository syncSettingsRepository,
                                     ObjectMapper objectMapper) {
        this.syncSettingsRepository = syncSettingsRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get sync settings for a user and student, creating default settings if none exist
     */
    public CalendarSyncSettings getSyncSettings(String userId, Long studentId) {
        logger.debug("Getting sync settings for user {} and student {}", userId, studentId);
        
        Optional<CalendarSyncSettings> existingSettings = syncSettingsRepository.findByUserIdAndStudentId(userId, studentId);
        
        if (existingSettings.isPresent()) {
            return existingSettings.get();
        }
        
        // Create default settings if none exist
        return createDefaultSettings(userId, studentId);
    }
    
    /**
     * Get sync settings as DTO
     */
    public SyncSettingsDto getSyncSettingsDto(String userId, Long studentId) {
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        return convertToDto(settings);
    }
    
    /**
     * Update sync settings from DTO
     */
    public CalendarSyncSettings updateSyncSettings(String userId, Long studentId, SyncSettingsDto settingsDto) {
        logger.info("Updating sync settings for user {} and student {}", userId, studentId);
        
        // Validate settings
        validateSyncSettings(settingsDto);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        
        // Update settings from DTO
        settings.setSyncEnabled(settingsDto.getSyncEnabled());
        settings.setSyncToParentCalendar(settingsDto.getSyncToParentCalendar());
        settings.setSyncToStudentCalendar(settingsDto.getSyncToStudentCalendar());
        settings.setSyncCompletedAssignments(settingsDto.getSyncCompletedAssignments());
        settings.setAutoSyncEnabled(settingsDto.getAutoSyncEnabled());
        
        // Update reminder settings
        if (settingsDto.getParentReminderMinutes() != null) {
            settings.setParentReminderMinutes(convertListToJson(settingsDto.getParentReminderMinutes()));
        }
        if (settingsDto.getStudentReminderMinutes() != null) {
            settings.setStudentReminderMinutes(convertListToJson(settingsDto.getStudentReminderMinutes()));
        }
        
        // Update course and assignment type filters
        if (settingsDto.getIncludedCourses() != null) {
            settings.setIncludedCourses(convertListToJson(settingsDto.getIncludedCourses()));
        }
        if (settingsDto.getExcludedAssignmentTypes() != null) {
            settings.setExcludedAssignmentTypes(convertListToJson(settingsDto.getExcludedAssignmentTypes()));
        }
        
        return syncSettingsRepository.save(settings);
    }
    
    /**
     * Enable or disable sync for a user and student
     */
    public void setSyncEnabled(String userId, Long studentId, boolean enabled) {
        logger.info("Setting sync enabled to {} for user {} and student {}", enabled, userId, studentId);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        settings.setSyncEnabled(enabled);
        syncSettingsRepository.save(settings);
    }
    
    /**
     * Enable or disable auto sync for a user and student
     */
    public void setAutoSyncEnabled(String userId, Long studentId, boolean enabled) {
        logger.info("Setting auto sync enabled to {} for user {} and student {}", enabled, userId, studentId);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        settings.setAutoSyncEnabled(enabled);
        syncSettingsRepository.save(settings);
    }
    
    /**
     * Update reminder settings for parent calendar
     */
    public void updateParentReminderSettings(String userId, Long studentId, List<Integer> reminderMinutes) {
        logger.info("Updating parent reminder settings for user {} and student {}", userId, studentId);
        
        validateReminderMinutes(reminderMinutes);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        settings.setParentReminderMinutes(convertListToJson(reminderMinutes));
        syncSettingsRepository.save(settings);
    }
    
    /**
     * Update reminder settings for student calendar
     */
    public void updateStudentReminderSettings(String userId, Long studentId, List<Integer> reminderMinutes) {
        logger.info("Updating student reminder settings for user {} and student {}", userId, studentId);
        
        validateReminderMinutes(reminderMinutes);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        settings.setStudentReminderMinutes(convertListToJson(reminderMinutes));
        syncSettingsRepository.save(settings);
    }
    
    /**
     * Update course filtering settings
     */
    public void updateCourseFiltering(String userId, Long studentId, List<String> includedCourses) {
        logger.info("Updating course filtering for user {} and student {}", userId, studentId);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        settings.setIncludedCourses(convertListToJson(includedCourses));
        syncSettingsRepository.save(settings);
    }
    
    /**
     * Update assignment type exclusion settings
     */
    public void updateAssignmentTypeExclusion(String userId, Long studentId, List<String> excludedTypes) {
        logger.info("Updating assignment type exclusion for user {} and student {}", userId, studentId);
        
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        settings.setExcludedAssignmentTypes(convertListToJson(excludedTypes));
        syncSettingsRepository.save(settings);
    }
    
    /**
     * Check if an assignment should be synced based on settings
     */
    public boolean shouldSyncAssignment(String userId, Long studentId, PlannerItem assignment) {
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        
        // Check if sync is enabled
        if (!settings.getSyncEnabled()) {
            return false;
        }
        
        // Check if completed assignments should be synced
        if (assignment.getSubmitted() && !settings.getSyncCompletedAssignments()) {
            return false;
        }
        
        // Check course filtering
        if (!shouldIncludeCourse(settings, assignment.getContextName())) {
            return false;
        }
        
        // Check assignment type exclusion - for now we'll use a simple heuristic based on title
        String assignmentType = determineAssignmentType(assignment.getAssignmentTitle());
        if (shouldExcludeAssignmentType(settings, assignmentType)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get reminder minutes for parent calendar
     */
    public List<Integer> getParentReminderMinutes(String userId, Long studentId) {
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        return convertJsonToIntegerList(settings.getParentReminderMinutes());
    }
    
    /**
     * Get reminder minutes for student calendar
     */
    public List<Integer> getStudentReminderMinutes(String userId, Long studentId) {
        CalendarSyncSettings settings = getSyncSettings(userId, studentId);
        return convertJsonToIntegerList(settings.getStudentReminderMinutes());
    }
    
    /**
     * Delete sync settings for a user and student
     */
    public void deleteSyncSettings(String userId, Long studentId) {
        logger.info("Deleting sync settings for user {} and student {}", userId, studentId);
        syncSettingsRepository.deleteByUserIdAndStudentId(userId, studentId);
    }
    
    /**
     * Get all users with auto sync enabled
     */
    public List<CalendarSyncSettings> getUsersWithAutoSyncEnabled() {
        return syncSettingsRepository.findByAutoSyncEnabledTrue();
    }
    
    // Private helper methods
    
    private CalendarSyncSettings createDefaultSettings(String userId, Long studentId) {
        logger.debug("Creating default sync settings for user {} and student {}", userId, studentId);
        
        CalendarSyncSettings settings = new CalendarSyncSettings(userId, studentId);
        settings.setSyncEnabled(true);
        settings.setSyncToParentCalendar(true);
        settings.setSyncToStudentCalendar(true);
        settings.setParentReminderMinutes(convertListToJson(DEFAULT_PARENT_REMINDERS));
        settings.setStudentReminderMinutes(convertListToJson(DEFAULT_STUDENT_REMINDERS));
        settings.setSyncCompletedAssignments(false);
        settings.setAutoSyncEnabled(true);
        
        return syncSettingsRepository.save(settings);
    }
    
    private SyncSettingsDto convertToDto(CalendarSyncSettings settings) {
        SyncSettingsDto dto = new SyncSettingsDto();
        dto.setSyncEnabled(settings.getSyncEnabled());
        dto.setSyncToParentCalendar(settings.getSyncToParentCalendar());
        dto.setSyncToStudentCalendar(settings.getSyncToStudentCalendar());
        dto.setSyncCompletedAssignments(settings.getSyncCompletedAssignments());
        dto.setAutoSyncEnabled(settings.getAutoSyncEnabled());
        
        dto.setParentReminderMinutes(convertJsonToIntegerList(settings.getParentReminderMinutes()));
        dto.setStudentReminderMinutes(convertJsonToIntegerList(settings.getStudentReminderMinutes()));
        dto.setIncludedCourses(convertJsonToStringList(settings.getIncludedCourses()));
        dto.setExcludedAssignmentTypes(convertJsonToStringList(settings.getExcludedAssignmentTypes()));
        
        return dto;
    }
    
    private void validateSyncSettings(SyncSettingsDto settingsDto) {
        if (settingsDto.getSyncEnabled() == null) {
            throw new IllegalArgumentException("Sync enabled flag cannot be null");
        }
        
        if (settingsDto.getSyncToParentCalendar() == null) {
            throw new IllegalArgumentException("Sync to parent calendar flag cannot be null");
        }
        
        if (settingsDto.getSyncToStudentCalendar() == null) {
            throw new IllegalArgumentException("Sync to student calendar flag cannot be null");
        }
        
        if (settingsDto.getSyncCompletedAssignments() == null) {
            throw new IllegalArgumentException("Sync completed assignments flag cannot be null");
        }
        
        if (settingsDto.getAutoSyncEnabled() == null) {
            throw new IllegalArgumentException("Auto sync enabled flag cannot be null");
        }
        
        // Validate reminder minutes
        if (settingsDto.getParentReminderMinutes() != null) {
            validateReminderMinutes(settingsDto.getParentReminderMinutes());
        }
        
        if (settingsDto.getStudentReminderMinutes() != null) {
            validateReminderMinutes(settingsDto.getStudentReminderMinutes());
        }
    }
    
    private void validateReminderMinutes(List<Integer> reminderMinutes) {
        if (reminderMinutes == null || reminderMinutes.isEmpty()) {
            throw new IllegalArgumentException("Reminder minutes cannot be null or empty");
        }
        
        for (Integer minutes : reminderMinutes) {
            if (minutes == null || minutes < 0) {
                throw new IllegalArgumentException("Reminder minutes must be non-negative");
            }
            if (minutes > 40320) { // 4 weeks in minutes
                throw new IllegalArgumentException("Reminder minutes cannot exceed 4 weeks (40320 minutes)");
            }
        }
    }
    
    private boolean shouldIncludeCourse(CalendarSyncSettings settings, String courseName) {
        if (settings.getIncludedCourses() == null || settings.getIncludedCourses().trim().isEmpty()) {
            return true; // Include all courses if no filter is set
        }
        
        List<String> includedCourses = convertJsonToStringList(settings.getIncludedCourses());
        return includedCourses.isEmpty() || includedCourses.contains(courseName);
    }
    
    private boolean shouldExcludeAssignmentType(CalendarSyncSettings settings, String assignmentType) {
        if (settings.getExcludedAssignmentTypes() == null || settings.getExcludedAssignmentTypes().trim().isEmpty()) {
            return false; // Don't exclude any types if no filter is set
        }
        
        List<String> excludedTypes = convertJsonToStringList(settings.getExcludedAssignmentTypes());
        return excludedTypes.contains(assignmentType);
    }
    
    private String determineAssignmentType(String assignmentTitle) {
        if (assignmentTitle == null) {
            return "assignment";
        }
        
        String lowerTitle = assignmentTitle.toLowerCase();
        
        if (lowerTitle.contains("quiz")) {
            return "quiz";
        } else if (lowerTitle.contains("exam") || lowerTitle.contains("test")) {
            return "exam";
        } else if (lowerTitle.contains("discussion")) {
            return "discussion";
        } else if (lowerTitle.contains("homework") || lowerTitle.contains("hw")) {
            return "homework";
        } else if (lowerTitle.contains("project")) {
            return "project";
        } else if (lowerTitle.contains("lab")) {
            return "lab";
        } else {
            return "assignment";
        }
    }
    
    private String convertListToJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("Error converting list to JSON: {}", e.getMessage());
            return "[]";
        }
    }
    
    private List<Integer> convertJsonToIntegerList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Arrays.asList();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to integer list: {}", e.getMessage());
            return Arrays.asList();
        }
    }
    
    private List<String> convertJsonToStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Arrays.asList();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to string list: {}", e.getMessage());
            return Arrays.asList();
        }
    }
}