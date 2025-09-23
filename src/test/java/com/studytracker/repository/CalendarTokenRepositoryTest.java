package com.studytracker.repository;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
class CalendarTokenRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CalendarTokenRepository calendarTokenRepository;
    
    private CalendarToken parentToken;
    private CalendarToken studentToken;
    private CalendarToken expiredToken;
    
    @BeforeEach
    void setUp() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        parentToken = new CalendarToken(
                "user123",
                1L,
                AccountType.PARENT,
                "encrypted_parent_access",
                "encrypted_parent_refresh",
                futureTime
        );
        parentToken.setGoogleEmail("parent@example.com");
        parentToken.setCalendarId("parent_calendar_123");
        
        studentToken = new CalendarToken(
                "user123",
                1L,
                AccountType.STUDENT,
                "encrypted_student_access",
                "encrypted_student_refresh",
                futureTime
        );
        studentToken.setGoogleEmail("student@example.com");
        studentToken.setCalendarId("student_calendar_456");
        
        expiredToken = new CalendarToken(
                "user456",
                2L,
                AccountType.PARENT,
                "encrypted_expired_access",
                "encrypted_expired_refresh",
                pastTime
        );
        
        entityManager.persistAndFlush(parentToken);
        entityManager.persistAndFlush(studentToken);
        entityManager.persistAndFlush(expiredToken);
    }
    
    @Test
    void testFindByUserIdAndStudentIdAndAccountType() {
        Optional<CalendarToken> found = calendarTokenRepository
                .findByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.PARENT);
        
        assertTrue(found.isPresent());
        assertEquals(parentToken.getEncryptedAccessToken(), found.get().getEncryptedAccessToken());
        assertEquals("parent@example.com", found.get().getGoogleEmail());
    }
    
    @Test
    void testFindByUserIdAndStudentIdAndAccountType_NotFound() {
        Optional<CalendarToken> found = calendarTokenRepository
                .findByUserIdAndStudentIdAndAccountType("nonexistent", 1L, AccountType.PARENT);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void testFindByUserIdAndStudentId() {
        List<CalendarToken> tokens = calendarTokenRepository
                .findByUserIdAndStudentId("user123", 1L);
        
        assertEquals(2, tokens.size());
        assertTrue(tokens.stream().anyMatch(t -> t.getAccountType() == AccountType.PARENT));
        assertTrue(tokens.stream().anyMatch(t -> t.getAccountType() == AccountType.STUDENT));
    }
    
    @Test
    void testFindByUserId() {
        List<CalendarToken> tokens = calendarTokenRepository.findByUserId("user123");
        
        assertEquals(2, tokens.size());
        assertTrue(tokens.stream().allMatch(t -> "user123".equals(t.getUserId())));
    }
    
    @Test
    void testFindByStudentId() {
        List<CalendarToken> tokens = calendarTokenRepository.findByStudentId(1L);
        
        assertEquals(2, tokens.size());
        assertTrue(tokens.stream().allMatch(t -> t.getStudentId().equals(1L)));
    }
    
    @Test
    void testFindByAccountType() {
        List<CalendarToken> parentTokens = calendarTokenRepository
                .findByAccountType(AccountType.PARENT);
        
        assertEquals(2, parentTokens.size());
        assertTrue(parentTokens.stream().allMatch(t -> t.getAccountType() == AccountType.PARENT));
        
        List<CalendarToken> studentTokens = calendarTokenRepository
                .findByAccountType(AccountType.STUDENT);
        
        assertEquals(1, studentTokens.size());
        assertEquals(AccountType.STUDENT, studentTokens.get(0).getAccountType());
    }
    
    @Test
    void testFindExpiredTokens() {
        LocalDateTime currentTime = LocalDateTime.now();
        List<CalendarToken> expiredTokens = calendarTokenRepository
                .findExpiredTokens(currentTime);
        
        assertEquals(1, expiredTokens.size());
        assertEquals(expiredToken.getId(), expiredTokens.get(0).getId());
    }
    
    @Test
    void testFindTokensExpiringWithin() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursFromNow = now.plusHours(2);
        
        List<CalendarToken> expiringTokens = calendarTokenRepository
                .findTokensExpiringWithin(now, twoHoursFromNow);
        
        assertEquals(2, expiringTokens.size());
        assertTrue(expiringTokens.stream().noneMatch(t -> t.getId().equals(expiredToken.getId())));
    }
    
    @Test
    void testExistsByUserIdAndStudentIdAndAccountType() {
        assertTrue(calendarTokenRepository
                .existsByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.PARENT));
        
        assertFalse(calendarTokenRepository
                .existsByUserIdAndStudentIdAndAccountType("nonexistent", 1L, AccountType.PARENT));
    }
    
    @Test
    void testExistsByUserIdAndStudentId() {
        assertTrue(calendarTokenRepository.existsByUserIdAndStudentId("user123", 1L));
        assertFalse(calendarTokenRepository.existsByUserIdAndStudentId("nonexistent", 1L));
    }
    
    @Test
    void testDeleteByUserIdAndStudentId() {
        assertTrue(calendarTokenRepository.existsByUserIdAndStudentId("user123", 1L));
        
        calendarTokenRepository.deleteByUserIdAndStudentId("user123", 1L);
        entityManager.flush();
        
        assertFalse(calendarTokenRepository.existsByUserIdAndStudentId("user123", 1L));
    }
    
    @Test
    void testDeleteByUserIdAndStudentIdAndAccountType() {
        assertTrue(calendarTokenRepository
                .existsByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.PARENT));
        
        calendarTokenRepository
                .deleteByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.PARENT);
        entityManager.flush();
        
        assertFalse(calendarTokenRepository
                .existsByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.PARENT));
        
        // Student token should still exist
        assertTrue(calendarTokenRepository
                .existsByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.STUDENT));
    }
    
    @Test
    void testDeleteExpiredTokens() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Verify expired token exists
        assertEquals(1, calendarTokenRepository.findExpiredTokens(currentTime).size());
        
        int deletedCount = calendarTokenRepository.deleteExpiredTokens(currentTime);
        entityManager.flush();
        
        assertEquals(1, deletedCount);
        assertEquals(0, calendarTokenRepository.findExpiredTokens(currentTime).size());
    }
    
    @Test
    @Transactional
    void testUpdateCalendarId() {
        String newCalendarId = "new_calendar_123";
        
        int updatedCount = calendarTokenRepository.updateCalendarId(
                "user123", 1L, AccountType.PARENT, newCalendarId);
        entityManager.flush();
        entityManager.clear(); // Clear the cache to force a fresh fetch
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarToken> updated = calendarTokenRepository
                .findByUserIdAndStudentIdAndAccountType("user123", 1L, AccountType.PARENT);
        
        assertTrue(updated.isPresent());
        assertEquals(newCalendarId, updated.get().getCalendarId());
    }
    
    @Test
    @Transactional
    void testUpdateTokenExpiration() {
        LocalDateTime newExpirationTime = LocalDateTime.now().plusDays(1);
        
        int updatedCount = calendarTokenRepository.updateTokenExpiration(
                parentToken.getId(), newExpirationTime);
        entityManager.flush();
        entityManager.clear(); // Clear the cache to force a fresh fetch
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarToken> updated = calendarTokenRepository.findById(parentToken.getId());
        assertTrue(updated.isPresent());
        assertEquals(newExpirationTime, updated.get().getTokenExpiresAt());
    }
    
    @Test
    void testUniqueConstraint() {
        // Try to create duplicate token with same user, student, and account type
        CalendarToken duplicate = new CalendarToken(
                "user123",
                1L,
                AccountType.PARENT,
                "duplicate_access",
                "duplicate_refresh",
                LocalDateTime.now().plusHours(1)
        );
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }
}