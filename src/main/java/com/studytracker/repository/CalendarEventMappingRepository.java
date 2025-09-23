package com.studytracker.repository;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarEventMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventMappingRepository extends JpaRepository<CalendarEventMapping, Long> {
    
    /**
     * Find mapping by assignment ID, student ID, and account type
     */
    Optional<CalendarEventMapping> findByAssignmentIdAndStudentIdAndAccountType(
            Long assignmentId, Long studentId, AccountType accountType);
    
    /**
     * Find all mappings for a specific assignment and student
     */
    List<CalendarEventMapping> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    
    /**
     * Find all mappings for a specific assignment
     */
    List<CalendarEventMapping> findByAssignmentId(Long assignmentId);
    
    /**
     * Find all mappings for a specific student
     */
    List<CalendarEventMapping> findByStudentId(Long studentId);
    
    /**
     * Find all mappings for a specific student and account type
     */
    List<CalendarEventMapping> findByStudentIdAndAccountType(Long studentId, AccountType accountType);
    
    /**
     * Find all mappings by account type
     */
    List<CalendarEventMapping> findByAccountType(AccountType accountType);
    
    /**
     * Find mappings by Google Calendar ID
     */
    List<CalendarEventMapping> findByGoogleCalendarId(String googleCalendarId);
    
    /**
     * Find mapping by Google Event ID
     */
    Optional<CalendarEventMapping> findByGoogleEventId(String googleEventId);
    
    /**
     * Find mappings that haven't been synced recently
     */
    @Query("SELECT cem FROM CalendarEventMapping cem WHERE cem.lastSyncedAt IS NULL OR cem.lastSyncedAt < :threshold")
    List<CalendarEventMapping> findMappingsNotSyncedSince(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find mappings synced after a specific time
     */
    @Query("SELECT cem FROM CalendarEventMapping cem WHERE cem.lastSyncedAt >= :syncTime")
    List<CalendarEventMapping> findMappingsSyncedAfter(@Param("syncTime") LocalDateTime syncTime);
    
    /**
     * Check if mapping exists for assignment, student, and account type
     */
    boolean existsByAssignmentIdAndStudentIdAndAccountType(
            Long assignmentId, Long studentId, AccountType accountType);
    
    /**
     * Check if any mapping exists for assignment and student
     */
    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    
    /**
     * Delete mappings by assignment ID and student ID
     */
    @Modifying
    @Query("DELETE FROM CalendarEventMapping cem WHERE cem.assignmentId = :assignmentId AND cem.studentId = :studentId")
    void deleteByAssignmentIdAndStudentId(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);
    
    /**
     * Delete mapping by assignment ID, student ID, and account type
     */
    @Modifying
    @Query("DELETE FROM CalendarEventMapping cem WHERE cem.assignmentId = :assignmentId AND cem.studentId = :studentId AND cem.accountType = :accountType")
    void deleteByAssignmentIdAndStudentIdAndAccountType(
            @Param("assignmentId") Long assignmentId, 
            @Param("studentId") Long studentId, 
            @Param("accountType") AccountType accountType);
    
    /**
     * Delete all mappings for a specific student
     */
    @Modifying
    @Query("DELETE FROM CalendarEventMapping cem WHERE cem.studentId = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
    
    /**
     * Delete all mappings for a specific student and account type
     */
    @Modifying
    @Query("DELETE FROM CalendarEventMapping cem WHERE cem.studentId = :studentId AND cem.accountType = :accountType")
    void deleteByStudentIdAndAccountType(@Param("studentId") Long studentId, @Param("accountType") AccountType accountType);
    
    /**
     * Delete mappings by Google Calendar ID
     */
    @Modifying
    @Query("DELETE FROM CalendarEventMapping cem WHERE cem.googleCalendarId = :calendarId")
    void deleteByGoogleCalendarId(@Param("calendarId") String calendarId);
    
    /**
     * Update last synced timestamp for a mapping
     */
    @Modifying
    @Query("UPDATE CalendarEventMapping cem SET cem.lastSyncedAt = :syncTime WHERE cem.id = :id")
    int updateLastSyncedAt(@Param("id") Long id, @Param("syncTime") LocalDateTime syncTime);
    
    /**
     * Update Google Event ID for a mapping
     */
    @Modifying
    @Query("UPDATE CalendarEventMapping cem SET cem.googleEventId = :eventId WHERE cem.assignmentId = :assignmentId AND cem.studentId = :studentId AND cem.accountType = :accountType")
    int updateGoogleEventId(
            @Param("assignmentId") Long assignmentId, 
            @Param("studentId") Long studentId, 
            @Param("accountType") AccountType accountType, 
            @Param("eventId") String eventId);
    
    /**
     * Count mappings for a specific student
     */
    @Query("SELECT COUNT(cem) FROM CalendarEventMapping cem WHERE cem.studentId = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
    
    /**
     * Count mappings for a specific student and account type
     */
    @Query("SELECT COUNT(cem) FROM CalendarEventMapping cem WHERE cem.studentId = :studentId AND cem.accountType = :accountType")
    long countByStudentIdAndAccountType(@Param("studentId") Long studentId, @Param("accountType") AccountType accountType);
}