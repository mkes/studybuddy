package com.studytracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_tokens", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "student_id", "account_type"}))
public class CalendarToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    private String userId; // Canvas user ID or session ID
    
    @Column(name = "student_id", nullable = false)
    @NotNull(message = "Student ID cannot be null")
    private Long studentId;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account type cannot be null")
    private AccountType accountType; // PARENT or STUDENT
    
    @Column(name = "google_email")
    private String googleEmail; // Google account email
    
    @Column(name = "encrypted_access_token", nullable = false)
    @NotBlank(message = "Access token cannot be blank")
    private String encryptedAccessToken;
    
    @Column(name = "encrypted_refresh_token", nullable = false)
    @NotBlank(message = "Refresh token cannot be blank")
    private String encryptedRefreshToken;
    
    @Column(name = "token_expires_at", nullable = false)
    @NotNull(message = "Token expiration time cannot be null")
    private LocalDateTime tokenExpiresAt;
    
    @Column(name = "calendar_id")
    private String calendarId; // Google Calendar ID for assignments
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public CalendarToken() {}
    
    // Constructor with required fields
    public CalendarToken(String userId, Long studentId, AccountType accountType, 
                        String encryptedAccessToken, String encryptedRefreshToken, 
                        LocalDateTime tokenExpiresAt) {
        this.userId = userId;
        this.studentId = studentId;
        this.accountType = accountType;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
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
    
    public AccountType getAccountType() {
        return accountType;
    }
    
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
    
    public String getGoogleEmail() {
        return googleEmail;
    }
    
    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }
    
    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }
    
    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }
    
    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }
    
    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }
    
    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }
    
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }
    
    public String getCalendarId() {
        return calendarId;
    }
    
    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
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
    
    // Helper method to check if token is expired
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }
    
    @Override
    public String toString() {
        return "CalendarToken{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", studentId=" + studentId +
                ", accountType=" + accountType +
                ", googleEmail='" + googleEmail + '\'' +
                ", tokenExpiresAt=" + tokenExpiresAt +
                ", calendarId='" + calendarId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}