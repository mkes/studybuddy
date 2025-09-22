package com.studytracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studytracker.dto.AssignmentDto;
import com.studytracker.dto.StudentDto;
import com.studytracker.exception.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CanvasApiService with mocked HTTP responses.
 * Tests all Canvas API integration methods and error handling scenarios.
 */
class CanvasApiServiceTest {

    private MockWebServer mockWebServer;
    private CanvasApiService canvasApiService;
    private ObjectMapper objectMapper;
    
    private static final String VALID_TOKEN = "test-canvas-token-123";
    private static final String INVALID_TOKEN = "invalid-token";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        canvasApiService = new CanvasApiService(baseUrl);
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() throws InterruptedException {
        // Given
        Map<String, Object> userResponse = Map.of(
            "id", 12345L,
            "name", "Test User",
            "email", "test@example.com"
        );
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(toJson(userResponse)));

        // When
        boolean result = canvasApiService.validateToken(VALID_TOKEN);

        // Then
        assertThat(result).isTrue();
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/users/self");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer " + VALID_TOKEN);
    }

    @Test
    void validateToken_WithInvalidToken_ThrowsInvalidTokenException() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"errors\":[{\"message\":\"Invalid access token\"}]}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.validateToken(INVALID_TOKEN))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("Canvas API token is invalid or expired");
    }

    @Test
    void validateToken_WithServerError_ThrowsCanvasApiException() {
        // Given - Enqueue multiple 500 responses to handle retries
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Internal server error\"}"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Internal server error\"}"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Internal server error\"}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.validateToken(VALID_TOKEN))
            .isInstanceOf(CanvasApiException.class)
            .hasMessageContaining("Failed to validate Canvas token");
    }

    @Test
    void getObservedStudents_WithValidToken_ReturnsStudentList() throws InterruptedException {
        // Given
        List<StudentDto> expectedStudents = List.of(
            StudentDto.builder()
                .id(1001L)
                .name("John Doe")
                .sortableName("Doe, John")
                .shortName("John")
                .avatarUrl("https://example.com/avatar1.jpg")
                .loginId("john.doe")
                .build(),
            StudentDto.builder()
                .id(1002L)
                .name("Jane Smith")
                .sortableName("Smith, Jane")
                .shortName("Jane")
                .avatarUrl("https://example.com/avatar2.jpg")
                .loginId("jane.smith")
                .build()
        );
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(toJson(expectedStudents)));

        // When
        List<StudentDto> result = canvasApiService.getObservedStudents(VALID_TOKEN);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1001L);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        assertThat(result.get(1).getId()).isEqualTo(1002L);
        assertThat(result.get(1).getName()).isEqualTo("Jane Smith");
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/users/self/observees");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer " + VALID_TOKEN);
    }

    @Test
    void getObservedStudents_WithEmptyResponse_ReturnsEmptyList() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("[]"));

        // When
        List<StudentDto> result = canvasApiService.getObservedStudents(VALID_TOKEN);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getObservedStudents_WithInsufficientPermissions_ThrowsInsufficientPermissionsException() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(403)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"errors\":[{\"message\":\"Insufficient permissions\"}]}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.getObservedStudents(VALID_TOKEN))
            .isInstanceOf(InsufficientPermissionsException.class)
            .hasMessageContaining("Insufficient permissions to access Canvas data");
    }

    @Test
    void getStudentAssignments_WithValidParameters_ReturnsAssignmentList() throws InterruptedException {
        // Given
        Long studentId = 1001L;
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        
        List<AssignmentDto> expectedAssignments = List.of(
            AssignmentDto.builder()
                .plannableId(2001L)
                .studentId(studentId)
                .assignmentTitle("Math Homework 1")
                .contextName("Algebra I")
                .dueAt(LocalDateTime.of(2025, 8, 15, 23, 59))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.00"))
                .submission(AssignmentDto.SubmissionDto.builder()
                    .submitted(true)
                    .missing(false)
                    .late(false)
                    .graded(true)
                    .score(new BigDecimal("85.00"))
                    .grade("B")
                    .workflowState("graded")
                    .build())
                .build(),
            AssignmentDto.builder()
                .plannableId(2002L)
                .studentId(studentId)
                .assignmentTitle("Science Lab Report")
                .contextName("Biology")
                .dueAt(LocalDateTime.of(2025, 8, 20, 23, 59))
                .pointsPossible(new BigDecimal("50.00"))
                .submission(AssignmentDto.SubmissionDto.builder()
                    .submitted(false)
                    .missing(true)
                    .late(false)
                    .graded(false)
                    .workflowState("unsubmitted")
                    .build())
                .build()
        );
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(toJson(expectedAssignments)));

        // When
        List<AssignmentDto> result = canvasApiService.getStudentAssignments(VALID_TOKEN, studentId, startDate, endDate);

        // Then
        assertThat(result).hasSize(2);
        
        AssignmentDto assignment1 = result.get(0);
        assertThat(assignment1.getPlannableId()).isEqualTo(2001L);
        assertThat(assignment1.getAssignmentTitle()).isEqualTo("Math Homework 1");
        assertThat(assignment1.getContextName()).isEqualTo("Algebra I");
        assertThat(assignment1.getSubmission().getSubmitted()).isTrue();
        assertThat(assignment1.getSubmission().getGraded()).isTrue();
        
        AssignmentDto assignment2 = result.get(1);
        assertThat(assignment2.getPlannableId()).isEqualTo(2002L);
        assertThat(assignment2.getAssignmentTitle()).isEqualTo("Science Lab Report");
        assertThat(assignment2.getSubmission().getMissing()).isTrue();
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/api/v1/planner/items");
        assertThat(request.getPath()).contains("user_id=1001");
        assertThat(request.getPath()).contains("start_date=2025-08-01");
        assertThat(request.getPath()).contains("end_date=2025-09-30");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer " + VALID_TOKEN);
    }

    @Test
    void getStudentAssignments_WithRateLimitExceeded_ThrowsRateLimitExceededException() {
        // Given
        Long studentId = 1001L;
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setHeader("Content-Type", "application/json")
            .setHeader("Retry-After", "120")
            .setBody("{\"errors\":[{\"message\":\"Rate limit exceeded\"}]}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.getStudentAssignments(VALID_TOKEN, studentId, startDate, endDate))
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessageContaining("Canvas API rate limit exceeded")
            .satisfies(ex -> {
                RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
                assertThat(rateLimitEx.getRetryAfterSeconds()).isEqualTo(120);
            });
    }

    @Test
    void getStudentAssignments_WithCanvasUnavailable_ThrowsCanvasUnavailableException() {
        // Given
        Long studentId = 1001L;
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        
        // Enqueue multiple 502 responses to handle retries
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Bad Gateway\"}"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Bad Gateway\"}"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Bad Gateway\"}"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Bad Gateway\"}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.getStudentAssignments(VALID_TOKEN, studentId, startDate, endDate))
            .isInstanceOf(CanvasApiException.class)
            .hasMessageContaining("Failed to fetch student assignments");
    }

    @Test
    void getStudentAssignments_WithNetworkTimeout_RetriesAndFails() {
        // Given
        Long studentId = 1001L;
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        
        // Enqueue multiple timeout responses to test retry logic
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.getStudentAssignments(VALID_TOKEN, studentId, startDate, endDate))
            .isInstanceOf(CanvasApiException.class)
            .hasMessageContaining("Failed to fetch student assignments");
    }

    @Test
    void getStudentAssignments_WithRetryAfterMissingHeader_UsesDefaultRetryTime() {
        // Given
        Long studentId = 1001L;
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"errors\":[{\"message\":\"Rate limit exceeded\"}]}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.getStudentAssignments(VALID_TOKEN, studentId, startDate, endDate))
            .isInstanceOf(RateLimitExceededException.class)
            .satisfies(ex -> {
                RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
                assertThat(rateLimitEx.getRetryAfterSeconds()).isEqualTo(60); // Default value
            });
    }

    @Test
    void getStudentAssignments_WithInvalidRetryAfterHeader_UsesDefaultRetryTime() {
        // Given
        Long studentId = 1001L;
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setHeader("Content-Type", "application/json")
            .setHeader("Retry-After", "invalid-number")
            .setBody("{\"errors\":[{\"message\":\"Rate limit exceeded\"}]}"));

        // When & Then
        assertThatThrownBy(() -> canvasApiService.getStudentAssignments(VALID_TOKEN, studentId, startDate, endDate))
            .isInstanceOf(RateLimitExceededException.class)
            .satisfies(ex -> {
                RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
                assertThat(rateLimitEx.getRetryAfterSeconds()).isEqualTo(60); // Default value
            });
    }

    /**
     * Helper method to convert objects to JSON string for mock responses.
     */
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}