package com.studytracker.repository;

import com.studytracker.model.CalendarSyncSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
class CalendarSyncSettingsRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CalendarSyncSettingsRepository syncSettingsRepository;
    
    private CalendarSyncSettings settings1;
    private CalendarSyncSettings settings2;
    private CalendarSyncSettings disabledSettings;
    
    @BeforeEach
    void setUp() {
        settings1 = new CalendarSyncSettings("user123", 1L);
        settings1.setSyncEnabled(true);
        settings1.setAutoSyncEnabled(true);
        settings1.setSyncToParentCalendar(true);
        settings1.setSyncToStudentCalendar(true);
        settings1.setSyncCompletedAssignments(false);
        settings1.setIncludedCourses("[\"Math\",\"Science\"]");
        settings1.setExcludedAssignmentTypes("[\"quiz\"]");
        
        settings2 = new CalendarSyncSettings("user123", 2L);
        settings2.setSyncEnabled(true);
        settings2.setAutoSyncEnabled(false);
        settings2.setSyncToParentCalendar(false);
        settings2.setSyncToStudentCalendar(true);
        settings2.setSyncCompletedAssignments(true);
        
        disabledSettings = new CalendarSyncSettings("user456", 3L);
        disabledSettings.setSyncEnabled(false);
        disabledSettings.setAutoSyncEnabled(false);
        disabledSettings.setSyncToParentCalendar(false);
        disabledSettings.setSyncToStudentCalendar(false);
        disabledSettings.setSyncCompletedAssignments(false);
        
        entityManager.persistAndFlush(settings1);
        entityManager.persistAndFlush(settings2);
        entityManager.persistAndFlush(disabledSettings);
    }
    
    @Test
    void testFindByUserIdAndStudentId() {
        Optional<CalendarSyncSettings> found = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        
        assertTrue(found.isPresent());
        assertEquals(settings1.getId(), found.get().getId());
        assertEquals("[\"Math\",\"Science\"]", found.get().getIncludedCourses());
    }
    
    @Test
    void testFindByUserIdAndStudentId_NotFound() {
        Optional<CalendarSyncSettings> found = syncSettingsRepository
                .findByUserIdAndStudentId("nonexistent", 1L);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void testFindByUserId() {
        List<CalendarSyncSettings> settings = syncSettingsRepository.findByUserId("user123");
        
        assertEquals(2, settings.size());
        assertTrue(settings.stream().allMatch(s -> "user123".equals(s.getUserId())));
    }
    
    @Test
    void testFindByStudentId() {
        List<CalendarSyncSettings> settings = syncSettingsRepository.findByStudentId(1L);
        
        assertEquals(1, settings.size());
        assertEquals(settings1.getId(), settings.get(0).getId());
    }
    
    @Test
    void testFindBySyncEnabledTrue() {
        List<CalendarSyncSettings> enabledSettings = syncSettingsRepository
                .findBySyncEnabledTrue();
        
        assertEquals(2, enabledSettings.size());
        assertTrue(enabledSettings.stream().allMatch(CalendarSyncSettings::getSyncEnabled));
        assertFalse(enabledSettings.stream().anyMatch(s -> s.getId().equals(disabledSettings.getId())));
    }
    
    @Test
    void testFindByAutoSyncEnabledTrue() {
        List<CalendarSyncSettings> autoSyncEnabled = syncSettingsRepository
                .findByAutoSyncEnabledTrue();
        
        assertEquals(1, autoSyncEnabled.size());
        assertEquals(settings1.getId(), autoSyncEnabled.get(0).getId());
    }
    
    @Test
    void testFindBySyncToParentCalendarTrue() {
        List<CalendarSyncSettings> parentSyncEnabled = syncSettingsRepository
                .findBySyncToParentCalendarTrue();
        
        assertEquals(1, parentSyncEnabled.size());
        assertEquals(settings1.getId(), parentSyncEnabled.get(0).getId());
    }
    
    @Test
    void testFindBySyncToStudentCalendarTrue() {
        List<CalendarSyncSettings> studentSyncEnabled = syncSettingsRepository
                .findBySyncToStudentCalendarTrue();
        
        assertEquals(2, studentSyncEnabled.size());
        assertTrue(studentSyncEnabled.stream().anyMatch(s -> s.getId().equals(settings1.getId())));
        assertTrue(studentSyncEnabled.stream().anyMatch(s -> s.getId().equals(settings2.getId())));
    }
    
    @Test
    void testFindBySyncCompletedAssignmentsTrue() {
        List<CalendarSyncSettings> completedSyncEnabled = syncSettingsRepository
                .findBySyncCompletedAssignmentsTrue();
        
        assertEquals(1, completedSyncEnabled.size());
        assertEquals(settings2.getId(), completedSyncEnabled.get(0).getId());
    }
    
    @Test
    void testExistsByUserIdAndStudentId() {
        assertTrue(syncSettingsRepository.existsByUserIdAndStudentId("user123", 1L));
        assertFalse(syncSettingsRepository.existsByUserIdAndStudentId("nonexistent", 1L));
    }
    
    @Test
    void testDeleteByUserIdAndStudentId() {
        assertTrue(syncSettingsRepository.existsByUserIdAndStudentId("user123", 1L));
        
        syncSettingsRepository.deleteByUserIdAndStudentId("user123", 1L);
        entityManager.flush();
        
        assertFalse(syncSettingsRepository.existsByUserIdAndStudentId("user123", 1L));
    }
    
    @Test
    @Transactional
    void testUpdateSyncEnabled() {
        int updatedCount = syncSettingsRepository.updateSyncEnabled("user123", 1L, false);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarSyncSettings> updated = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        assertTrue(updated.isPresent());
        assertFalse(updated.get().getSyncEnabled());
    }
    
    @Test
    @Transactional
    void testUpdateAutoSyncEnabled() {
        int updatedCount = syncSettingsRepository.updateAutoSyncEnabled("user123", 1L, false);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarSyncSettings> updated = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        assertTrue(updated.isPresent());
        assertFalse(updated.get().getAutoSyncEnabled());
    }
    
    @Test
    @Transactional
    void testUpdateSyncToParentCalendar() {
        int updatedCount = syncSettingsRepository.updateSyncToParentCalendar("user123", 1L, false);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarSyncSettings> updated = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        assertTrue(updated.isPresent());
        assertFalse(updated.get().getSyncToParentCalendar());
    }
    
    @Test
    @Transactional
    void testUpdateSyncToStudentCalendar() {
        int updatedCount = syncSettingsRepository.updateSyncToStudentCalendar("user123", 1L, false);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarSyncSettings> updated = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        assertTrue(updated.isPresent());
        assertFalse(updated.get().getSyncToStudentCalendar());
    }
    
    @Test
    @Transactional
    void testUpdateParentReminderMinutes() {
        String newReminders = "[60,15]";
        int updatedCount = syncSettingsRepository.updateParentReminderMinutes("user123", 1L, newReminders);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarSyncSettings> updated = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        assertTrue(updated.isPresent());
        assertEquals(newReminders, updated.get().getParentReminderMinutes());
    }
    
    @Test
    @Transactional
    void testUpdateStudentReminderMinutes() {
        String newReminders = "[30,10]";
        int updatedCount = syncSettingsRepository.updateStudentReminderMinutes("user123", 1L, newReminders);
        entityManager.flush();
        entityManager.clear();
        
        assertEquals(1, updatedCount);
        
        Optional<CalendarSyncSettings> updated = syncSettingsRepository
                .findByUserIdAndStudentId("user123", 1L);
        assertTrue(updated.isPresent());
        assertEquals(newReminders, updated.get().getStudentReminderMinutes());
    }
    
    @Test
    void testUniqueConstraint() {
        // Try to create duplicate settings with same user and student
        CalendarSyncSettings duplicate = new CalendarSyncSettings("user123", 1L);
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }
    
    @Test
    void testDefaultValues() {
        CalendarSyncSettings newSettings = new CalendarSyncSettings("newuser", 99L);
        entityManager.persistAndFlush(newSettings);
        
        Optional<CalendarSyncSettings> saved = syncSettingsRepository
                .findByUserIdAndStudentId("newuser", 99L);
        
        assertTrue(saved.isPresent());
        assertTrue(saved.get().getSyncEnabled());
        assertTrue(saved.get().getSyncToParentCalendar());
        assertTrue(saved.get().getSyncToStudentCalendar());
        assertEquals("[1440,120]", saved.get().getParentReminderMinutes());
        assertEquals("[120,30]", saved.get().getStudentReminderMinutes());
        assertFalse(saved.get().getSyncCompletedAssignments());
        assertTrue(saved.get().getAutoSyncEnabled());
    }
}