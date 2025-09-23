package com.studytracker.repository;

import com.studytracker.model.InvitationStatus;
import com.studytracker.model.StudentCalendarInvitation;
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
class StudentCalendarInvitationRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private StudentCalendarInvitationRepository invitationRepository;
    
    private StudentCalendarInvitation pendingInvitation;
    private StudentCalendarInvitation acceptedInvitation;
    private StudentCalendarInvitation expiredInvitation;
    
    @BeforeEach
    void setUp() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        LocalDateTime acceptedTime = LocalDateTime.now().minusHours(2);
        
        pendingInvitation = new StudentCalendarInvitation(
                "parent123", 1L, "student1@example.com",
                "token_123", futureTime
        );
        pendingInvitation.setStatus(InvitationStatus.PENDING);
        
        acceptedInvitation = new StudentCalendarInvitation(
                "parent123", 2L, "student2@example.com",
                "token_456", futureTime
        );
        acceptedInvitation.setStatus(InvitationStatus.ACCEPTED);
        acceptedInvitation.setAcceptedAt(acceptedTime);
        
        expiredInvitation = new StudentCalendarInvitation(
                "parent456", 3L, "student3@example.com",
                "token_789", pastTime
        );
        expiredInvitation.setStatus(InvitationStatus.PENDING);
        
        entityManager.persistAndFlush(pendingInvitation);
        entityManager.persistAndFlush(acceptedInvitation);
        entityManager.persistAndFlush(expiredInvitation);
    }
    
    @Test
    void testFindByUserIdAndStudentId() {
        Optional<StudentCalendarInvitation> found = invitationRepository
                .findByUserIdAndStudentId("parent123", 1L);
        
        assertTrue(found.isPresent());
        assertEquals(pendingInvitation.getId(), found.get().getId());
        assertEquals("student1@example.com", found.get().getStudentEmail());
    }
    
    @Test
    void testFindByUserIdAndStudentId_NotFound() {
        Optional<StudentCalendarInvitation> found = invitationRepository
                .findByUserIdAndStudentId("nonexistent", 1L);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void testFindByInvitationToken() {
        Optional<StudentCalendarInvitation> found = invitationRepository
                .findByInvitationToken("token_123");
        
        assertTrue(found.isPresent());
        assertEquals(pendingInvitation.getId(), found.get().getId());
    }
    
    @Test
    void testFindByUserId() {
        List<StudentCalendarInvitation> invitations = invitationRepository
                .findByUserId("parent123");
        
        assertEquals(2, invitations.size());
        assertTrue(invitations.stream().allMatch(i -> "parent123".equals(i.getUserId())));
    }
    
    @Test
    void testFindByStudentId() {
        List<StudentCalendarInvitation> invitations = invitationRepository
                .findByStudentId(1L);
        
        assertEquals(1, invitations.size());
        assertEquals(pendingInvitation.getId(), invitations.get(0).getId());
    }
    
    @Test
    void testFindByStudentEmail() {
        List<StudentCalendarInvitation> invitations = invitationRepository
                .findByStudentEmail("student1@example.com");
        
        assertEquals(1, invitations.size());
        assertEquals(pendingInvitation.getId(), invitations.get(0).getId());
    }
    
    @Test
    void testFindByStatus() {
        List<StudentCalendarInvitation> pendingInvitations = invitationRepository
                .findByStatus(InvitationStatus.PENDING);
        
        assertEquals(2, pendingInvitations.size());
        assertTrue(pendingInvitations.stream().anyMatch(i -> i.getId().equals(pendingInvitation.getId())));
        assertTrue(pendingInvitations.stream().anyMatch(i -> i.getId().equals(expiredInvitation.getId())));
        
        List<StudentCalendarInvitation> acceptedInvitations = invitationRepository
                .findByStatus(InvitationStatus.ACCEPTED);
        
        assertEquals(1, acceptedInvitations.size());
        assertEquals(acceptedInvitation.getId(), acceptedInvitations.get(0).getId());
    }
    
    @Test
    void testFindByStatusAndExpiresAtAfter() {
        LocalDateTime currentTime = LocalDateTime.now();
        List<StudentCalendarInvitation> activePending = invitationRepository
                .findByStatusAndExpiresAtAfter(InvitationStatus.PENDING, currentTime);
        
        assertEquals(1, activePending.size());
        assertEquals(pendingInvitation.getId(), activePending.get(0).getId());
    }
    
    @Test
    void testFindExpiredInvitations() {
        LocalDateTime currentTime = LocalDateTime.now();
        List<StudentCalendarInvitation> expired = invitationRepository
                .findExpiredInvitations(InvitationStatus.PENDING, currentTime);
        
        assertEquals(1, expired.size());
        assertEquals(expiredInvitation.getId(), expired.get(0).getId());
    }
    
    @Test
    void testFindInvitationsExpiringWithin() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekFromNow = now.plusDays(7).plusHours(1);
        
        List<StudentCalendarInvitation> expiringSoon = invitationRepository
                .findInvitationsExpiringWithin(InvitationStatus.PENDING, now, oneWeekFromNow);
        
        assertEquals(1, expiringSoon.size());
        assertEquals(pendingInvitation.getId(), expiringSoon.get(0).getId());
    }
    
    @Test
    void testFindByStatusAndAcceptedAtIsNotNull() {
        List<StudentCalendarInvitation> acceptedWithTime = invitationRepository
                .findByStatusAndAcceptedAtIsNotNull(InvitationStatus.ACCEPTED);
        
        assertEquals(1, acceptedWithTime.size());
        assertEquals(acceptedInvitation.getId(), acceptedWithTime.get(0).getId());
        assertNotNull(acceptedWithTime.get(0).getAcceptedAt());
    }
    
    @Test
    void testExistsByUserIdAndStudentId() {
        assertTrue(invitationRepository.existsByUserIdAndStudentId("parent123", 1L));
        assertFalse(invitationRepository.existsByUserIdAndStudentId("nonexistent", 1L));
    }
    
    @Test
    void testExistsByInvitationToken() {
        assertTrue(invitationRepository.existsByInvitationToken("token_123"));
        assertFalse(invitationRepository.existsByInvitationToken("nonexistent_token"));
    }
    
    @Test
    void testExistsActiveInvitation() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        assertTrue(invitationRepository.existsActiveInvitation(
                "parent123", 1L, InvitationStatus.PENDING, currentTime));
        
        assertFalse(invitationRepository.existsActiveInvitation(
                "parent456", 3L, InvitationStatus.PENDING, currentTime));
        
        assertFalse(invitationRepository.existsActiveInvitation(
                "nonexistent", 1L, InvitationStatus.PENDING, currentTime));
    }
    
    @Test
    void testDeleteByUserIdAndStudentId() {
        assertTrue(invitationRepository.existsByUserIdAndStudentId("parent123", 1L));
        
        invitationRepository.deleteByUserIdAndStudentId("parent123", 1L);
        entityManager.flush();
        
        assertFalse(invitationRepository.existsByUserIdAndStudentId("parent123", 1L));
    }
    
    @Test
    void testDeleteByInvitationToken() {
        assertTrue(invitationRepository.existsByInvitationToken("token_123"));
        
        invitationRepository.deleteByInvitationToken("token_123");
        entityManager.flush();
        
        assertFalse(invitationRepository.existsByInvitationToken("token_123"));
    }
    
    @Test
    void testDeleteExpiredInvitations() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Verify expired invitation exists
        assertEquals(1, invitationRepository
                .findExpiredInvitations(InvitationStatus.PENDING, currentTime).size());
        
        int deletedCount = invitationRepository.deleteExpiredInvitations(currentTime);
        entityManager.flush();
        
        assertEquals(1, deletedCount);
        assertEquals(0, invitationRepository
                .findExpiredInvitations(InvitationStatus.PENDING, currentTime).size());
    }
    
    @Test
    @Transactional
    void testUpdateInvitationStatus() {
        int updatedCount = invitationRepository.updateInvitationStatus(
                "token_123", InvitationStatus.EXPIRED);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<StudentCalendarInvitation> updated = invitationRepository
                .findByInvitationToken("token_123");
        assertTrue(updated.isPresent());
        assertEquals(InvitationStatus.EXPIRED, updated.get().getStatus());
    }
    
    @Test
    @Transactional
    void testUpdateInvitationStatusAndAcceptedAt() {
        LocalDateTime acceptedTime = LocalDateTime.now();
        
        int updatedCount = invitationRepository.updateInvitationStatusAndAcceptedAt(
                "token_123", InvitationStatus.ACCEPTED, acceptedTime);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<StudentCalendarInvitation> updated = invitationRepository
                .findByInvitationToken("token_123");
        assertTrue(updated.isPresent());
        assertEquals(InvitationStatus.ACCEPTED, updated.get().getStatus());
        assertEquals(acceptedTime, updated.get().getAcceptedAt());
    }
    
    @Test
    @Transactional
    void testMarkExpiredInvitations() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        int markedCount = invitationRepository.markExpiredInvitations(
                InvitationStatus.PENDING, InvitationStatus.EXPIRED, currentTime);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, markedCount);
        
        Optional<StudentCalendarInvitation> updated = invitationRepository
                .findById(expiredInvitation.getId());
        assertTrue(updated.isPresent());
        assertEquals(InvitationStatus.EXPIRED, updated.get().getStatus());
    }
    
    @Test
    void testCountByStatus() {
        long pendingCount = invitationRepository.countByStatus(InvitationStatus.PENDING);
        assertEquals(2, pendingCount);
        
        long acceptedCount = invitationRepository.countByStatus(InvitationStatus.ACCEPTED);
        assertEquals(1, acceptedCount);
        
        long expiredCount = invitationRepository.countByStatus(InvitationStatus.EXPIRED);
        assertEquals(0, expiredCount);
    }
    
    @Test
    void testCountActiveInvitationsByUser() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        long activeCount = invitationRepository.countActiveInvitationsByUser(
                "parent123", InvitationStatus.PENDING, currentTime);
        assertEquals(1, activeCount);
        
        long noActiveCount = invitationRepository.countActiveInvitationsByUser(
                "parent456", InvitationStatus.PENDING, currentTime);
        assertEquals(0, noActiveCount);
    }
    
    @Test
    void testUniqueConstraint() {
        // Try to create duplicate invitation with same user and student
        StudentCalendarInvitation duplicate = new StudentCalendarInvitation(
                "parent123", 1L, "duplicate@example.com",
                "duplicate_token", LocalDateTime.now().plusDays(1)
        );
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }
    
    @Test
    void testUniqueTokenConstraint() {
        // Try to create invitation with duplicate token
        StudentCalendarInvitation duplicateToken = new StudentCalendarInvitation(
                "parent789", 99L, "unique@example.com",
                "token_123", LocalDateTime.now().plusDays(1)
        );
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicateToken);
        });
    }
}