package com.studytracker.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO for calendar sync settings
 */
public class SyncSettingsDto {
    
    @NotNull(message = "Sync enabled flag cannot be null")
    private Boolean syncEnabled;
    
    @NotNull(message = "Sync to parent calendar flag cannot be null")
    private Boolean syncToParentCalendar;
    
    @NotNull(message = "Sync to student calendar flag cannot be null")
    private Boolean syncToStudentCalendar;
    
    private List<Integer> parentReminderMinutes;
    private List<Integer> studentReminderMinutes;
    private List<String> includedCourses;
    private List<String> excludedAssignmentTypes;
    
    @NotNull(message = "Sync completed assignments flag cannot be null")
    private Boolean syncCompletedAssignments;
    
    @NotNull(message = "Auto sync enabled flag cannot be null")
    private Boolean autoSyncEnabled;
    
    // Default constructor
    public SyncSettingsDto() {}
    
    // Constructor with all fields
    public SyncSettingsDto(Boolean syncEnabled, Boolean syncToParentCalendar, Boolean syncToStudentCalendar,
                          List<Integer> parentReminderMinutes, List<Integer> studentReminderMinutes,
                          List<String> includedCourses, List<String> excludedAssignmentTypes,
                          Boolean syncCompletedAssignments, Boolean autoSyncEnabled) {
        this.syncEnabled = syncEnabled;
        this.syncToParentCalendar = syncToParentCalendar;
        this.syncToStudentCalendar = syncToStudentCalendar;
        this.parentReminderMinutes = parentReminderMinutes;
        this.studentReminderMinutes = studentReminderMinutes;
        this.includedCourses = includedCourses;
        this.excludedAssignmentTypes = excludedAssignmentTypes;
        this.syncCompletedAssignments = syncCompletedAssignments;
        this.autoSyncEnabled = autoSyncEnabled;
    }
    
    // Getters and Setters
    public Boolean getSyncEnabled() {
        return syncEnabled;
    }
    
    public void setSyncEnabled(Boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
    
    public Boolean getSyncToParentCalendar() {
        return syncToParentCalendar;
    }
    
    public void setSyncToParentCalendar(Boolean syncToParentCalendar) {
        this.syncToParentCalendar = syncToParentCalendar;
    }
    
    public Boolean getSyncToStudentCalendar() {
        return syncToStudentCalendar;
    }
    
    public void setSyncToStudentCalendar(Boolean syncToStudentCalendar) {
        this.syncToStudentCalendar = syncToStudentCalendar;
    }
    
    public List<Integer> getParentReminderMinutes() {
        return parentReminderMinutes;
    }
    
    public void setParentReminderMinutes(List<Integer> parentReminderMinutes) {
        this.parentReminderMinutes = parentReminderMinutes;
    }
    
    public List<Integer> getStudentReminderMinutes() {
        return studentReminderMinutes;
    }
    
    public void setStudentReminderMinutes(List<Integer> studentReminderMinutes) {
        this.studentReminderMinutes = studentReminderMinutes;
    }
    
    public List<String> getIncludedCourses() {
        return includedCourses;
    }
    
    public void setIncludedCourses(List<String> includedCourses) {
        this.includedCourses = includedCourses;
    }
    
    public List<String> getExcludedAssignmentTypes() {
        return excludedAssignmentTypes;
    }
    
    public void setExcludedAssignmentTypes(List<String> excludedAssignmentTypes) {
        this.excludedAssignmentTypes = excludedAssignmentTypes;
    }
    
    public Boolean getSyncCompletedAssignments() {
        return syncCompletedAssignments;
    }
    
    public void setSyncCompletedAssignments(Boolean syncCompletedAssignments) {
        this.syncCompletedAssignments = syncCompletedAssignments;
    }
    
    public Boolean getAutoSyncEnabled() {
        return autoSyncEnabled;
    }
    
    public void setAutoSyncEnabled(Boolean autoSyncEnabled) {
        this.autoSyncEnabled = autoSyncEnabled;
    }
    
    @Override
    public String toString() {
        return "SyncSettingsDto{" +
                "syncEnabled=" + syncEnabled +
                ", syncToParentCalendar=" + syncToParentCalendar +
                ", syncToStudentCalendar=" + syncToStudentCalendar +
                ", parentReminderMinutes=" + parentReminderMinutes +
                ", studentReminderMinutes=" + studentReminderMinutes +
                ", includedCourses=" + includedCourses +
                ", excludedAssignmentTypes=" + excludedAssignmentTypes +
                ", syncCompletedAssignments=" + syncCompletedAssignments +
                ", autoSyncEnabled=" + autoSyncEnabled +
                '}';
    }
}