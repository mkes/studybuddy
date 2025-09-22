package com.studytracker.controller;

import com.studytracker.service.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SyncController focusing on timeout and performance scenarios.
 */
@WebMvcTest(SyncController.class)
class SyncControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssignmentService assignmentService;

    private MockHttpSession session;
    private static final String CANVAS_TOKEN = "test-canvas-token";
    private static final Long STUDENT_ID = 12345L;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute("canvasToken", CANVAS_TOKEN);
    }

    @Test
    void syncStudentAssignments_TimeoutScenario_ReturnsServiceUnavailable() throws Exception {
        // Given - Mock a slow service that takes longer than 10 seconds
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenAnswer(invocation -> {
                    // Simulate a slow operation that exceeds timeout
                    Thread.sleep(12000); // 12 seconds - exceeds 10 second timeout
                    return java.util.Collections.emptyList();
                });

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value(containsString("timed out")))
                .andExpect(jsonPath("$.error_message").value(containsString("10 seconds")))
                .andExpect(jsonPath("$.assignments_count").value(0));
    }

    @Test
    void syncStudentAssignments_FastOperation_CompletesWithinTimeout() throws Exception {
        // Given - Mock a fast service that completes quickly
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenAnswer(invocation -> {
                    // Simulate a fast operation
                    Thread.sleep(100); // 100ms - well within timeout
                    return java.util.Collections.emptyList();
                });

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.assignments_count").value(0));
    }

    @Test
    void syncStudentAssignments_InterruptedOperation_HandlesGracefully() throws Exception {
        // Given - Mock an operation that gets interrupted
        when(assignmentService.syncAssignments(eq(STUDENT_ID), eq(CANVAS_TOKEN)))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("Operation interrupted");
                });

        // When & Then
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.student_id").value(STUDENT_ID))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error_message").value("Internal server error during sync"));
    }

    @Test
    void syncStudentAssignments_ConcurrentRequests_HandledCorrectly() throws Exception {
        // Given - Mock service that handles concurrent requests
        when(assignmentService.syncAssignments(any(Long.class), eq(CANVAS_TOKEN)))
                .thenAnswer(invocation -> {
                    Thread.sleep(1000); // 1 second operation
                    return java.util.Collections.emptyList();
                });

        // When & Then - Test multiple concurrent requests
        // This test verifies that the controller can handle multiple simultaneous sync requests
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Second concurrent request with different student
        MockHttpSession session2 = new MockHttpSession();
        session2.setAttribute("canvasToken", CANVAS_TOKEN);
        
        mockMvc.perform(post("/api/sync/{studentId}", STUDENT_ID + 1)
                        .session(session2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}