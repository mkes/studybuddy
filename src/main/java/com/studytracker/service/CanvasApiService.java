package com.studytracker.service;

import com.studytracker.dto.AssignmentDto;
import com.studytracker.dto.StudentDto;
import com.studytracker.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Canvas LMS REST API.
 * Handles authentication, API calls, and error handling for Canvas integration.
 */
@Slf4j
@Service
public class CanvasApiService {

    private final WebClient webClient;
    private final String canvasBaseUrl;

    public CanvasApiService(@Value("${canvas.api.base-url}") String canvasBaseUrl) {
        this.canvasBaseUrl = canvasBaseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(canvasBaseUrl)
                .build();
    }

    /**
     * Validates a Canvas API token by making a test API call.
     * 
     * @param token Canvas API token to validate
     * @return true if token is valid, false otherwise
     * @throws InvalidTokenException if token is invalid or expired
     * @throws CanvasApiException for other API errors
     */
    public boolean validateToken(String token) {
        log.debug("Validating Canvas API token");
        
        try {
            webClient.get()
                    .uri("/api/v1/users/self")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            log.debug("Canvas API token validation successful");
            return true;
            
        } catch (WebClientResponseException e) {
            handleWebClientException(e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token validation", e);
            throw new CanvasApiException("Failed to validate Canvas token", e);
        }
    }

    /**
     * Retrieves list of students that the authenticated user observes.
     * 
     * @param token Canvas API token with observer permissions
     * @return List of observed students
     * @throws InvalidTokenException if token is invalid
     * @throws InsufficientPermissionsException if token lacks observer permissions
     * @throws CanvasApiException for other API errors
     */
    public List<StudentDto> getObservedStudents(String token) {
        log.debug("Fetching observed students from Canvas API");
        
        try {
            List<StudentDto> students = webClient.get()
                    .uri("/api/v1/users/self/observees")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<StudentDto>>() {})
                    .timeout(Duration.ofSeconds(15))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(this::isRetryableException))
                    .block();
            
            log.debug("Successfully fetched {} observed students", students != null ? students.size() : 0);
            return students != null ? students : List.of();
            
        } catch (WebClientResponseException e) {
            handleWebClientException(e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching observed students", e);
            throw new CanvasApiException("Failed to fetch observed students", e);
        }
    }

    /**
     * Retrieves assignments for a specific student within a date range.
     * 
     * @param token Canvas API token
     * @param studentId Canvas user ID of the student
     * @param startDate Start date for assignment filtering (inclusive)
     * @param endDate End date for assignment filtering (inclusive)
     * @return List of assignments for the student
     * @throws InvalidTokenException if token is invalid
     * @throws InsufficientPermissionsException if token lacks permissions for this student
     * @throws CanvasApiException for other API errors
     */
    public List<AssignmentDto> getStudentAssignments(String token, Long studentId, 
                                                   LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching assignments for student {} from {} to {}", studentId, startDate, endDate);
        
        try {
            String startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            List<AssignmentDto> assignments = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/planner/items")
                            .queryParam("user_id", studentId)
                            .queryParam("start_date", startDateStr)
                            .queryParam("end_date", endDateStr)
                            .queryParam("filter", "new_activity")
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<AssignmentDto>>() {})
                    .timeout(Duration.ofSeconds(20))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(this::isRetryableException))
                    .block();
            
            log.debug("Successfully fetched {} assignments for student {}", 
                     assignments != null ? assignments.size() : 0, studentId);
            return assignments != null ? assignments : List.of();
            
        } catch (WebClientResponseException e) {
            handleWebClientException(e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching assignments for student {}", studentId, e);
            throw new CanvasApiException("Failed to fetch student assignments", e);
        }
    }

    /**
     * Handles WebClientResponseException and converts to appropriate Canvas API exceptions.
     * 
     * @param e WebClientResponseException to handle
     * @throws CanvasApiException appropriate exception based on HTTP status
     */
    private void handleWebClientException(WebClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();
        
        log.warn("Canvas API error: {} - {}", e.getStatusCode(), responseBody);
        
        switch (e.getStatusCode().value()) {
            case 401:
                throw new InvalidTokenException("Canvas API token is invalid or expired");
            case 403:
                throw new InsufficientPermissionsException("Insufficient permissions to access Canvas data");
            case 429:
                long retryAfter = extractRetryAfterSeconds(e);
                throw new RateLimitExceededException("Canvas API rate limit exceeded", retryAfter);
            case 502:
            case 503:
            case 504:
                throw new CanvasUnavailableException("Canvas API is currently unavailable");
            default:
                throw new CanvasApiException("Canvas API error: " + e.getMessage(), e.getStatusCode().value());
        }
    }

    /**
     * Extracts retry-after seconds from rate limit response headers.
     * 
     * @param e WebClientResponseException with rate limit response
     * @return retry after seconds, defaults to 60 if not found
     */
    private long extractRetryAfterSeconds(WebClientResponseException e) {
        try {
            String retryAfter = e.getHeaders().getFirst("Retry-After");
            return retryAfter != null ? Long.parseLong(retryAfter) : 60;
        } catch (NumberFormatException ex) {
            log.warn("Invalid Retry-After header value", ex);
            return 60;
        }
    }

    /**
     * Determines if an exception is retryable (transient network errors).
     * 
     * @param throwable Exception to check
     * @return true if exception is retryable
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException e = (WebClientResponseException) throwable;
            int statusCode = e.getStatusCode().value();
            // Retry on server errors (5xx) but not client errors (4xx)
            return statusCode >= 500 && statusCode < 600;
        }
        // Retry on network timeouts and connection issues
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException ||
               throwable.getClass().getName().contains("ReactiveException");
    }
}