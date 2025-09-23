package com.studytracker.repository;

import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCalendarInvitationRepository extends JpaRepository<StudentCalendarInvitation, Long> {
    
    /**
     * Find invitation by user ID and student ID
     */
    Optional<StudentCalendarInvitation> findByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Find invitation by invitation token
     */
    Optional<StudentCalendarInvitation> findByInvitationToken(String invitationToken);
    
    /**
     * Find all invitations for a specific user
     */
    List<StudentCalendarInvitation> findByUserId(String userId);
    
    /**
     * Find all invitations for a specific student
     */
    List<StudentCalendarInvitation> findByStudentId(Long studentId);
    
    /**
     * Find invitations by student email
     */
    List<StudentCalendarInvitation> findByStudentEmail(String studentEmail);
    
    /**
     * Find invitations by status
     */
    List<StudentCalendarInvitation> findByStatus(InvitationStatus status);
    
    /**
     * Find pending invitations
     */
    List<StudentCalendarInvitation> findByStatusAndExpiresAtAfter(
            InvitationStatus status, LocalDateTime currentTime);
    
    /**
     * Find expired invitations that haven't been marked as expired
     */
    @Query("SELECT sci FROM StudentCalendarInvitation sci WHERE sci.status = :status AND sci.expiresAt < :currentTime")
    List<StudentCalendarInvitation> findExpiredInvitations(
            @Param("status") InvitationStatus status, 
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find invitations expiring within a specific time window
     */
    @Query("SELECT sci FROM StudentCalendarInvitation sci WHERE sci.status = :status AND sci.expiresAt BETWEEN :now AND :expiryThreshold")
    List<StudentCalendarInvitation> findInvitationsExpiringWithin(
            @Param("status") InvitationStatus status,
            @Param("now") LocalDateTime now, 
            @Param("expiryThreshold") LocalDateTime expiryThreshold);
    
    /**
     * Find accepted invitations
     */
    List<StudentCalendarInvitation> findByStatusAndAcceptedAtIsNotNull(InvitationStatus status);
    
    /**
     * Check if invitation exists for user and student
     */
    boolean existsByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Check if invitation exists by token
     */
    boolean existsByInvitationToken(String invitationToken);
    
    /**
     * Check if active invitation exists for user and student
     */
    @Query("SELECT COUNT(sci) > 0 FROM StudentCalendarInvitation sci WHERE sci.userId = :userId AND sci.studentId = :studentId AND sci.status = :status AND sci.expiresAt > :currentTime")
    boolean existsActiveInvitation(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("status") InvitationStatus status, 
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Delete invitation by user ID and student ID
     */
    @Modifying
    @Query("DELETE FROM StudentCalendarInvitation sci WHERE sci.userId = :userId AND sci.studentId = :studentId")
    void deleteByUserIdAndStudentId(@Param("userId") String userId, @Param("studentId") Long studentId);
    
    /**
     * Delete invitation by token
     */
    @Modifying
    @Query("DELETE FROM StudentCalendarInvitation sci WHERE sci.invitationToken = :token")
    void deleteByInvitationToken(@Param("token") String token);
    
    /**
     * Delete expired invitations
     */
    @Modifying
    @Query("DELETE FROM StudentCalendarInvitation sci WHERE sci.expiresAt < :currentTime")
    int deleteExpiredInvitations(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Update invitation status
     */
    @Modifying
    @Query("UPDATE StudentCalendarInvitation sci SET sci.status = :status WHERE sci.invitationToken = :token")
    int updateInvitationStatus(@Param("token") String token, @Param("status") InvitationStatus status);
    
    /**
     * Update invitation status and accepted time
     */
    @Modifying
    @Query("UPDATE StudentCalendarInvitation sci SET sci.status = :status, sci.acceptedAt = :acceptedAt WHERE sci.invitationToken = :token")
    int updateInvitationStatusAndAcceptedAt(
            @Param("token") String token, 
            @Param("status") InvitationStatus status, 
            @Param("acceptedAt") LocalDateTime acceptedAt);
    
    /**
     * Mark expired invitations as expired
     */
    @Modifying
    @Query("UPDATE StudentCalendarInvitation sci SET sci.status = :expiredStatus WHERE sci.status = :pendingStatus AND sci.expiresAt < :currentTime")
    int markExpiredInvitations(
            @Param("pendingStatus") InvitationStatus pendingStatus,
            @Param("expiredStatus") InvitationStatus expiredStatus,
            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Count invitations by status
     */
    @Query("SELECT COUNT(sci) FROM StudentCalendarInvitation sci WHERE sci.status = :status")
    long countByStatus(@Param("status") InvitationStatus status);
    
    /**
     * Count active invitations for a user
     */
    @Query("SELECT COUNT(sci) FROM StudentCalendarInvitation sci WHERE sci.userId = :userId AND sci.status = :status AND sci.expiresAt > :currentTime")
    long countActiveInvitationsByUser(
            @Param("userId") String userId, 
            @Param("status") InvitationStatus status, 
            @Param("currentTime") LocalDateTime currentTime);
}