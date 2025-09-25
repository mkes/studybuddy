package com.studytracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing parent notification preferences and settings
 */
@Entity
@Table(name = "parent_notification_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "student_id"}))
public class ParentNotificationSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @Column(name = "student_id", nullable = false)
    @NotNull(message = "Student ID cannot be null")
    private Long studentId;
    
    @Column(name = "parent_email", nullable = false)
    @NotBlank(message = "Parent email cannot be blank")
    @Email(message = "Parent email must be valid")
    private String parentEmail;
    
    @Column(name = "parent_name")
    private String parentName;
    
    @Column(name = "email_verified", nullable = false)
    @NotNull(message = "Email verified flag cannot be null")
    private Boolean emailVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "verification_expires_at")
    private LocalDateTime verificationExpiresAt;
    
    // Notification preferences
    @Column(name = "notify_assignment_due", nullable = false)
    @NotNull(message = "Assignment due notification flag cannot be null")
    private Boolean notifyAssignmentDue = true;
    
    @Column(name = "notify_assignment_missing", nullable = false)
    @NotNull(message = "Assignment missing notification flag cannot be null")
    private Boolean notifyAssignmentMissing = true;
    
    @Column(name = "notify_assignment_graded", nullable = false)
    @NotNull(message = "Assignment graded notification flag cannot be null")
    private Boolean notifyAssignmentGraded = true;
    
    @Column(name = "notify_calendar_sync", nullable = false)
    @NotNull(message = "Calendar sync notification flag cannot be null")
    private Boolean notifyCalendarSync = false;
    
    @Column(name = "daily_summary_enabled", nullable = false)
    @NotNull(message = "Daily summary enabled flag cannot be null")
    private Boolean dailySummaryEnabled = false;
    
    @Column(name = "daily_summary_time")
    private String dailySummaryTime = "08:00"; // Default 8 AM
    
    @Column(name = "weekly_summary_enabled", nullable = false)
    @NotNull(message = "Weekly summary enabled flag cannot be null")
    private Boolean weeklySummaryEnabled = false;
    
    @Column(name = "weekly_summary_day")
    private String weeklySummaryDay = "SUNDAY"; // Default Sunday
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public ParentNotificationSettings() {}
    
    // Constructor with required fields
    public ParentNotificationSettings(String userId, Long studentId, String parentEmail) {
        this.userId = userId;
        this.studentId = studentId;
        this.parentEmail = parentEmail;
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
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public String getVerificationToken() {
        return verificationToken;
    }
    
    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
    
    public LocalDateTime getVerificationExpiresAt() {
        return verificationExpiresAt;
    }
    
    public void setVerificationExpiresAt(LocalDateTime verificationExpiresAt) {
        this.verificationExpiresAt = verificationExpiresAt;
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
    
    /**
     * Check if email verification is expired
     */
    public boolean isVerificationExpired() {
        return verificationExpiresAt != null && verificationExpiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if email verification is pending
     */
    public boolean isVerificationPending() {
        return !emailVerified && verificationToken != null && !isVerificationExpired();
    }
    
    @Override
    public String toString() {
        return "ParentNotificationSettings{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", studentId=" + studentId +
                ", parentEmail='" + parentEmail + '\'' +
                ", parentName='" + parentName + '\'' +
                ", emailVerified=" + emailVerified +
                ", notifyAssignmentDue=" + notifyAssignmentDue +
                ", notifyAssignmentMissing=" + notifyAssignmentMissing +
                ", notifyAssignmentGraded=" + notifyAssignmentGraded +
                ", notifyCalendarSync=" + notifyCalendarSync +
                ", dailySummaryEnabled=" + dailySummaryEnabled +
                ", weeklySummaryEnabled=" + weeklySummaryEnabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}