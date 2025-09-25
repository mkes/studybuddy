package com.studytracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for parent notification settings
 */
public class ParentNotificationSettingsDto {
    
    @NotBlank(message = "Parent email cannot be blank")
    @Email(message = "Parent email must be valid")
    private String parentEmail;
    
    private String parentName;
    
    @NotNull(message = "Assignment due notification preference cannot be null")
    private Boolean notifyAssignmentDue = true;
    
    @NotNull(message = "Assignment missing notification preference cannot be null")
    private Boolean notifyAssignmentMissing = true;
    
    @NotNull(message = "Assignment graded notification preference cannot be null")
    private Boolean notifyAssignmentGraded = true;
    
    @NotNull(message = "Calendar sync notification preference cannot be null")
    private Boolean notifyCalendarSync = false;
    
    @NotNull(message = "Daily summary preference cannot be null")
    private Boolean dailySummaryEnabled = false;
    
    private String dailySummaryTime = "08:00";
    
    @NotNull(message = "Weekly summary preference cannot be null")
    private Boolean weeklySummaryEnabled = false;
    
    private String weeklySummaryDay = "SUNDAY";
    
    // Default constructor
    public ParentNotificationSettingsDto() {}
    
    // Constructor with email
    public ParentNotificationSettingsDto(String parentEmail) {
        this.parentEmail = parentEmail;
    }
    
    // Constructor with all notification preferences
    public ParentNotificationSettingsDto(String parentEmail, String parentName,
                                       Boolean notifyAssignmentDue, Boolean notifyAssignmentMissing,
                                       Boolean notifyAssignmentGraded, Boolean notifyCalendarSync,
                                       Boolean dailySummaryEnabled, String dailySummaryTime,
                                       Boolean weeklySummaryEnabled, String weeklySummaryDay) {
        this.parentEmail = parentEmail;
        this.parentName = parentName;
        this.notifyAssignmentDue = notifyAssignmentDue;
        this.notifyAssignmentMissing = notifyAssignmentMissing;
        this.notifyAssignmentGraded = notifyAssignmentGraded;
        this.notifyCalendarSync = notifyCalendarSync;
        this.dailySummaryEnabled = dailySummaryEnabled;
        this.dailySummaryTime = dailySummaryTime;
        this.weeklySummaryEnabled = weeklySummaryEnabled;
        this.weeklySummaryDay = weeklySummaryDay;
    }
    
    // Getters and Setters
    public String getParentEmail() {
        return parentEmail;
    }
    
    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }
    
    public String getParentName() {
        return parentName;
    }
    
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
    
    public Boolean getNotifyAssignmentDue() {
        return notifyAssignmentDue;
    }
    
    public void setNotifyAssignmentDue(Boolean notifyAssignmentDue) {
        this.notifyAssignmentDue = notifyAssignmentDue;
    }
    
    public Boolean getNotifyAssignmentMissing() {
        return notifyAssignmentMissing;
    }
    
    public void setNotifyAssignmentMissing(Boolean notifyAssignmentMissing) {
        this.notifyAssignmentMissing = notifyAssignmentMissing;
    }
    
    public Boolean getNotifyAssignmentGraded() {
        return notifyAssignmentGraded;
    }
    
    public void setNotifyAssignmentGraded(Boolean notifyAssignmentGraded) {
        this.notifyAssignmentGraded = notifyAssignmentGraded;
    }
    
    public Boolean getNotifyCalendarSync() {
        return notifyCalendarSync;
    }
    
    public void setNotifyCalendarSync(Boolean notifyCalendarSync) {
        this.notifyCalendarSync = notifyCalendarSync;
    }
    
    public Boolean getDailySummaryEnabled() {
        return dailySummaryEnabled;
    }
    
    public void setDailySummaryEnabled(Boolean dailySummaryEnabled) {
        this.dailySummaryEnabled = dailySummaryEnabled;
    }
    
    public String getDailySummaryTime() {
        return dailySummaryTime;
    }
    
    public void setDailySummaryTime(String dailySummaryTime) {
        this.dailySummaryTime = dailySummaryTime;
    }
    
    public Boolean getWeeklySummaryEnabled() {
        return weeklySummaryEnabled;
    }
    
    public void setWeeklySummaryEnabled(Boolean weeklySummaryEnabled) {
        this.weeklySummaryEnabled = weeklySummaryEnabled;
    }
    
    public String getWeeklySummaryDay() {
        return weeklySummaryDay;
    }
    
    public void setWeeklySummaryDay(String weeklySummaryDay) {
        this.weeklySummaryDay = weeklySummaryDay;
    }
    
    @Override
    public String toString() {
        return "ParentNotificationSettingsDto{" +
                "parentEmail='" + parentEmail + '\'' +
                ", parentName='" + parentName + '\'' +
                ", notifyAssignmentDue=" + notifyAssignmentDue +
                ", notifyAssignmentMissing=" + notifyAssignmentMissing +
                ", notifyAssignmentGraded=" + notifyAssignmentGraded +
                ", notifyCalendarSync=" + notifyCalendarSync +
                ", dailySummaryEnabled=" + dailySummaryEnabled +
                ", dailySummaryTime='" + dailySummaryTime + '\'' +
                ", weeklySummaryEnabled=" + weeklySummaryEnabled +
                ", weeklySummaryDay='" + weeklySummaryDay + '\'' +
                '}';
    }
}