package com.studytracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_sync_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "student_id"}))
public class CalendarSyncSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @Column(name = "student_id", nullable = false)
    @NotNull(message = "Student ID cannot be null")
    private Long studentId;
    
    @Column(name = "sync_enabled", nullable = false)
    @NotNull(message = "Sync enabled flag cannot be null")
    private Boolean syncEnabled = true;
    
    @Column(name = "sync_to_parent_calendar", nullable = false)
    @NotNull(message = "Sync to parent calendar flag cannot be null")
    private Boolean syncToParentCalendar = true;
    
    @Column(name = "sync_to_student_calendar", nullable = false)
    @NotNull(message = "Sync to student calendar flag cannot be null")
    private Boolean syncToStudentCalendar = true;
    
    @Column(name = "parent_reminder_minutes")
    private String parentReminderMinutes = "[1440,120]"; // 24h, 2h default
    
    @Column(name = "student_reminder_minutes")
    private String studentReminderMinutes = "[120,30]"; // 2h, 30min default
    
    @Column(name = "included_courses", columnDefinition = "TEXT")
    private String includedCourses; // JSON array of course names
    
    @Column(name = "excluded_assignment_types", columnDefinition = "TEXT")
    private String excludedAssignmentTypes; // JSON array
    
    @Column(name = "sync_completed_assignments", nullable = false)
    @NotNull(message = "Sync completed assignments flag cannot be null")
    private Boolean syncCompletedAssignments = false;
    
    @Column(name = "auto_sync_enabled", nullable = false)
    @NotNull(message = "Auto sync enabled flag cannot be null")
    private Boolean autoSyncEnabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public CalendarSyncSettings() {}
    
    // Constructor with required fields
    public CalendarSyncSettings(String userId, Long studentId) {
        this.userId = userId;
        this.studentId = studentId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Long getStudentId() {
        return studentId;
    }
    
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    
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
    
    public String getParentReminderMinutes() {
        return parentReminderMinutes;
    }
    
    public void setParentReminderMinutes(String parentReminderMinutes) {
        this.parentReminderMinutes = parentReminderMinutes;
    }
    
    public String getStudentReminderMinutes() {
        return studentReminderMinutes;
    }
    
    public void setStudentReminderMinutes(String studentReminderMinutes) {
        this.studentReminderMinutes = studentReminderMinutes;
    }
    
    public String getIncludedCourses() {
        return includedCourses;
    }
    
    public void setIncludedCourses(String includedCourses) {
        this.includedCourses = includedCourses;
    }
    
    public String getExcludedAssignmentTypes() {
        return excludedAssignmentTypes;
    }
    
    public void setExcludedAssignmentTypes(String excludedAssignmentTypes) {
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "CalendarSyncSettings{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", studentId=" + studentId +
                ", syncEnabled=" + syncEnabled +
                ", syncToParentCalendar=" + syncToParentCalendar +
                ", syncToStudentCalendar=" + syncToStudentCalendar +
                ", parentReminderMinutes='" + parentReminderMinutes + '\'' +
                ", studentReminderMinutes='" + studentReminderMinutes + '\'' +
                ", syncCompletedAssignments=" + syncCompletedAssignments +
                ", autoSyncEnabled=" + autoSyncEnabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}