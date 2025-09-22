package com.studytracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studytracker.dto.SyncResponseDto;
import com.studytracker.exception.CanvasApiException;
import com.studytracker.exception.RateLimitExceededException;
import com.studytracker.model.PlannerItem;
import com.studytracker.service.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SyncController.
 * Tests the sync API endpoints and error handling scenarios.
 */
@WebMvcTest(SyncController.class)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssignmentService assignmentService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;
    private static final String CANVAS_TOKEN = "test-canvas-token";
    private static final Long STUDENT_ID = 12345L;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute("canvasToken", CANVAS_TOKEN);
    }

    @Test
    void syncStudentAssignments_Success() throws Exception {
        // Given
        List<PlannerItem> mockAssignments = createMockAssignments();
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenReturn(mockAssignments);
        when(assignmentService.calculateAssignmentStatus(any(PlannerItem.class)))
                .thenReturn("pending");

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.assignments_count").value(2))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.error_message").doesNotExist())
                .andExpect(jsonPath("$.sync_timestamp").exists())
                .andExpect(jsonPath("$.assignments").isArray())
                .andExpect(jsonPath("$.assignments", hasSize(2)))
                .andExpect(jsonPath("$.assignments[0].assignment_id").value(1001L))
                .andExpect(jsonPath("$.assignments[0].title").value("Math Homework"))
                .andExpect(jsonPath("$.assignments[0].course_name").value("Mathematics"))
                .andExpect(jsonPath("$.assignments[0].points_possible").value(100))
                .andExpect(jsonPath("$.assignments[0].status").value("pending"));

        verify(assignmentService).syncAssignments(STUDENT_ID, CANVAS_TOKEN);
    }

    @Test
    void syncStudentAssignments_NoToken_ReturnsUnauthorized() throws Exception {
        // Given
        MockHttpSession emptySession = new MockHttpSession();

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(emptySession)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value("No Canvas token found in session"))
                .andExpect(jsonPath("$.assignments_count").value(0));

        verify(assignmentService, never()).syncAssignments(any(), any());
    }

    @Test
    void syncStudentAssignments_EmptyToken_ReturnsUnauthorized() throws Exception {
        // Given
        MockHttpSession sessionWithEmptyToken = new MockHttpSession();
        sessionWithEmptyToken.setAttribute("canvasToken", "   ");

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(sessionWithEmptyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value("No Canvas token found in session"));

        verify(assignmentService, never()).syncAssignments(any(), any());
    }

    @Test
    void syncStudentAssignments_RateLimitExceeded_ReturnsTooManyRequests() throws Exception {
        // Given
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded", 60L));

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value(containsString("rate limit exceeded")))
                .andExpect(jsonPath("$.error_message").value(containsString("60 seconds")))
                .andExpect(jsonPath("$.assignments_count").value(0));
    }

    @Test
    void syncStudentAssignments_CanvasApiException_ReturnsAppropriateStatus() throws Exception {
        // Given
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenThrow(new CanvasApiException("Canvas API unavailable", 502));

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value(containsString("Canvas API error")))
                .andExpect(jsonPath("$.assignments_count").value(0));
    }

    @Test
    void syncStudentAssignments_GenericException_ReturnsInternalServerError() throws Exception {
        // Given
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value("Internal server error during sync"))
                .andExpect(jsonPath("$.assignments_count").value(0));
    }

    @Test
    void syncStudentAssignments_EmptyAssignmentsList_ReturnsSuccess() throws Exception {
        // Given
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.assignments_count").value(0))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.assignments").isArray())
                .andExpect(jsonPath("$.assignments", hasSize(0)));
    }

    @Test
    void syncStudentAssignments_WithGrades_ReturnsCompleteData() throws Exception {
        // Given
        List<PlannerItem> mockAssignments = createMockAssignmentsWithGrades();
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenReturn(mockAssignments);
        when(assignmentService.calculateAssignmentStatus(any(PlannerItem.class)))
                .thenReturn("submitted", "missing");

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].current_grade").value(85.5))
                .andExpect(jsonPath("$.assignments[0].status").value("submitted"))
                .andExpect(jsonPath("$.assignments[0].submitted").value(true))
                .andExpect(jsonPath("$.assignments[0].graded").value(true))
                .andExpect(jsonPath("$.assignments[1].status").value("missing"))
                .andExpect(jsonPath("$.assignments[1].missing").value(true));
    }

    @Test
    void health_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/sync/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Sync API is healthy"));
    }

    @Test
    void syncStudentAssignments_InvalidStudentId_HandledGracefully() throws Exception {
        // Given
        Long invalidStudentId = -1L;
        when(assignmentService.syncAssignments(eq(invalidStudentId), eq(CANVAS_TOKEN)))
                .thenThrow(new CanvasApiException("Student not found", 404));

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", invalidStudentId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.student_id").value(invalidStudentId))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void syncStudentAssignments_LargeAssignmentList_HandlesCorrectly() throws Exception {
        // Given
        List<PlannerItem> largeAssignmentList = createLargeAssignmentList(50);
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenReturn(largeAssignmentList);
        when(assignmentService.calculateAssignmentStatus(any(PlannerItem.class)))
                .thenReturn("pending");

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments_count").value(50))
                .andExpect(jsonPath("$.assignments", hasSize(50)))
                .andExpect(jsonPath("$.status").value("success"));
    }

    // Helper methods

    private List<PlannerItem> createMockAssignments() {
        PlannerItem assignment1 = PlannerItem.builder()
                .id(1L)
                .studentId(STUDENT_ID)
                .plannableId(1001L)
                .assignmentTitle("Math Homework")
                .contextName("Mathematics")
                .dueAt(LocalDateTime.now().plusDays(1))
                .pointsPossible(new BigDecimal("100.00"))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();

        PlannerItem assignment2 = PlannerItem.builder()
                .id(2L)
                .studentId(STUDENT_ID)
                .plannableId(1002L)
                .assignmentTitle("Science Project")
                .contextName("Science")
                .dueAt(LocalDateTime.now().plusDays(3))
                .pointsPossible(new BigDecimal("150.00"))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();

        return Arrays.asList(assignment1, assignment2);
    }

    private List<PlannerItem> createMockAssignmentsWithGrades() {
        PlannerItem assignment1 = PlannerItem.builder()
                .id(1L)
                .studentId(STUDENT_ID)
                .plannableId(1001L)
                .assignmentTitle("Graded Assignment")
                .contextName("Mathematics")
                .dueAt(LocalDateTime.now().minusDays(1))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.50"))
                .submitted(true)
                .missing(false)
                .late(false)
                .graded(true)
                .build();

        PlannerItem assignment2 = PlannerItem.builder()
                .id(2L)
                .studentId(STUDENT_ID)
                .plannableId(1002L)
                .assignmentTitle("Missing Assignment")
                .contextName("Science")
                .dueAt(LocalDateTime.now().minusDays(2))
                .pointsPossible(new BigDecimal("50.00"))
                .submitted(false)
                .missing(true)
                .late(false)
                .graded(false)
                .build();

        return Arrays.asList(assignment1, assignment2);
    }

    private List<PlannerItem> createLargeAssignmentList(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> PlannerItem.builder()
                        .id((long) i)
                        .studentId(STUDENT_ID)
                        .plannableId(1000L + i)
                        .assignmentTitle("Assignment " + i)
                        .contextName("Course " + (i % 5))
                        .dueAt(LocalDateTime.now().plusDays(i % 30))
                        .pointsPossible(new BigDecimal("100.00"))
                        .submitted(false)
                        .missing(false)
                        .late(false)
                        .graded(false)
                        .build())
                .toList();
    }
}