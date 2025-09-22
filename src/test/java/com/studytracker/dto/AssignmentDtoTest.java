package com.studytracker.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AssignmentDto class.
 * Tests JSON serialization/deserialization and nested DTO handling.
 */
class AssignmentDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testAssignmentDtoCreation() {
        // Given
        Long plannableId = 98765L;
        Long studentId = 12345L;
        String assignmentTitle = "Math Homework Chapter 5";
        String contextName = "Algebra I";
        LocalDateTime dueAt = LocalDateTime.of(2025, 9, 15, 23, 59);
        BigDecimal pointsPossible = new BigDecimal("100.00");
        BigDecimal currentGrade = new BigDecimal("85.50");

        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .score(new BigDecimal("85.50"))
                .grade("B+")
                .workflowState("graded")
                .build();

        // When
        AssignmentDto assignment = AssignmentDto.builder()
                .plannableId(plannableId)
                .studentId(studentId)
                .assignmentTitle(assignmentTitle)
                .contextName(contextName)
                .dueAt(dueAt)
                .pointsPossible(pointsPossible)
                .currentGrade(currentGrade)
                .submission(submission)
                .build();

        // Then
        assertThat(assignment.getPlannableId()).isEqualTo(plannableId);
        assertThat(assignment.getStudentId()).isEqualTo(studentId);
        assertThat(assignment.getAssignmentTitle()).isEqualTo(assignmentTitle);
        assertThat(assignment.getContextName()).isEqualTo(contextName);
        assertThat(assignment.getDueAt()).isEqualTo(dueAt);
        assertThat(assignment.getPointsPossible()).isEqualTo(pointsPossible);
        assertThat(assignment.getCurrentGrade()).isEqualTo(currentGrade);
        assertThat(assignment.getSubmission()).isNotNull();
        assertThat(assignment.getSubmission().getSubmitted()).isTrue();
        assertThat(assignment.getSubmission().getGraded()).isTrue();
    }

    @Test
    void testSubmissionDtoCreation() {
        // Given & When
        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(true)
                .missing(false)
                .late(true)
                .graded(true)
                .score(new BigDecimal("75.00"))
                .grade("C+")
                .workflowState("graded")
                .build();

        // Then
        assertThat(submission.getSubmitted()).isTrue();
        assertThat(submission.getMissing()).isFalse();
        assertThat(submission.getLate()).isTrue();
        assertThat(submission.getGraded()).isTrue();
        assertThat(submission.getScore()).isEqualTo(new BigDecimal("75.00"));
        assertThat(submission.getGrade()).isEqualTo("C+");
        assertThat(submission.getWorkflowState()).isEqualTo("graded");
    }

    @Test
    void testJsonDeserialization() throws Exception {
        // Given
        String json = """
                {
                    "plannable_id": 98765,
                    "user_id": 12345,
                    "plannable_title": "Math Homework Chapter 5",
                    "context_name": "Algebra I",
                    "plannable_date": "2025-09-15T23:59:00",
                    "points_possible": 100.00,
                    "current_grade": 85.50,
                    "submissions": {
                        "submitted": true,
                        "missing": false,
                        "late": false,
                        "graded": true,
                        "score": 85.50,
                        "grade": "B+",
                        "workflow_state": "graded"
                    }
                }
                """;

        // When
        AssignmentDto assignment = objectMapper.readValue(json, AssignmentDto.class);

        // Then
        assertThat(assignment.getPlannableId()).isEqualTo(98765L);
        assertThat(assignment.getStudentId()).isEqualTo(12345L);
        assertThat(assignment.getAssignmentTitle()).isEqualTo("Math Homework Chapter 5");
        assertThat(assignment.getContextName()).isEqualTo("Algebra I");
        assertThat(assignment.getDueAt()).isEqualTo(LocalDateTime.of(2025, 9, 15, 23, 59));
        assertThat(assignment.getPointsPossible()).isEqualTo(new BigDecimal("100.00"));
        assertThat(assignment.getCurrentGrade()).isEqualTo(new BigDecimal("85.50"));
        
        assertThat(assignment.getSubmission()).isNotNull();
        assertThat(assignment.getSubmission().getSubmitted()).isTrue();
        assertThat(assignment.getSubmission().getMissing()).isFalse();
        assertThat(assignment.getSubmission().getLate()).isFalse();
        assertThat(assignment.getSubmission().getGraded()).isTrue();
        assertThat(assignment.getSubmission().getScore()).isEqualTo(new BigDecimal("85.50"));
        assertThat(assignment.getSubmission().getGrade()).isEqualTo("B+");
        assertThat(assignment.getSubmission().getWorkflowState()).isEqualTo("graded");
    }

    @Test
    void testJsonDeserializationWithNullSubmission() throws Exception {
        // Given
        String json = """
                {
                    "plannable_id": 98765,
                    "user_id": 12345,
                    "plannable_title": "Math Homework Chapter 5",
                    "context_name": "Algebra I",
                    "plannable_date": "2025-09-15T23:59:00",
                    "points_possible": 100.00
                }
                """;

        // When
        AssignmentDto assignment = objectMapper.readValue(json, AssignmentDto.class);

        // Then
        assertThat(assignment.getPlannableId()).isEqualTo(98765L);
        assertThat(assignment.getStudentId()).isEqualTo(12345L);
        assertThat(assignment.getAssignmentTitle()).isEqualTo("Math Homework Chapter 5");
        assertThat(assignment.getContextName()).isEqualTo("Algebra I");
        assertThat(assignment.getDueAt()).isEqualTo(LocalDateTime.of(2025, 9, 15, 23, 59));
        assertThat(assignment.getPointsPossible()).isEqualTo(new BigDecimal("100.00"));
        assertThat(assignment.getCurrentGrade()).isNull();
        assertThat(assignment.getSubmission()).isNull();
    }

    @Test
    void testAssignmentDtoWithMinimalData() {
        // Given & When
        AssignmentDto assignment = AssignmentDto.builder()
                .plannableId(98765L)
                .studentId(12345L)
                .assignmentTitle("Basic Assignment")
                .build();

        // Then
        assertThat(assignment.getPlannableId()).isEqualTo(98765L);
        assertThat(assignment.getStudentId()).isEqualTo(12345L);
        assertThat(assignment.getAssignmentTitle()).isEqualTo("Basic Assignment");
        assertThat(assignment.getContextName()).isNull();
        assertThat(assignment.getDueAt()).isNull();
        assertThat(assignment.getPointsPossible()).isNull();
        assertThat(assignment.getCurrentGrade()).isNull();
        assertThat(assignment.getSubmission()).isNull();
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        AssignmentDto assignment1 = AssignmentDto.builder()
                .plannableId(98765L)
                .studentId(12345L)
                .assignmentTitle("Math Homework")
                .contextName("Algebra I")
                .build();

        AssignmentDto assignment2 = AssignmentDto.builder()
                .plannableId(98765L)
                .studentId(12345L)
                .assignmentTitle("Math Homework")
                .contextName("Algebra I")
                .build();

        AssignmentDto assignment3 = AssignmentDto.builder()
                .plannableId(11111L)
                .studentId(12345L)
                .assignmentTitle("Science Lab")
                .contextName("Biology")
                .build();

        // Then
        assertThat(assignment1).isEqualTo(assignment2);
        assertThat(assignment1.hashCode()).isEqualTo(assignment2.hashCode());
        assertThat(assignment1).isNotEqualTo(assignment3);
    }
}