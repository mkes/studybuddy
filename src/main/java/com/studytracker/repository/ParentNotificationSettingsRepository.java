package com.studytracker.repository;

import com.studytracker.model.ParentNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParentNotificationSettingsRepository extends JpaRepository<ParentNotificationSettings, Long> {
    
    /**
     * Find notification settings by user ID and student ID
     */
    Optional<ParentNotificationSettings> findByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Find all notification settings for a specific user
     */
    List<ParentNotificationSettings> findByUserId(String userId);
    
    /**
     * Find all notification settings for a specific student
     */
    List<ParentNotificationSettings> findByStudentId(Long studentId);
    
    /**
     * Find by verification token
     */
    Optional<ParentNotificationSettings> findByVerificationToken(String verificationToken);
    
    /**
     * Find all verified parent notification settings
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrue();
    
    /**
     * Find all settings with daily summary enabled
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrueAndDailySummaryEnabledTrue();
    
    /**
     * Find all settings with weekly summary enabled
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrueAndWeeklySummaryEnabledTrue();
    
    /**
     * Find all settings with assignment due notifications enabled
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrueAndNotifyAssignmentDueTrue();
    
    /**
     * Find all settings with assignment missing notifications enabled
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrueAndNotifyAssignmentMissingTrue();
    
    /**
     * Find all settings with assignment graded notifications enabled
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrueAndNotifyAssignmentGradedTrue();
    
    /**
     * Find all settings with calendar sync notifications enabled
     */
    List<ParentNotificationSettings> findByEmailVerifiedTrueAndNotifyCalendarSyncTrue();
    
    /**
     * Check if notification settings exist for user and student
     */
    boolean existsByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Check if email is already registered for any student of this user
     */
    boolean existsByUserIdAndParentEmail(String userId, String parentEmail);
    
    /**
     * Delete notification settings by user ID and student ID
     */
    @Modifying
    @Query("DELETE FROM ParentNotificationSettings pns WHERE pns.userId = :userId AND pns.studentId = :studentId")
    void deleteByUserIdAndStudentId(@Param("userId") String userId, @Param("studentId") Long studentId);
    
    /**
     * Update email verification status
     */
    @Modifying
    @Query("UPDATE ParentNotificationSettings pns SET pns.emailVerified = :verified, pns.verificationToken = null, pns.verificationExpiresAt = null WHERE pns.verificationToken = :token")
    int updateEmailVerificationByToken(@Param("token") String token, @Param("verified") Boolean verified);
    
    /**
     * Clean up expired verification tokens
     */
    @Modifying
    @Query("UPDATE ParentNotificationSettings pns SET pns.verificationToken = null, pns.verificationExpiresAt = null WHERE pns.verificationExpiresAt < :now AND pns.emailVerified = false")
    int cleanupExpiredVerificationTokens(@Param("now") LocalDateTime now);
    
    /**
     * Update notification preferences
     */
    @Modifying
    @Query("UPDATE ParentNotificationSettings pns SET " +
           "pns.notifyAssignmentDue = :assignmentDue, " +
           "pns.notifyAssignmentMissing = :assignmentMissing, " +
           "pns.notifyAssignmentGraded = :assignmentGraded, " +
           "pns.notifyCalendarSync = :calendarSync " +
           "WHERE pns.userId = :userId AND pns.studentId = :studentId")
    int updateNotificationPreferences(
            @Param("userId") String userId,
            @Param("studentId") Long studentId,
            @Param("assignmentDue") Boolean assignmentDue,
            @Param("assignmentMissing") Boolean assignmentMissing,
            @Param("assignmentGraded") Boolean assignmentGraded,
            @Param("calendarSync") Boolean calendarSync
    );
    
    /**
     * Update summary preferences
     */
    @Modifying
    @Query("UPDATE ParentNotificationSettings pns SET " +
           "pns.dailySummaryEnabled = :dailyEnabled, " +
           "pns.dailySummaryTime = :dailyTime, " +
           "pns.weeklySummaryEnabled = :weeklyEnabled, " +
           "pns.weeklySummaryDay = :weeklyDay " +
           "WHERE pns.userId = :userId AND pns.studentId = :studentId")
    int updateSummaryPreferences(
            @Param("userId") String userId,
            @Param("studentId") Long studentId,
            @Param("dailyEnabled") Boolean dailyEnabled,
            @Param("dailyTime") String dailyTime,
            @Param("weeklyEnabled") Boolean weeklyEnabled,
            @Param("weeklyDay") String weeklyDay
    );
}