package com.studytracker.service;

import com.studytracker.dto.AssignmentDto;
import com.studytracker.dto.mapper.CanvasMapper;
import com.studytracker.model.PlannerItem;
import com.studytracker.repository.PlannerItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssignmentService business logic and status calculations.
 * Tests assignment synchronization, status calculation, and date filtering functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Tests")
class AssignmentServiceTest {

    @Mock
    private CanvasApiService canvasApiService;

    @Mock
    private PlannerItemRepository plannerItemRepository;

    @Mock
    private CanvasMapper canvasMapper;

    @InjectMocks
    private AssignmentService assignmentService;

    private Long studentId;
    private String token;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @BeforeEach
    void setUp() {
        studentId = 12345L;
        token = "test-canvas-token";
        startDate = LocalDate.of(2025, 8, 1);
        endDate = LocalDate.of(2025, 9, 30);
        startDateTime = startDate.atStartOfDay();
        endDateTime = endDate.atTime(LocalTime.MAX);
    }

    @Test
    @DisplayName("Should sync assignments successfully with default date range")
    void syncAssignments_WithDefaultDateRange_ShouldSyncSuccessfully() {
        // Given
        List<AssignmentDto> assignmentDtos = createMockAssignmentDtos();
        List<PlannerItem> newAssignments = createMockPlannerItems();
        List<PlannerItem> existingAssignments = List.of();
        List<PlannerItem> savedAssignments = createMockPlannerItems();

        when(canvasApiService.getStudentAssignments(eq(token), eq(studentId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(assignmentDtos);
        when(canvasMapper.toPlannerItems(assignmentDtos, studentId)).thenReturn(newAssignments);
        when(plannerItemRepository.findByStudentIdAndDueAtBetweenOrderByDueAtDesc(eq(studentId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(existingAssignments);
        when(plannerItemRepository.saveAll(anyList())).thenReturn(savedAssignments);

        // When
        List<PlannerItem> result = assignmentService.syncAssignments(studentId, token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(canvasApiService).getStudentAssignments(eq(token), eq(studentId), any(LocalDate.class), any(LocalDate.class));
        verify(canvasMapper).toPlannerItems(assignmentDtos, studentId);
        verify(plannerItemRepository).findByStudentIdAndDueAtBetweenOrderByDueAtDesc(eq(studentId), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(plannerItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should sync assignments with custom date range")
    void syncAssignments_WithCustomDateRange_ShouldSyncSuccessfully() {
        // Given
        List<AssignmentDto> assignmentDtos = createMockAssignmentDtos();
        List<PlannerItem> newAssignments = createMockPlannerItems();
        List<PlannerItem> existingAssignments = List.of();
        List<PlannerItem> savedAssignments = createMockPlannerItems();

        when(canvasApiService.getStudentAssignments(token, studentId, startDate, endDate))
                .thenReturn(assignmentDtos);
        when(canvasMapper.toPlannerItems(assignmentDtos, studentId)).thenReturn(newAssignments);
        when(plannerItemRepository.findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime))
                .thenReturn(existingAssignments);
        when(plannerItemRepository.saveAll(anyList())).thenReturn(savedAssignments);

        // When
        List<PlannerItem> result = assignmentService.syncAssignments(studentId, token, startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(canvasApiService).getStudentAssignments(token, studentId, startDate, endDate);
        verify(canvasMapper).toPlannerItems(assignmentDtos, studentId);
        verify(plannerItemRepository).findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime);
        verify(plannerItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should update existing assignments during sync")
    void syncAssignments_WithExistingAssignments_ShouldUpdateExisting() {
        // Given
        List<AssignmentDto> assignmentDtos = createMockAssignmentDtos();
        List<PlannerItem> newAssignments = createMockPlannerItems();
        List<PlannerItem> existingAssignments = createExistingPlannerItems();
        List<PlannerItem> savedAssignments = createMockPlannerItems();

        when(canvasApiService.getStudentAssignments(token, studentId, startDate, endDate))
                .thenReturn(assignmentDtos);
        when(canvasMapper.toPlannerItems(assignmentDtos, studentId)).thenReturn(newAssignments);
        when(plannerItemRepository.findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime))
                .thenReturn(existingAssignments);
        when(plannerItemRepository.saveAll(anyList())).thenReturn(savedAssignments);

        // When
        List<PlannerItem> result = assignmentService.syncAssignments(studentId, token, startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        verify(plannerItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle sync failure gracefully")
    void syncAssignments_WhenCanvasApiThrowsException_ShouldThrowRuntimeException() {
        // Given
        when(canvasApiService.getStudentAssignments(token, studentId, startDate, endDate))
                .thenThrow(new RuntimeException("Canvas API error"));

        // When & Then
        assertThatThrownBy(() -> assignmentService.syncAssignments(studentId, token, startDate, endDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Assignment synchronization failed");
    }

    @Test
    @DisplayName("Should get assignments by student with default date range")
    void getAssignmentsByStudent_WithDefaultDateRange_ShouldReturnAssignments() {
        // Given
        List<PlannerItem> assignments = createMockPlannerItems();
        when(plannerItemRepository.findByStudentIdAndDueAtBetweenOrderByDueAtDesc(eq(studentId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(assignments);

        // When
        List<PlannerItem> result = assignmentService.getAssignmentsByStudent(studentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(plannerItemRepository).findByStudentIdAndDueAtBetweenOrderByDueAtDesc(eq(studentId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should get assignments by student with custom date range")
    void getAssignmentsByStudent_WithCustomDateRange_ShouldReturnAssignments() {
        // Given
        List<PlannerItem> assignments = createMockPlannerItems();
        when(plannerItemRepository.findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime))
                .thenReturn(assignments);

        // When
        List<PlannerItem> result = assignmentService.getAssignmentsByStudent(studentId, startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(plannerItemRepository).findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime);
    }

    @Test
    @DisplayName("Should calculate status as 'submitted' for submitted and graded assignment")
    void calculateAssignmentStatus_SubmittedAndGraded_ShouldReturnSubmitted() {
        // Given
        PlannerItem assignment = PlannerItem.builder()
                .submitted(true)
                .graded(true)
                .missing(false)
                .late(false)
                .dueAt(LocalDateTime.now().plusDays(1))
                .build();

        // When
        String status = assignmentService.calculateAssignmentStatus(assignment);

        // Then
        assertThat(status).isEqualTo("submitted");
    }

    @Test
    @DisplayName("Should calculate status as 'missing' for missing assignment")
    void calculateAssignmentStatus_Missing_ShouldReturnMissing() {
        // Given
        PlannerItem assignment = PlannerItem.builder()
                .submitted(false)
                .graded(false)
                .missing(true)
                .late(false)
                .dueAt(LocalDateTime.now().plusDays(1))
                .build();

        // When
        String status = assignmentService.calculateAssignmentStatus(assignment);

        // Then
        assertThat(status).isEqualTo("missing");
    }

    @Test
    @DisplayName("Should calculate status as 'late' for late assignment")
    void calculateAssignmentStatus_Late_ShouldReturnLate() {
        // Given
        PlannerItem assignment = PlannerItem.builder()
                .submitted(true)
                .graded(false)
                .missing(false)
                .late(true)
                .dueAt(LocalDateTime.now().minusDays(1))
                .build();

        // When
        String status = assignmentService.calculateAssignmentStatus(assignment);

        // Then
        assertThat(status).isEqualTo("late");
    }

    @Test
    @DisplayName("Should calculate status as 'overdue' for past due unsubmitted assignment")
    void calculateAssignmentStatus_PastDueNotSubmitted_ShouldReturnOverdue() {
        // Given
        PlannerItem assignment = PlannerItem.builder()
                .submitted(false)
                .graded(false)
                .missing(false)
                .late(false)
                .dueAt(LocalDateTime.now().minusDays(1))
                .build();

        // When
        String status = assignmentService.calculateAssignmentStatus(assignment);

        // Then
        assertThat(status).isEqualTo("overdue");
    }

    @Test
    @DisplayName("Should calculate status as 'pending' for future assignment")
    void calculateAssignmentStatus_FutureAssignment_ShouldReturnPending() {
        // Given
        PlannerItem assignment = PlannerItem.builder()
                .submitted(false)
                .graded(false)
                .missing(false)
                .late(false)
                .dueAt(LocalDateTime.now().plusDays(1))
                .build();

        // When
        String status = assignmentService.calculateAssignmentStatus(assignment);

        // Then
        assertThat(status).isEqualTo("pending");
    }

    @Test
    @DisplayName("Should calculate status as 'pending' for null assignment")
    void calculateAssignmentStatus_NullAssignment_ShouldReturnPending() {
        // When
        String status = assignmentService.calculateAssignmentStatus(null);

        // Then
        assertThat(status).isEqualTo("pending");
    }

    @Test
    @DisplayName("Should return correct badge class for submitted status")
    void getStatusBadgeClass_SubmittedStatus_ShouldReturnSuccessBadge() {
        // When
        String badgeClass = assignmentService.getStatusBadgeClass("submitted");

        // Then
        assertThat(badgeClass).isEqualTo("badge-success");
    }

    @Test
    @DisplayName("Should return correct badge class for missing status")
    void getStatusBadgeClass_MissingStatus_ShouldReturnDangerBadge() {
        // When
        String badgeClass = assignmentService.getStatusBadgeClass("missing");

        // Then
        assertThat(badgeClass).isEqualTo("badge-danger");
    }

    @Test
    @DisplayName("Should return correct badge class for overdue status")
    void getStatusBadgeClass_OverdueStatus_ShouldReturnDangerBadge() {
        // When
        String badgeClass = assignmentService.getStatusBadgeClass("overdue");

        // Then
        assertThat(badgeClass).isEqualTo("badge-danger");
    }

    @Test
    @DisplayName("Should return correct badge class for late status")
    void getStatusBadgeClass_LateStatus_ShouldReturnWarningBadge() {
        // When
        String badgeClass = assignmentService.getStatusBadgeClass("late");

        // Then
        assertThat(badgeClass).isEqualTo("badge-warning");
    }

    @Test
    @DisplayName("Should return correct badge class for pending status")
    void getStatusBadgeClass_PendingStatus_ShouldReturnPrimaryBadge() {
        // When
        String badgeClass = assignmentService.getStatusBadgeClass("pending");

        // Then
        assertThat(badgeClass).isEqualTo("badge-primary");
    }

    @Test
    @DisplayName("Should return secondary badge class for unknown status")
    void getStatusBadgeClass_UnknownStatus_ShouldReturnSecondaryBadge() {
        // When
        String badgeClass = assignmentService.getStatusBadgeClass("unknown");

        // Then
        assertThat(badgeClass).isEqualTo("badge-secondary");
    }

    @Test
    @DisplayName("Should get overdue assignments")
    void getOverdueAssignments_ShouldReturnOverdueAssignments() {
        // Given
        List<PlannerItem> overdueAssignments = Arrays.asList(
                createOverdueAssignment(1L),
                createOverdueAssignment(2L)
        );
        when(plannerItemRepository.findOverdueAssignments(eq(studentId), any(LocalDateTime.class)))
                .thenReturn(overdueAssignments);

        // When
        List<PlannerItem> result = assignmentService.getOverdueAssignments(studentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(plannerItemRepository).findOverdueAssignments(eq(studentId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should get assignment count for date range")
    void getAssignmentCount_ShouldReturnCount() {
        // Given
        long expectedCount = 5L;
        when(plannerItemRepository.countByStudentIdAndDueAtBetween(studentId, startDateTime, endDateTime))
                .thenReturn(expectedCount);

        // When
        long result = assignmentService.getAssignmentCount(studentId, startDate, endDate);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(plannerItemRepository).countByStudentIdAndDueAtBetween(studentId, startDateTime, endDateTime);
    }

    // Helper methods for creating test data

    private List<AssignmentDto> createMockAssignmentDtos() {
        return Arrays.asList(
                AssignmentDto.builder()
                        .plannableId(1L)
                        .studentId(studentId)
                        .assignmentTitle("Math Homework")
                        .contextName("Mathematics")
                        .dueAt(LocalDateTime.now().plusDays(1))
                        .pointsPossible(BigDecimal.valueOf(100))
                        .build(),
                AssignmentDto.builder()
                        .plannableId(2L)
                        .studentId(studentId)
                        .assignmentTitle("Science Project")
                        .contextName("Science")
                        .dueAt(LocalDateTime.now().plusDays(3))
                        .pointsPossible(BigDecimal.valueOf(150))
                        .build()
        );
    }

    private List<PlannerItem> createMockPlannerItems() {
        return Arrays.asList(
                PlannerItem.builder()
                        .id(1L)
                        .studentId(studentId)
                        .plannableId(1L)
                        .assignmentTitle("Math Homework")
                        .contextName("Mathematics")
                        .dueAt(LocalDateTime.now().plusDays(1))
                        .pointsPossible(BigDecimal.valueOf(100))
                        .submitted(false)
                        .missing(false)
                        .late(false)
                        .graded(false)
                        .build(),
                PlannerItem.builder()
                        .id(2L)
                        .studentId(studentId)
                        .plannableId(2L)
                        .assignmentTitle("Science Project")
                        .contextName("Science")
                        .dueAt(LocalDateTime.now().plusDays(3))
                        .pointsPossible(BigDecimal.valueOf(150))
                        .submitted(false)
                        .missing(false)
                        .late(false)
                        .graded(false)
                        .build()
        );
    }

    private List<PlannerItem> createExistingPlannerItems() {
        return Arrays.asList(
                PlannerItem.builder()
                        .id(1L)
                        .studentId(studentId)
                        .plannableId(1L)
                        .assignmentTitle("Old Math Homework")
                        .contextName("Mathematics")
                        .dueAt(LocalDateTime.now().plusDays(1))
                        .pointsPossible(BigDecimal.valueOf(80))
                        .submitted(false)
                        .missing(false)
                        .late(false)
                        .graded(false)
                        .build()
        );
    }

    private PlannerItem createOverdueAssignment(Long plannableId) {
        return PlannerItem.builder()
                .id(plannableId)
                .studentId(studentId)
                .plannableId(plannableId)
                .assignmentTitle("Overdue Assignment " + plannableId)
                .contextName("Test Course")
                .dueAt(LocalDateTime.now().minusDays(1))
                .pointsPossible(BigDecimal.valueOf(100))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();
    }
}