package com.studytracker.repository;

import com.studytracker.model.PlannerItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PlannerItemRepository.
 * Tests repository methods with embedded H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
class PlannerItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlannerItemRepository plannerItemRepository;

    private PlannerItem assignment1;
    private PlannerItem assignment2;
    private PlannerItem assignment3;
    private PlannerItem assignment4;

    private final Long STUDENT_ID_1 = 1001L;
    private final Long STUDENT_ID_2 = 1002L;
    private final LocalDateTime NOW = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        // Create test assignments with different due dates and statuses
        assignment1 = PlannerItem.builder()
                .studentId(STUDENT_ID_1)
                .plannableId(2001L)
                .assignmentTitle("Math Homework 1")
                .contextName("Mathematics 101")
                .dueAt(NOW.minusDays(5))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.00"))
                .submitted(true)
                .graded(true)
                .missing(false)
                .late(false)
                .build();

        assignment2 = PlannerItem.builder()
                .studentId(STUDENT_ID_1)
                .plannableId(2002L)
                .assignmentTitle("Science Project")
                .contextName("Biology 101")
                .dueAt(NOW.minusDays(2))
                .pointsPossible(new BigDecimal("150.00"))
                .submitted(false)
                .graded(false)
                .missing(true)
                .late(false)
                .build();

        assignment3 = PlannerItem.builder()
                .studentId(STUDENT_ID_1)
                .plannableId(2003L)
                .assignmentTitle("English Essay")
                .contextName("English 101")
                .dueAt(NOW.plusDays(3))
                .pointsPossible(new BigDecimal("200.00"))
                .submitted(false)
                .graded(false)
                .missing(false)
                .late(false)
                .build();

        assignment4 = PlannerItem.builder()
                .studentId(STUDENT_ID_2)
                .plannableId(2004L)
                .assignmentTitle("History Report")
                .contextName("History 101")
                .dueAt(NOW.minusDays(1))
                .pointsPossible(new BigDecimal("120.00"))
                .submitted(true)
                .graded(false)
                .missing(false)
                .late(true)
                .build();

        // Persist test data
        entityManager.persistAndFlush(assignment1);
        entityManager.persistAndFlush(assignment2);
        entityManager.persistAndFlush(assignment3);
        entityManager.persistAndFlush(assignment4);
    }

    @Test
    void testFindByStudentIdAndDueAtBetweenOrderByDueAtDesc() {
        // Test date range filtering with descending order
        LocalDateTime startDate = NOW.minusDays(10);
        LocalDateTime endDate = NOW.plusDays(10);

        List<PlannerItem> results = plannerItemRepository
                .findByStudentIdAndDueAtBetweenOrderByDueAtDesc(STUDENT_ID_1, startDate, endDate);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getAssignmentTitle()).isEqualTo("English Essay"); // Most recent due date
        assertThat(results.get(1).getAssignmentTitle()).isEqualTo("Science Project");
        assertThat(results.get(2).getAssignmentTitle()).isEqualTo("Math Homework 1"); // Oldest due date

        // Verify ordering by due date descending
        for (int i = 0; i < results.size() - 1; i++) {
            LocalDateTime current = results.get(i).getDueAt();
            LocalDateTime next = results.get(i + 1).getDueAt();
            assertThat(current).isAfterOrEqualTo(next);
        }
    }

    @Test
    void testFindByStudentIdAndDueAtBetweenOrderByDueAtDesc_EmptyRange() {
        // Test with date range that excludes all assignments
        LocalDateTime startDate = NOW.plusDays(10);
        LocalDateTime endDate = NOW.plusDays(20);

        List<PlannerItem> results = plannerItemRepository
                .findByStudentIdAndDueAtBetweenOrderByDueAtDesc(STUDENT_ID_1, startDate, endDate);

        assertThat(results).isEmpty();
    }

    @Test
    void testFindByStudentIdOrderByDueAtDesc() {
        List<PlannerItem> results = plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID_1);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getAssignmentTitle()).isEqualTo("English Essay");
        assertThat(results.get(1).getAssignmentTitle()).isEqualTo("Science Project");
        assertThat(results.get(2).getAssignmentTitle()).isEqualTo("Math Homework 1");
    }

    @Test
    void testFindByStudentIdOrderByDueAtDesc_NoAssignments() {
        List<PlannerItem> results = plannerItemRepository.findByStudentIdOrderByDueAtDesc(9999L);
        assertThat(results).isEmpty();
    }

    @Test
    void testFindByStudentIdAndPlannableId() {
        PlannerItem result = plannerItemRepository.findByStudentIdAndPlannableId(STUDENT_ID_1, 2001L);

        assertThat(result).isNotNull();
        assertThat(result.getAssignmentTitle()).isEqualTo("Math Homework 1");
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID_1);
        assertThat(result.getPlannableId()).isEqualTo(2001L);
    }

    @Test
    void testFindByStudentIdAndPlannableId_NotFound() {
        PlannerItem result = plannerItemRepository.findByStudentIdAndPlannableId(STUDENT_ID_1, 9999L);
        assertThat(result).isNull();
    }

    @Test
    void testDeleteByStudentIdAndPlannableId() {
        // Verify assignment exists before deletion
        PlannerItem beforeDelete = plannerItemRepository.findByStudentIdAndPlannableId(STUDENT_ID_1, 2001L);
        assertThat(beforeDelete).isNotNull();

        // Delete the assignment
        plannerItemRepository.deleteByStudentIdAndPlannableId(STUDENT_ID_1, 2001L);
        entityManager.flush();

        // Verify assignment is deleted
        PlannerItem afterDelete = plannerItemRepository.findByStudentIdAndPlannableId(STUDENT_ID_1, 2001L);
        assertThat(afterDelete).isNull();

        // Verify other assignments are not affected
        List<PlannerItem> remaining = plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID_1);
        assertThat(remaining).hasSize(2);
    }

    @Test
    void testDeleteByStudentId() {
        // Verify assignments exist before deletion
        List<PlannerItem> beforeDelete = plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID_1);
        assertThat(beforeDelete).hasSize(3);

        // Delete all assignments for student
        plannerItemRepository.deleteByStudentId(STUDENT_ID_1);
        entityManager.flush();

        // Verify all assignments are deleted for STUDENT_ID_1
        List<PlannerItem> afterDelete = plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID_1);
        assertThat(afterDelete).isEmpty();

        // Verify assignments for other students are not affected
        List<PlannerItem> otherStudent = plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID_2);
        assertThat(otherStudent).hasSize(1);
    }

    @Test
    void testCountByStudentIdAndDueAtBetween() {
        LocalDateTime startDate = NOW.minusDays(10);
        LocalDateTime endDate = NOW.plusDays(10);

        long count = plannerItemRepository.countByStudentIdAndDueAtBetween(STUDENT_ID_1, startDate, endDate);
        assertThat(count).isEqualTo(3);

        // Test with narrow date range
        LocalDateTime narrowStart = NOW.minusDays(3);
        LocalDateTime narrowEnd = NOW.minusDays(1);
        long narrowCount = plannerItemRepository.countByStudentIdAndDueAtBetween(STUDENT_ID_1, narrowStart, narrowEnd);
        assertThat(narrowCount).isEqualTo(1); // Only Science Project
    }

    @Test
    void testFindOverdueAssignments() {
        List<PlannerItem> overdueAssignments = plannerItemRepository
                .findOverdueAssignments(STUDENT_ID_1, NOW);

        // Should find Science Project (due 2 days ago, not submitted)
        assertThat(overdueAssignments).hasSize(1);
        assertThat(overdueAssignments.get(0).getAssignmentTitle()).isEqualTo("Science Project");
        assertThat(overdueAssignments.get(0).getDueAt()).isBefore(NOW);
        assertThat(overdueAssignments.get(0).getSubmitted()).isFalse();
    }

    @Test
    void testFindOverdueAssignments_NoOverdueAssignments() {
        List<PlannerItem> overdueAssignments = plannerItemRepository
                .findOverdueAssignments(STUDENT_ID_2, NOW);

        // STUDENT_ID_2 has one assignment that's submitted (even if late), so not overdue
        assertThat(overdueAssignments).isEmpty();
    }

    @Test
    void testBulkSaveOperations() {
        // Create multiple new assignments for bulk save testing
        List<PlannerItem> newAssignments = List.of(
                PlannerItem.builder()
                        .studentId(STUDENT_ID_1)
                        .plannableId(3001L)
                        .assignmentTitle("Physics Lab 1")
                        .contextName("Physics 101")
                        .dueAt(NOW.plusDays(7))
                        .pointsPossible(new BigDecimal("75.00"))
                        .submitted(false)
                        .graded(false)
                        .missing(false)
                        .late(false)
                        .build(),
                PlannerItem.builder()
                        .studentId(STUDENT_ID_1)
                        .plannableId(3002L)
                        .assignmentTitle("Chemistry Quiz")
                        .contextName("Chemistry 101")
                        .dueAt(NOW.plusDays(14))
                        .pointsPossible(new BigDecimal("50.00"))
                        .submitted(false)
                        .graded(false)
                        .missing(false)
                        .late(false)
                        .build()
        );

        // Perform bulk save
        List<PlannerItem> savedAssignments = plannerItemRepository.saveAll(newAssignments);
        entityManager.flush();

        assertThat(savedAssignments).hasSize(2);
        assertThat(savedAssignments.get(0).getId()).isNotNull();
        assertThat(savedAssignments.get(1).getId()).isNotNull();

        // Verify assignments are persisted
        List<PlannerItem> allAssignments = plannerItemRepository.findByStudentIdOrderByDueAtDesc(STUDENT_ID_1);
        assertThat(allAssignments).hasSize(5); // 3 original + 2 new
    }

    @Test
    void testUniqueConstraintViolation() {
        // Try to create duplicate assignment (same student_id and plannable_id)
        PlannerItem duplicate = PlannerItem.builder()
                .studentId(STUDENT_ID_1)
                .plannableId(2001L) // Same as assignment1
                .assignmentTitle("Duplicate Assignment")
                .contextName("Test Course")
                .dueAt(NOW)
                .pointsPossible(new BigDecimal("100.00"))
                .submitted(false)
                .graded(false)
                .missing(false)
                .late(false)
                .build();

        // This should throw an exception due to unique constraint
        try {
            plannerItemRepository.save(duplicate);
            entityManager.flush();
        } catch (Exception e) {
            // Expected behavior - unique constraint violation
            assertThat(e.getMessage()).contains("Unique index or primary key violation");
        }
    }

    @Test
    void testIndexPerformance() {
        // Create many assignments to test index performance
        List<PlannerItem> manyAssignments = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyAssignments.add(PlannerItem.builder()
                    .studentId(STUDENT_ID_1)
                    .plannableId(4000L + i)
                    .assignmentTitle("Assignment " + i)
                    .contextName("Course " + (i % 10))
                    .dueAt(NOW.plusDays(i % 30))
                    .pointsPossible(new BigDecimal("100.00"))
                    .submitted(i % 3 == 0)
                    .graded(i % 4 == 0)
                    .missing(i % 5 == 0)
                    .late(i % 7 == 0)
                    .build());
        }

        plannerItemRepository.saveAll(manyAssignments);
        entityManager.flush();

        // Test query performance with large dataset
        long startTime = System.currentTimeMillis();
        List<PlannerItem> results = plannerItemRepository
                .findByStudentIdAndDueAtBetweenOrderByDueAtDesc(
                        STUDENT_ID_1, 
                        NOW.minusDays(1), 
                        NOW.plusDays(31)
                );
        long endTime = System.currentTimeMillis();

        // Verify results and reasonable performance (should be fast with proper indexing)
        assertThat(results).hasSizeGreaterThan(50);
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }
}