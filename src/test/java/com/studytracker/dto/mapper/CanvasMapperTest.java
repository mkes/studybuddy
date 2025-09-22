package com.studytracker.dto.mapper;

import com.studytracker.dto.AssignmentDto;
import com.studytracker.model.PlannerItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CanvasMapper class.
 * Tests mapping between Canvas API DTOs and internal domain models.
 */
class CanvasMapperTest {

    private CanvasMapper canvasMapper;

    @BeforeEach
    void setUp() {
        canvasMapper = new CanvasMapper();
    }

    @Test
    void testToPlannerItem_WithCompleteData() {
        // Given
        Long studentId = 12345L;
        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .score(new BigDecimal("85.50"))
                .grade("B+")
                .workflowState("graded")
                .build();

        AssignmentDto assignmentDto = AssignmentDto.builder()
                .plannableId(98765L)
                .assignmentTitle("Math Homework Chapter 5")
                .contextName("Algebra I")
                .dueAt(LocalDateTime.of(2025, 9, 15, 23, 59))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.50"))
                .submission(submission)
                .build();

        // When
        PlannerItem plannerItem = canvasMapper.toPlannerItem(assignmentDto, studentId);

        // Then
        assertThat(plannerItem).isNotNull();
        assertThat(plannerItem.getStudentId()).isEqualTo(studentId);
        assertThat(plannerItem.getPlannableId()).isEqualTo(98765L);
        assertThat(plannerItem.getAssignmentTitle()).isEqualTo("Math Homework Chapter 5");
        assertThat(plannerItem.getContextName()).isEqualTo("Algebra I");
        assertThat(plannerItem.getDueAt()).isEqualTo(LocalDateTime.of(2025, 9, 15, 23, 59));
        assertThat(plannerItem.getPointsPossible()).isEqualTo(new BigDecimal("100.00"));
        assertThat(plannerItem.getCurrentGrade()).isEqualTo(new BigDecimal("85.50"));
        assertThat(plannerItem.getSubmitted()).isTrue();
        assertThat(plannerItem.getMissing()).isFalse();
        assertThat(plannerItem.getLate()).isFalse();
        assertThat(plannerItem.getGraded()).isTrue();
    }

    @Test
    void testToPlannerItem_WithNullSubmission() {
        // Given
        Long studentId = 12345L;
        AssignmentDto assignmentDto = AssignmentDto.builder()
                .plannableId(98765L)
                .assignmentTitle("Math Homework Chapter 5")
                .contextName("Algebra I")
                .dueAt(LocalDateTime.of(2025, 9, 15, 23, 59))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.50"))
                .submission(null)
                .build();

        // When
        PlannerItem plannerItem = canvasMapper.toPlannerItem(assignmentDto, studentId);

        // Then
        assertThat(plannerItem).isNotNull();
        assertThat(plannerItem.getStudentId()).isEqualTo(studentId);
        assertThat(plannerItem.getPlannableId()).isEqualTo(98765L);
        assertThat(plannerItem.getAssignmentTitle()).isEqualTo("Math Homework Chapter 5");
        assertThat(plannerItem.getSubmitted()).isFalse();
        assertThat(plannerItem.getMissing()).isFalse();
        assertThat(plannerItem.getLate()).isFalse();
        assertThat(plannerItem.getGraded()).isFalse();
    }

    @Test
    void testToPlannerItem_WithSubmissionScoreOverride() {
        // Given
        Long studentId = 12345L;
        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .score(new BigDecimal("92.00"))
                .build();

        AssignmentDto assignmentDto = AssignmentDto.builder()
                .plannableId(98765L)
                .assignmentTitle("Math Homework Chapter 5")
                .contextName("Algebra I")
                .currentGrade(null) // No current grade, should use submission score
                .submission(submission)
                .build();

        // When
        PlannerItem plannerItem = canvasMapper.toPlannerItem(assignmentDto, studentId);

        // Then
        assertThat(plannerItem).isNotNull();
        assertThat(plannerItem.getCurrentGrade()).isEqualTo(new BigDecimal("92.00"));
    }

    @Test
    void testToPlannerItem_WithNullDto() {
        // Given
        Long studentId = 12345L;

        // When
        PlannerItem plannerItem = canvasMapper.toPlannerItem(null, studentId);

        // Then
        assertThat(plannerItem).isNull();
    }

    @Test
    void testToPlannerItems_WithMultipleAssignments() {
        // Given
        Long studentId = 12345L;
        AssignmentDto assignment1 = AssignmentDto.builder()
                .plannableId(98765L)
                .assignmentTitle("Math Homework")
                .contextName("Algebra I")
                .build();

        AssignmentDto assignment2 = AssignmentDto.builder()
                .plannableId(98766L)
                .assignmentTitle("Science Lab")
                .contextName("Biology")
                .build();

        List<AssignmentDto> assignmentDtos = Arrays.asList(assignment1, assignment2);

        // When
        List<PlannerItem> plannerItems = canvasMapper.toPlannerItems(assignmentDtos, studentId);

        // Then
        assertThat(plannerItems).hasSize(2);
        assertThat(plannerItems.get(0).getPlannableId()).isEqualTo(98765L);
        assertThat(plannerItems.get(0).getAssignmentTitle()).isEqualTo("Math Homework");
        assertThat(plannerItems.get(1).getPlannableId()).isEqualTo(98766L);
        assertThat(plannerItems.get(1).getAssignmentTitle()).isEqualTo("Science Lab");
    }

    @Test
    void testToPlannerItems_WithNullList() {
        // Given
        Long studentId = 12345L;

        // When
        List<PlannerItem> plannerItems = canvasMapper.toPlannerItems(null, studentId);

        // Then
        assertThat(plannerItems).isEmpty();
    }

    @Test
    void testToAssignmentDto_WithCompleteData() {
        // Given
        PlannerItem plannerItem = PlannerItem.builder()
                .id(1L)
                .studentId(12345L)
                .plannableId(98765L)
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
        AssignmentDto assignmentDto = canvasMapper.toAssignmentDto(plannerItem);

        // Then
        assertThat(assignmentDto).isNotNull();
        assertThat(assignmentDto.getPlannableId()).isEqualTo(98765L);
        assertThat(assignmentDto.getStudentId()).isEqualTo(12345L);
        assertThat(assignmentDto.getAssignmentTitle()).isEqualTo("Math Homework Chapter 5");
        assertThat(assignmentDto.getContextName()).isEqualTo("Algebra I");
        assertThat(assignmentDto.getDueAt()).isEqualTo(LocalDateTime.of(2025, 9, 15, 23, 59));
        assertThat(assignmentDto.getPointsPossible()).isEqualTo(new BigDecimal("100.00"));
        assertThat(assignmentDto.getCurrentGrade()).isEqualTo(new BigDecimal("85.50"));
        
        assertThat(assignmentDto.getSubmission()).isNotNull();
        assertThat(assignmentDto.getSubmission().getSubmitted()).isTrue();
        assertThat(assignmentDto.getSubmission().getMissing()).isFalse();
        assertThat(assignmentDto.getSubmission().getLate()).isFalse();
        assertThat(assignmentDto.getSubmission().getGraded()).isTrue();
        assertThat(assignmentDto.getSubmission().getScore()).isEqualTo(new BigDecimal("85.50"));
    }

    @Test
    void testToAssignmentDto_WithNullPlannerItem() {
        // When
        AssignmentDto assignmentDto = canvasMapper.toAssignmentDto(null);

        // Then
        assertThat(assignmentDto).isNull();
    }

    @Test
    void testToAssignmentDtos_WithMultiplePlannerItems() {
        // Given
        PlannerItem item1 = PlannerItem.builder()
                .id(1L)
                .studentId(12345L)
                .plannableId(98765L)
                .assignmentTitle("Math Homework")
                .contextName("Algebra I")
                .build();

        PlannerItem item2 = PlannerItem.builder()
                .id(2L)
                .studentId(12345L)
                .plannableId(98766L)
                .assignmentTitle("Science Lab")
                .contextName("Biology")
                .build();

        List<PlannerItem> plannerItems = Arrays.asList(item1, item2);

        // When
        List<AssignmentDto> assignmentDtos = canvasMapper.toAssignmentDtos(plannerItems);

        // Then
        assertThat(assignmentDtos).hasSize(2);
        assertThat(assignmentDtos.get(0).getPlannableId()).isEqualTo(98765L);
        assertThat(assignmentDtos.get(0).getAssignmentTitle()).isEqualTo("Math Homework");
        assertThat(assignmentDtos.get(1).getPlannableId()).isEqualTo(98766L);
        assertThat(assignmentDtos.get(1).getAssignmentTitle()).isEqualTo("Science Lab");
    }

    @Test
    void testUpdatePlannerItem_WithNewData() {
        // Given
        PlannerItem existingItem = PlannerItem.builder()
                .id(1L)
                .studentId(12345L)
                .plannableId(98765L)
                .assignmentTitle("Old Title")
                .contextName("Old Context")
                .currentGrade(new BigDecimal("70.00"))
                .submitted(false)
                .graded(false)
                .build();

        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .score(new BigDecimal("85.50"))
                .build();

        AssignmentDto assignmentDto = AssignmentDto.builder()
                .plannableId(98765L)
                .assignmentTitle("Updated Title")
                .contextName("Updated Context")
                .dueAt(LocalDateTime.of(2025, 9, 15, 23, 59))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.50"))
                .submission(submission)
                .build();

        // When
        PlannerItem updatedItem = canvasMapper.updatePlannerItem(existingItem, assignmentDto);

        // Then
        assertThat(updatedItem).isSameAs(existingItem); // Should update in place
        assertThat(updatedItem.getId()).isEqualTo(1L); // ID should be preserved
        assertThat(updatedItem.getStudentId()).isEqualTo(12345L); // Student ID should be preserved
        assertThat(updatedItem.getPlannableId()).isEqualTo(98765L); // Plannable ID should be preserved
        assertThat(updatedItem.getAssignmentTitle()).isEqualTo("Updated Title");
        assertThat(updatedItem.getContextName()).isEqualTo("Updated Context");
        assertThat(updatedItem.getDueAt()).isEqualTo(LocalDateTime.of(2025, 9, 15, 23, 59));
        assertThat(updatedItem.getPointsPossible()).isEqualTo(new BigDecimal("100.00"));
        assertThat(updatedItem.getCurrentGrade()).isEqualTo(new BigDecimal("85.50"));
        assertThat(updatedItem.getSubmitted()).isTrue();
        assertThat(updatedItem.getGraded()).isTrue();
    }

    @Test
    void testUpdatePlannerItem_WithNullValues() {
        // Given
        PlannerItem existingItem = PlannerItem.builder()
                .id(1L)
                .studentId(12345L)
                .plannableId(98765L)
                .assignmentTitle("Original Title")
                .build();

        // When - null assignment DTO
        PlannerItem result1 = canvasMapper.updatePlannerItem(existingItem, null);
        
        // When - null existing item
        PlannerItem result2 = canvasMapper.updatePlannerItem(null, new AssignmentDto());

        // Then
        assertThat(result1).isSameAs(existingItem);
        assertThat(result2).isNull();
    }

    @Test
    void testUpdatePlannerItem_WithSubmissionScoreOverride() {
        // Given
        PlannerItem existingItem = PlannerItem.builder()
                .id(1L)
                .studentId(12345L)
                .plannableId(98765L)
                .assignmentTitle("Assignment")
                .currentGrade(new BigDecimal("70.00"))
                .build();

        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(true)
                .graded(true)
                .score(new BigDecimal("92.00"))
                .build();

        AssignmentDto assignmentDto = AssignmentDto.builder()
                .plannableId(98765L)
                .assignmentTitle("Assignment")
                .currentGrade(null) // No current grade, should use submission score
                .submission(submission)
                .build();

        // When
        PlannerItem updatedItem = canvasMapper.updatePlannerItem(existingItem, assignmentDto);

        // Then
        assertThat(updatedItem.getCurrentGrade()).isEqualTo(new BigDecimal("92.00"));
    }
}