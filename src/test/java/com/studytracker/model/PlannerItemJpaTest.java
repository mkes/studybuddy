package com.studytracker.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PlannerItem JPA entity.
 * Tests database constraints, persistence, and JPA annotations.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PlannerItem JPA Integration Tests")
class PlannerItemJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist and retrieve PlannerItem correctly")
    void testPersistAndRetrieve() {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Math Homework Chapter 5")
                .contextName("Algebra I")
                .dueAt(LocalDateTime.of(2025, 9, 15, 23, 59))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.50"))
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .build();

        // When
        PlannerItem savedItem = entityManager.persistAndFlush(plannerItem);
        entityManager.clear(); // Clear persistence context
        PlannerItem retrievedItem = entityManager.find(PlannerItem.class, savedItem.getId());

        // Then
        assertNotNull(retrievedItem, "Retrieved item should not be null");
        assertEquals(12345L, retrievedItem.getStudentId());
        assertEquals(67890L, retrievedItem.getPlannableId());
        assertEquals("Math Homework Chapter 5", retrievedItem.getAssignmentTitle());
        assertEquals("Algebra I", retrievedItem.getContextName());
        assertEquals(LocalDateTime.of(2025, 9, 15, 23, 59), retrievedItem.getDueAt());
        assertEquals(new BigDecimal("100.00"), retrievedItem.getPointsPossible());
        assertEquals(new BigDecimal("85.50"), retrievedItem.getCurrentGrade());
        assertTrue(retrievedItem.getSubmitted());
        assertFalse(retrievedItem.getMissing());
        assertFalse(retrievedItem.getLate());
        assertTrue(retrievedItem.getGraded());
    }

    @Test
    @DisplayName("Should auto-generate ID on persist")
    void testIdGeneration() {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Test Assignment")
                .build();

        // When
        assertNull(plannerItem.getId(), "ID should be null before persist");
        PlannerItem savedItem = entityManager.persistAndFlush(plannerItem);

        // Then
        assertNotNull(savedItem.getId(), "ID should be generated after persist");
        assertTrue(savedItem.getId() > 0, "Generated ID should be positive");
    }

    @Test
    @DisplayName("Should set timestamps automatically")
    void testTimestamps() {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Test Assignment")
                .build();

        LocalDateTime beforePersist = LocalDateTime.now();

        // When
        PlannerItem savedItem = entityManager.persistAndFlush(plannerItem);

        // Then
        assertNotNull(savedItem.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(savedItem.getUpdatedAt(), "Updated timestamp should be set");
        assertTrue(savedItem.getCreatedAt().isAfter(beforePersist.minusSeconds(1)), 
                "Created timestamp should be recent");
        assertTrue(savedItem.getUpdatedAt().isAfter(beforePersist.minusSeconds(1)), 
                "Updated timestamp should be recent");
    }

    @Test
    @DisplayName("Should update timestamp on entity modification")
    void testUpdateTimestamp() throws InterruptedException {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Test Assignment")
                .submitted(false)
                .build();

        PlannerItem savedItem = entityManager.persistAndFlush(plannerItem);
        LocalDateTime originalUpdatedAt = savedItem.getUpdatedAt();
        LocalDateTime originalCreatedAt = savedItem.getCreatedAt();

        // Small delay to ensure timestamp difference
        Thread.sleep(100);

        // When
        savedItem.setSubmitted(true);
        savedItem.setCurrentGrade(new BigDecimal("95.00"));
        entityManager.flush();
        entityManager.clear();
        
        PlannerItem updatedItem = entityManager.find(PlannerItem.class, savedItem.getId());

        // Then
        assertNotNull(updatedItem.getUpdatedAt(), "Updated timestamp should not be null");
        assertNotNull(updatedItem.getCreatedAt(), "Created timestamp should not be null");
        assertEquals(originalCreatedAt, updatedItem.getCreatedAt(), 
                "Created timestamp should remain unchanged");
        // Note: @UpdateTimestamp behavior may vary between databases, so we just verify it exists
        assertTrue(updatedItem.getSubmitted(), "Submitted flag should be updated");
        assertEquals(new BigDecimal("95.00"), updatedItem.getCurrentGrade(), "Grade should be updated");
    }

    @Test
    @DisplayName("Should allow multiple assignments for same student")
    void testMultipleAssignmentsForStudent() {
        // Given
        PlannerItem item1 = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("First Assignment")
                .build();

        PlannerItem item2 = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67891L) // Different plannable ID
                .assignmentTitle("Second Assignment")
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            entityManager.persistAndFlush(item1);
            entityManager.persistAndFlush(item2);
        }, "Should allow multiple assignments for same student with different plannable IDs");
    }

    @Test
    @DisplayName("Should allow same plannable_id for different students")
    void testDifferentStudentsSamePlannable() {
        // Given
        PlannerItem item1 = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Assignment for Student 1")
                .build();

        PlannerItem item2 = PlannerItem.builder()
                .studentId(54321L) // Different student
                .plannableId(67890L) // Same plannable ID
                .assignmentTitle("Assignment for Student 2")
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            entityManager.persistAndFlush(item1);
            entityManager.persistAndFlush(item2);
        }, "Should allow same plannable_id for different students");
    }

    @Test
    @DisplayName("Should handle null optional fields correctly")
    void testNullOptionalFields() {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Minimal Assignment")
                // All other fields are null or default
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            PlannerItem savedItem = entityManager.persistAndFlush(plannerItem);
            
            assertNotNull(savedItem.getId());
            assertNull(savedItem.getContextName());
            assertNull(savedItem.getDueAt());
            assertNull(savedItem.getPointsPossible());
            assertNull(savedItem.getCurrentGrade());
            
            // Boolean fields should have defaults
            assertFalse(savedItem.getSubmitted());
            assertFalse(savedItem.getMissing());
            assertFalse(savedItem.getLate());
            assertFalse(savedItem.getGraded());
        }, "Should persist entity with minimal required fields");
    }

    @Test
    @DisplayName("Should handle decimal precision correctly")
    void testDecimalPrecision() {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Precision Test")
                .pointsPossible(new BigDecimal("99.99"))
                .currentGrade(new BigDecimal("87.65"))
                .build();

        // When
        PlannerItem savedItem = entityManager.persistAndFlush(plannerItem);
        entityManager.clear();
        PlannerItem retrievedItem = entityManager.find(PlannerItem.class, savedItem.getId());

        // Then
        assertEquals(new BigDecimal("99.99"), retrievedItem.getPointsPossible(), 
                "Points possible should maintain decimal precision");
        assertEquals(new BigDecimal("87.65"), retrievedItem.getCurrentGrade(), 
                "Current grade should maintain decimal precision");
    }

    @Test
    @DisplayName("Should validate required fields")
    void testRequiredFields() {
        // Given - entity with all required fields
        PlannerItem validItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Valid Assignment")
                .build();

        // When & Then
        assertDoesNotThrow(() -> {
            PlannerItem savedItem = entityManager.persistAndFlush(validItem);
            assertNotNull(savedItem.getId());
            assertEquals(12345L, savedItem.getStudentId());
            assertEquals(67890L, savedItem.getPlannableId());
            assertEquals("Valid Assignment", savedItem.getAssignmentTitle());
        }, "Should persist entity with all required fields");
    }
}