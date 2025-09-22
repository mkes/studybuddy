package com.studytracker.controller;

import com.studytracker.dto.SyncResponseDto;
import com.studytracker.exception.CanvasApiException;
import com.studytracker.exception.RateLimitExceededException;
import com.studytracker.exception.SyncTimeoutException;
import com.studytracker.model.PlannerItem;
import com.studytracker.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * REST controller for internal sync API operations.
 * Provides endpoints for synchronizing assignment data from Canvas API.
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final AssignmentService assignmentService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 10-second timeout as per requirements
    private static final long SYNC_TIMEOUT_SECONDS = 10;

    /**
     * Synchronizes assignments for a specific student.
     * Fetches latest assignment data from Canvas API and updates local cache.
     * 
     * @param studentId Canvas user ID of the student
     * @param session HTTP session containing Canvas API token
     * @return ResponseEntity with sync results or error information
     */
    @PostMapping("/{studentId}")
    public ResponseEntity<SyncResponseDto> syncStudentAssignments(
            @PathVariable Long studentId,
            HttpSession session) {

        log.info("Starting sync operation for student {}", studentId);
        
        try {
            // Get Canvas token from session
            String token = (String) session.getAttribute("canvasToken");
            if (token == null || token.trim().isEmpty()) {
                log.warn("No Canvas token found in session for sync request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(studentId, "No Canvas token found in session"));
            }

            // Execute sync with timeout
            SyncResponseDto response = executeSyncWithTimeout(studentId, token);
            
            log.info("Sync completed successfully for student {} with {} assignments", 
                    studentId, response.getAssignmentsCount());
            
            return ResponseEntity.ok(response);

        } catch (SyncTimeoutException e) {
            log.error("Sync timeout for student {}: {}", studentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(createErrorResponse(studentId, "Sync operation timed out after " + 
                            SYNC_TIMEOUT_SECONDS + " seconds"));

        } catch (RateLimitExceededException e) {
            log.error("Rate limit exceeded for student {}: {}", studentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse(studentId, "Canvas API rate limit exceeded. " +
                            "Please try again in " + e.getRetryAfterSeconds() + " seconds"));

        } catch (CanvasApiException e) {
            log.error("Canvas API error during sync for student {}: {}", studentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.valueOf(e.getStatusCode()))
                    .body(createErrorResponse(studentId, "Canvas API error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during sync for student {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(studentId, "Internal server error during sync"));
        }
    }

    /**
     * Executes the sync operation with a timeout to ensure it completes within 10 seconds.
     * 
     * @param studentId Canvas user ID of the student
     * @param token Canvas API token
     * @return SyncResponseDto with sync results
     * @throws SyncTimeoutException if sync takes longer than timeout
     */
    private SyncResponseDto executeSyncWithTimeout(Long studentId, String token) 
            throws SyncTimeoutException {
        
        Future<SyncResponseDto> future = executorService.submit(() -> {
            // Perform the actual sync operation
            List<PlannerItem> syncedAssignments = assignmentService.syncAssignments(studentId, token);
            
            // Convert to response format
            return createSuccessResponse(studentId, syncedAssignments);
        });

        try {
            // Wait for completion with timeout
            return future.get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SyncTimeoutException("Sync operation exceeded " + SYNC_TIMEOUT_SECONDS + 
                    " second timeout", SYNC_TIMEOUT_SECONDS);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new RuntimeException("Sync operation was interrupted", e);
            
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CanvasApiException) {
                throw (CanvasApiException) cause;
            } else if (cause instanceof RateLimitExceededException) {
                throw (RateLimitExceededException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException("Sync operation failed", cause);
            }
        }
    }

    /**
     * Creates a successful sync response with assignment data.
     * 
     * @param studentId Canvas user ID of the student
     * @param assignments List of synchronized assignments
     * @return SyncResponseDto with success status and assignment data
     */
    private SyncResponseDto createSuccessResponse(Long studentId, List<PlannerItem> assignments) {
        List<SyncResponseDto.AssignmentSyncDto> assignmentDtos = assignments.stream()
                .map(this::convertToAssignmentSyncDto)
                .toList();

        return SyncResponseDto.builder()
                .studentId(studentId)
                .assignmentsCount(assignments.size())
                .syncTimestamp(LocalDateTime.now())
                .assignments(assignmentDtos)
                .status("success")
                .build();
    }

    /**
     * Creates an error response for failed sync operations.
     * 
     * @param studentId Canvas user ID of the student
     * @param errorMessage Error message to include in response
     * @return SyncResponseDto with error status and message
     */
    private SyncResponseDto createErrorResponse(Long studentId, String errorMessage) {
        return SyncResponseDto.builder()
                .studentId(studentId)
                .assignmentsCount(0)
                .syncTimestamp(LocalDateTime.now())
                .status("error")
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Converts a PlannerItem entity to AssignmentSyncDto for API response.
     * 
     * @param plannerItem PlannerItem entity to convert
     * @return AssignmentSyncDto with assignment data
     */
    private SyncResponseDto.AssignmentSyncDto convertToAssignmentSyncDto(PlannerItem plannerItem) {
        String status = assignmentService.calculateAssignmentStatus(plannerItem);
        
        return SyncResponseDto.AssignmentSyncDto.builder()
                .assignmentId(plannerItem.getPlannableId())
                .title(plannerItem.getAssignmentTitle())
                .courseName(plannerItem.getContextName())
                .dueAt(plannerItem.getDueAt())
                .pointsPossible(plannerItem.getPointsPossible())
                .currentGrade(plannerItem.getCurrentGrade())
                .status(status)
                .submitted(plannerItem.getSubmitted())
                .missing(plannerItem.getMissing())
                .late(plannerItem.getLate())
                .graded(plannerItem.getGraded())
                .build();
    }

    /**
     * Health check endpoint for sync API.
     * 
     * @return ResponseEntity indicating sync API is available
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Sync API is healthy");
    }
}