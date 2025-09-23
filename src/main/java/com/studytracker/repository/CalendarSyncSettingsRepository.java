package com.studytracker.repository;

import com.studytracker.model.CalendarSyncSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarSyncSettingsRepository extends JpaRepository<CalendarSyncSettings, Long> {
    
    /**
     * Find sync settings by user ID and student ID
     */
    Optional<CalendarSyncSettings> findByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Find all sync settings for a specific user
     */
    List<CalendarSyncSettings> findByUserId(String userId);
    
    /**
     * Find all sync settings for a specific student
     */
    List<CalendarSyncSettings> findByStudentId(Long studentId);
    
    /**
     * Find all settings where sync is enabled
     */
    List<CalendarSyncSettings> findBySyncEnabledTrue();
    
    /**
     * Find all settings where auto sync is enabled
     */
    List<CalendarSyncSettings> findByAutoSyncEnabledTrue();
    
    /**
     * Find settings where parent calendar sync is enabled
     */
    List<CalendarSyncSettings> findBySyncToParentCalendarTrue();
    
    /**
     * Find settings where student calendar sync is enabled
     */
    List<CalendarSyncSettings> findBySyncToStudentCalendarTrue();
    
    /**
     * Find settings where completed assignments sync is enabled
     */
    List<CalendarSyncSettings> findBySyncCompletedAssignmentsTrue();
    
    /**
     * Check if sync settings exist for user and student
     */
    boolean existsByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Delete sync settings by user ID and student ID
     */
    @Modifying
    @Query("DELETE FROM CalendarSyncSettings css WHERE css.userId = :userId AND css.studentId = :studentId")
    void deleteByUserIdAndStudentId(@Param("userId") String userId, @Param("studentId") Long studentId);
    
    /**
     * Update sync enabled status
     */
    @Modifying
    @Query("UPDATE CalendarSyncSettings css SET css.syncEnabled = :syncEnabled WHERE css.userId = :userId AND css.studentId = :studentId")
    int updateSyncEnabled(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("syncEnabled") Boolean syncEnabled);
    
    /**
     * Update auto sync enabled status
     */
    @Modifying
    @Query("UPDATE CalendarSyncSettings css SET css.autoSyncEnabled = :autoSyncEnabled WHERE css.userId = :userId AND css.studentId = :studentId")
    int updateAutoSyncEnabled(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("autoSyncEnabled") Boolean autoSyncEnabled);
    
    /**
     * Update parent calendar sync preference
     */
    @Modifying
    @Query("UPDATE CalendarSyncSettings css SET css.syncToParentCalendar = :syncToParent WHERE css.userId = :userId AND css.studentId = :studentId")
    int updateSyncToParentCalendar(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("syncToParent") Boolean syncToParent);
    
    /**
     * Update student calendar sync preference
     */
    @Modifying
    @Query("UPDATE CalendarSyncSettings css SET css.syncToStudentCalendar = :syncToStudent WHERE css.userId = :userId AND css.studentId = :studentId")
    int updateSyncToStudentCalendar(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("syncToStudent") Boolean syncToStudent);
    
    /**
     * Update reminder settings for parent calendar
     */
    @Modifying
    @Query("UPDATE CalendarSyncSettings css SET css.parentReminderMinutes = :reminderMinutes WHERE css.userId = :userId AND css.studentId = :studentId")
    int updateParentReminderMinutes(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("reminderMinutes") String reminderMinutes);
    
    /**
     * Update reminder settings for student calendar
     */
    @Modifying
    @Query("UPDATE CalendarSyncSettings css SET css.studentReminderMinutes = :reminderMinutes WHERE css.userId = :userId AND css.studentId = :studentId")
    int updateStudentReminderMinutes(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("reminderMinutes") String reminderMinutes);
}