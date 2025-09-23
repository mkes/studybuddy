package com.studytracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_event_mappings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id", "account_type"}))
public class CalendarEventMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "assignment_id", nullable = false)
    @NotNull(message = "Assignment ID cannot be null")
    private Long assignmentId; // PlannerItem.plannableId
    
    @Column(name = "student_id", nullable = false)
    @NotNull(message = "Student ID cannot be null")
    private Long studentId;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account type cannot be null")
    private AccountType accountType; // PARENT or STUDENT
    
    @Column(name = "google_event_id", nullable = false)
    @NotBlank(message = "Google event ID cannot be blank")
    private String googleEventId;
    
    @Column(name = "google_calendar_id", nullable = false)
    @NotBlank(message = "Google calendar ID cannot be blank")
    private String googleCalendarId;
    
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public CalendarEventMapping() {}
    
    // Constructor with required fields
    public CalendarEventMapping(Long assignmentId, Long studentId, AccountType accountType,
                               String googleEventId, String googleCalendarId) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.accountType = accountType;
        this.googleEventId = googleEventId;
        this.googleCalendarId = googleCalendarId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAssignmentId() {
        return assignmentId;
    }
    
    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }
    
    public Long getStudentId() {
        return studentId;
    }
    
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    
    public AccountType getAccountType() {
        return accountType;
    }
    
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
    
    public String getGoogleEventId() {
        return googleEventId;
    }
    
    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
    
    public String getGoogleCalendarId() {
        return googleCalendarId;
    }
    
    public void setGoogleCalendarId(String googleCalendarId) {
        this.googleCalendarId = googleCalendarId;
    }
    
    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }
    
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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
    
    // Helper method to update sync timestamp
    public void markAsSynced() {
        this.lastSyncedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "CalendarEventMapping{" +
                "id=" + id +
                ", assignmentId=" + assignmentId +
                ", studentId=" + studentId +
                ", accountType=" + accountType +
                ", googleEventId='" + googleEventId + '\'' +
                ", googleCalendarId='" + googleCalendarId + '\'' +
                ", lastSyncedAt=" + lastSyncedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}