package com.studytracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_calendar_invitations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "student_id"}))
public class StudentCalendarInvitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID cannot be blank")
    private String userId; // Parent's user ID
    
    @Column(name = "student_id", nullable = false)
    @NotNull(message = "Student ID cannot be null")
    private Long studentId;
    
    @Column(name = "student_email", nullable = false)
    @NotBlank(message = "Student email cannot be blank")
    @Email(message = "Student email must be valid")
    private String studentEmail;
    
    @Column(name = "invitation_token", nullable = false, unique = true)
    @NotBlank(message = "Invitation token cannot be blank")
    private String invitationToken; // Secure token for invitation link
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Invitation status cannot be null")
    private InvitationStatus status = InvitationStatus.PENDING;
    
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time cannot be null")
    private LocalDateTime expiresAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public StudentCalendarInvitation() {}
    
    // Constructor with required fields
    public StudentCalendarInvitation(String userId, Long studentId, String studentEmail,
                                   String invitationToken, LocalDateTime expiresAt) {
        this.userId = userId;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.invitationToken = invitationToken;
        this.expiresAt = expiresAt;
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
    
    public String getStudentEmail() {
        return studentEmail;
    }
    
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
    
    public String getInvitationToken() {
        return invitationToken;
    }
    
    public void setInvitationToken(String invitationToken) {
        this.invitationToken = invitationToken;
    }
    
    public InvitationStatus getStatus() {
        return status;
    }
    
    public void setStatus(InvitationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }
    
    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
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
    
    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean isPending() {
        return status == InvitationStatus.PENDING && !isExpired();
    }
    
    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }
    
    public void expire() {
        this.status = InvitationStatus.EXPIRED;
    }
    
    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }
    
    @Override
    public String toString() {
        return "StudentCalendarInvitation{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", studentId=" + studentId +
                ", studentEmail='" + studentEmail + '\'' +
                ", invitationToken='" + invitationToken + '\'' +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                ", acceptedAt=" + acceptedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}