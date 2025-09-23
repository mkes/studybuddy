package com.studytracker.repository;

import com.studytracker.model.AccountType;
import com.studytracker.model.CalendarEventMapping;
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
class CalendarEventMappingRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CalendarEventMappingRepository eventMappingRepository;
    
    private CalendarEventMapping parentMapping;
    private CalendarEventMapping studentMapping;
    private CalendarEventMapping oldMapping;
    
    @BeforeEach
    void setUp() {
        LocalDateTime recentSync = LocalDateTime.now().minusMinutes(30);
        LocalDateTime oldSync = LocalDateTime.now().minusDays(2);
        
        parentMapping = new CalendarEventMapping(
                123L, 1L, AccountType.PARENT,
                "parent_event_123", "parent_calendar_456"
        );
        parentMapping.setLastSyncedAt(recentSync);
        
        studentMapping = new CalendarEventMapping(
                123L, 1L, AccountType.STUDENT,
                "student_event_123", "student_calendar_789"
        );
        studentMapping.setLastSyncedAt(recentSync);
        
        oldMapping = new CalendarEventMapping(
                456L, 2L, AccountType.PARENT,
                "old_event_456", "parent_calendar_456"
        );
        oldMapping.setLastSyncedAt(oldSync);
        
        entityManager.persistAndFlush(parentMapping);
        entityManager.persistAndFlush(studentMapping);
        entityManager.persistAndFlush(oldMapping);
    }
    
    @Test
    void testFindByAssignmentIdAndStudentIdAndAccountType() {
        Optional<CalendarEventMapping> found = eventMappingRepository
                .findByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.PARENT);
        
        assertTrue(found.isPresent());
        assertEquals(parentMapping.getId(), found.get().getId());
        assertEquals("parent_event_123", found.get().getGoogleEventId());
    }
    
    @Test
    void testFindByAssignmentIdAndStudentIdAndAccountType_NotFound() {
        Optional<CalendarEventMapping> found = eventMappingRepository
                .findByAssignmentIdAndStudentIdAndAccountType(999L, 1L, AccountType.PARENT);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void testFindByAssignmentIdAndStudentId() {
        List<CalendarEventMapping> mappings = eventMappingRepository
                .findByAssignmentIdAndStudentId(123L, 1L);
        
        assertEquals(2, mappings.size());
        assertTrue(mappings.stream().anyMatch(m -> m.getAccountType() == AccountType.PARENT));
        assertTrue(mappings.stream().anyMatch(m -> m.getAccountType() == AccountType.STUDENT));
    }
    
    @Test
    void testFindByAssignmentId() {
        List<CalendarEventMapping> mappings = eventMappingRepository
                .findByAssignmentId(123L);
        
        assertEquals(2, mappings.size());
        assertTrue(mappings.stream().allMatch(m -> m.getAssignmentId().equals(123L)));
    }
    
    @Test
    void testFindByStudentId() {
        List<CalendarEventMapping> mappings = eventMappingRepository
                .findByStudentId(1L);
        
        assertEquals(2, mappings.size());
        assertTrue(mappings.stream().allMatch(m -> m.getStudentId().equals(1L)));
    }
    
    @Test
    void testFindByStudentIdAndAccountType() {
        List<CalendarEventMapping> parentMappings = eventMappingRepository
                .findByStudentIdAndAccountType(1L, AccountType.PARENT);
        
        assertEquals(1, parentMappings.size());
        assertEquals(parentMapping.getId(), parentMappings.get(0).getId());
        
        List<CalendarEventMapping> studentMappings = eventMappingRepository
                .findByStudentIdAndAccountType(1L, AccountType.STUDENT);
        
        assertEquals(1, studentMappings.size());
        assertEquals(studentMapping.getId(), studentMappings.get(0).getId());
    }
    
    @Test
    void testFindByAccountType() {
        List<CalendarEventMapping> parentMappings = eventMappingRepository
                .findByAccountType(AccountType.PARENT);
        
        assertEquals(2, parentMappings.size());
        assertTrue(parentMappings.stream().allMatch(m -> m.getAccountType() == AccountType.PARENT));
        
        List<CalendarEventMapping> studentMappings = eventMappingRepository
                .findByAccountType(AccountType.STUDENT);
        
        assertEquals(1, studentMappings.size());
        assertEquals(AccountType.STUDENT, studentMappings.get(0).getAccountType());
    }
    
    @Test
    void testFindByGoogleCalendarId() {
        List<CalendarEventMapping> mappings = eventMappingRepository
                .findByGoogleCalendarId("parent_calendar_456");
        
        assertEquals(2, mappings.size());
        assertTrue(mappings.stream().allMatch(m -> 
                "parent_calendar_456".equals(m.getGoogleCalendarId())));
    }
    
    @Test
    void testFindByGoogleEventId() {
        Optional<CalendarEventMapping> found = eventMappingRepository
                .findByGoogleEventId("parent_event_123");
        
        assertTrue(found.isPresent());
        assertEquals(parentMapping.getId(), found.get().getId());
    }
    
    @Test
    void testFindMappingsNotSyncedSince() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<CalendarEventMapping> oldMappings = eventMappingRepository
                .findMappingsNotSyncedSince(threshold);
        
        assertEquals(1, oldMappings.size());
        assertEquals(oldMapping.getId(), oldMappings.get(0).getId());
    }
    
    @Test
    void testFindMappingsSyncedAfter() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<CalendarEventMapping> recentMappings = eventMappingRepository
                .findMappingsSyncedAfter(threshold);
        
        assertEquals(2, recentMappings.size());
        assertFalse(recentMappings.stream().anyMatch(m -> m.getId().equals(oldMapping.getId())));
    }
    
    @Test
    void testExistsByAssignmentIdAndStudentIdAndAccountType() {
        assertTrue(eventMappingRepository
                .existsByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.PARENT));
        
        assertFalse(eventMappingRepository
                .existsByAssignmentIdAndStudentIdAndAccountType(999L, 1L, AccountType.PARENT));
    }
    
    @Test
    void testExistsByAssignmentIdAndStudentId() {
        assertTrue(eventMappingRepository.existsByAssignmentIdAndStudentId(123L, 1L));
        assertFalse(eventMappingRepository.existsByAssignmentIdAndStudentId(999L, 1L));
    }
    
    @Test
    void testDeleteByAssignmentIdAndStudentId() {
        assertTrue(eventMappingRepository.existsByAssignmentIdAndStudentId(123L, 1L));
        
        eventMappingRepository.deleteByAssignmentIdAndStudentId(123L, 1L);
        entityManager.flush();
        
        assertFalse(eventMappingRepository.existsByAssignmentIdAndStudentId(123L, 1L));
    }
    
    @Test
    void testDeleteByAssignmentIdAndStudentIdAndAccountType() {
        assertTrue(eventMappingRepository
                .existsByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.PARENT));
        
        eventMappingRepository
                .deleteByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.PARENT);
        entityManager.flush();
        
        assertFalse(eventMappingRepository
                .existsByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.PARENT));
        
        // Student mapping should still exist
        assertTrue(eventMappingRepository
                .existsByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.STUDENT));
    }
    
    @Test
    void testDeleteByStudentId() {
        assertEquals(2, eventMappingRepository.findByStudentId(1L).size());
        
        eventMappingRepository.deleteByStudentId(1L);
        entityManager.flush();
        
        assertEquals(0, eventMappingRepository.findByStudentId(1L).size());
        
        // Other student's mappings should remain
        assertEquals(1, eventMappingRepository.findByStudentId(2L).size());
    }
    
    @Test
    void testDeleteByStudentIdAndAccountType() {
        assertEquals(1, eventMappingRepository
                .findByStudentIdAndAccountType(1L, AccountType.PARENT).size());
        
        eventMappingRepository.deleteByStudentIdAndAccountType(1L, AccountType.PARENT);
        entityManager.flush();
        
        assertEquals(0, eventMappingRepository
                .findByStudentIdAndAccountType(1L, AccountType.PARENT).size());
        
        // Student mappings should remain
        assertEquals(1, eventMappingRepository
                .findByStudentIdAndAccountType(1L, AccountType.STUDENT).size());
    }
    
    @Test
    void testDeleteByGoogleCalendarId() {
        assertEquals(2, eventMappingRepository
                .findByGoogleCalendarId("parent_calendar_456").size());
        
        eventMappingRepository.deleteByGoogleCalendarId("parent_calendar_456");
        entityManager.flush();
        
        assertEquals(0, eventMappingRepository
                .findByGoogleCalendarId("parent_calendar_456").size());
        
        // Other calendar mappings should remain
        assertEquals(1, eventMappingRepository
                .findByGoogleCalendarId("student_calendar_789").size());
    }
    
    @Test
    @Transactional
    void testUpdateLastSyncedAt() {
        LocalDateTime newSyncTime = LocalDateTime.now();
        
        int updatedCount = eventMappingRepository.updateLastSyncedAt(parentMapping.getId(), newSyncTime);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarEventMapping> updated = eventMappingRepository.findById(parentMapping.getId());
        assertTrue(updated.isPresent());
        assertEquals(newSyncTime, updated.get().getLastSyncedAt());
    }
    
    @Test
    @Transactional
    void testUpdateGoogleEventId() {
        String newEventId = "new_event_id_123";
        
        int updatedCount = eventMappingRepository.updateGoogleEventId(
                123L, 1L, AccountType.PARENT, newEventId);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarEventMapping> updated = eventMappingRepository
                .findByAssignmentIdAndStudentIdAndAccountType(123L, 1L, AccountType.PARENT);
        assertTrue(updated.isPresent());
        assertEquals(newEventId, updated.get().getGoogleEventId());
    }
    
    @Test
    void testCountByStudentId() {
        long count = eventMappingRepository.countByStudentId(1L);
        assertEquals(2, count);
        
        long count2 = eventMappingRepository.countByStudentId(2L);
        assertEquals(1, count2);
        
        long count3 = eventMappingRepository.countByStudentId(999L);
        assertEquals(0, count3);
    }
    
    @Test
    void testCountByStudentIdAndAccountType() {
        long parentCount = eventMappingRepository.countByStudentIdAndAccountType(1L, AccountType.PARENT);
        assertEquals(1, parentCount);
        
        long studentCount = eventMappingRepository.countByStudentIdAndAccountType(1L, AccountType.STUDENT);
        assertEquals(1, studentCount);
        
        long noCount = eventMappingRepository.countByStudentIdAndAccountType(999L, AccountType.PARENT);
        assertEquals(0, noCount);
    }
    
    @Test
    void testUniqueConstraint() {
        // Try to create duplicate mapping with same assignment, student, and account type
        CalendarEventMapping duplicate = new CalendarEventMapping(
                123L, 1L, AccountType.PARENT,
                "duplicate_event", "duplicate_calendar"
        );
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }
}