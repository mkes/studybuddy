package com.studytracker.repository;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarTokenRepository extends JpaRepository<CalendarToken, Long> {
    
    /**
     * Find token by user ID, student ID, and account type
     */
    Optional<CalendarToken> findByUserIdAndStudentIdAndAccountType(
            String userId, Long studentId, AccountType accountType);
    
    /**
     * Find all tokens for a specific user and student
     */
    List<CalendarToken> findByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Find all tokens for a specific user
     */
    List<CalendarToken> findByUserId(String userId);
    
    /**
     * Find all tokens for a specific student
     */
    List<CalendarToken> findByStudentId(Long studentId);
    
    /**
     * Find all tokens by account type
     */
    List<CalendarToken> findByAccountType(AccountType accountType);
    
    /**
     * Find tokens that are expired
     */
    @Query("SELECT ct FROM CalendarToken ct WHERE ct.tokenExpiresAt < :currentTime")
    List<CalendarToken> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find tokens expiring within a specific time window
     */
    @Query("SELECT ct FROM CalendarToken ct WHERE ct.tokenExpiresAt BETWEEN :now AND :expiryThreshold")
    List<CalendarToken> findTokensExpiringWithin(
            @Param("now") LocalDateTime now, 
            @Param("expiryThreshold") LocalDateTime expiryThreshold);
    
    /**
     * Check if a calendar connection exists for user, student, and account type
     */
    boolean existsByUserIdAndStudentIdAndAccountType(
            String userId, Long studentId, AccountType accountType);
    
    /**
     * Check if any calendar connection exists for user and student
     */
    boolean existsByUserIdAndStudentId(String userId, Long studentId);
    
    /**
     * Delete tokens by user ID and student ID
     */
    @Modifying
    @Query("DELETE FROM CalendarToken ct WHERE ct.userId = :userId AND ct.studentId = :studentId")
    void deleteByUserIdAndStudentId(@Param("userId") String userId, @Param("studentId") Long studentId);
    
    /**
     * Delete tokens by user ID, student ID, and account type
     */
    @Modifying
    @Query("DELETE FROM CalendarToken ct WHERE ct.userId = :userId AND ct.studentId = :studentId AND ct.accountType = :accountType")
    void deleteByUserIdAndStudentIdAndAccountType(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("accountType") AccountType accountType);
    
    /**
     * Delete all expired tokens
     */
    @Modifying
    @Query("DELETE FROM CalendarToken ct WHERE ct.tokenExpiresAt < :currentTime")
    int deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Update calendar ID for a specific token
     */
    @Modifying
    @Query("UPDATE CalendarToken ct SET ct.calendarId = :calendarId WHERE ct.userId = :userId AND ct.studentId = :studentId AND ct.accountType = :accountType")
    int updateCalendarId(
            @Param("userId") String userId, 
            @Param("studentId") Long studentId, 
            @Param("accountType") AccountType accountType, 
            @Param("calendarId") String calendarId);
    
    /**
     * Update token expiration time
     */
    @Modifying
    @Query("UPDATE CalendarToken ct SET ct.tokenExpiresAt = :expiresAt WHERE ct.id = :id")
    int updateTokenExpiration(@Param("id") Long id, @Param("expiresAt") LocalDateTime expiresAt);
}