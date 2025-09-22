package com.studytracker.service;

import com.studytracker.dto.AssignmentDto;
import com.studytracker.dto.mapper.CanvasMapper;
import com.studytracker.model.PlannerItem;
import com.studytracker.repository.PlannerItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing assignment business logic and synchronization.
 * Handles assignment data synchronization from Canvas API and provides
 * business logic for assignment status calculations and filtering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final CanvasApiService canvasApiService;
    private final PlannerItemRepository plannerItemRepository;
    private final CanvasMapper canvasMapper;

    // Default date range: Aug 1 - Sep 30, 2025
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2025, 8, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2025, 9, 30);

    /**
     * Synchronizes assignments for a student from Canvas API to local database.
     * Fetches assignments within the default date range and updates local cache.
     * 
     * @param studentId Canvas user ID of the student
     * @param token Canvas API token for authentication
     * @return List of synchronized PlannerItem entities
     */
    @Transactional
    public List<PlannerItem> syncAssignments(Long studentId, String token) {
        return syncAssignments(studentId, token, DEFAULT_START_DATE, DEFAULT_END_DATE);
    }

    /**
     * Synchronizes assignments for a student within a specific date range.
     * Fetches assignments from Canvas API and updates local database cache.
     * 
     * @param studentId Canvas user ID of the student
     * @param token Canvas API token for authentication
     * @param startDate Start date for assignment filtering (inclusive)
     * @param endDate End date for assignment filtering (inclusive)
     * @return List of synchronized PlannerItem entities
     */
    @Transactional
    public List<PlannerItem> syncAssignments(Long studentId, String token, 
                                           LocalDate startDate, LocalDate endDate) {
        log.debug("Starting assignment sync for student {} from {} to {}", 
                 studentId, startDate, endDate);

        try {
            // Fetch assignments from Canvas API
            List<AssignmentDto> assignmentDtos = canvasApiService.getStudentAssignments(
                    token, studentId, startDate, endDate);
            
            log.debug("Fetched {} assignments from Canvas API for student {}", 
                     assignmentDtos.size(), studentId);

            // Convert DTOs to entities
            List<PlannerItem> newAssignments = canvasMapper.toPlannerItems(assignmentDtos, studentId);

            // Get existing assignments for the date range
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            List<PlannerItem> existingAssignments = plannerItemRepository
                    .findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime);

            // Create map of existing assignments by plannable ID for efficient lookup
            Map<Long, PlannerItem> existingAssignmentMap = existingAssignments.stream()
                    .collect(Collectors.toMap(PlannerItem::getPlannableId, Function.identity()));

            // Process assignments: update existing or create new
            List<PlannerItem> assignmentsToSave = newAssignments.stream()
                    .map(newAssignment -> {
                        PlannerItem existingAssignment = existingAssignmentMap.get(newAssignment.getPlannableId());
                        if (existingAssignment != null) {
                            // Update existing assignment
                            return updateExistingAssignment(existingAssignment, newAssignment);
                        } else {
                            // New assignment
                            return newAssignment;
                        }
                    })
                    .collect(Collectors.toList());

            // Save all assignments (new and updated)
            List<PlannerItem> savedAssignments = plannerItemRepository.saveAll(assignmentsToSave);
            
            log.info("Successfully synchronized {} assignments for student {}", 
                    savedAssignments.size(), studentId);

            return savedAssignments;

        } catch (Exception e) {
            log.error("Failed to sync assignments for student {}", studentId, e);
            throw new RuntimeException("Assignment synchronization failed", e);
        }
    }

    /**
     * Retrieves assignments for a student within the default date range.
     * Returns cached assignments from local database.
     * 
     * @param studentId Canvas user ID of the student
     * @return List of PlannerItem entities ordered by due date descending
     */
    public List<PlannerItem> getAssignmentsByStudent(Long studentId) {
        return getAssignmentsByStudent(studentId, DEFAULT_START_DATE, DEFAULT_END_DATE);
    }

    /**
     * Retrieves assignments for a student within a specific date range.
     * Returns cached assignments from local database with calculated status.
     * 
     * @param studentId Canvas user ID of the student
     * @param startDate Start date for assignment filtering (inclusive)
     * @param endDate End date for assignment filtering (inclusive)
     * @return List of PlannerItem entities ordered by due date descending
     */
    public List<PlannerItem> getAssignmentsByStudent(Long studentId, LocalDate startDate, LocalDate endDate) {
        log.debug("Retrieving assignments for student {} from {} to {}", 
                 studentId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<PlannerItem> assignments = plannerItemRepository
                .findByStudentIdAndDueAtBetweenOrderByDueAtDesc(studentId, startDateTime, endDateTime);

        // Update assignment status based on current time
        assignments.forEach(this::updateAssignmentStatus);

        log.debug("Retrieved {} assignments for student {}", assignments.size(), studentId);
        return assignments;
    }

    /**
     * Calculates and returns the assignment status based on submission flags and due date.
     * Status logic:
     * - "submitted": Assignment is submitted and graded
     * - "missing": Assignment is marked as missing in Canvas
     * - "late": Assignment was submitted late
     * - "overdue": Assignment is past due date and not submitted
     * - "pending": Assignment is not yet due or awaiting grading
     * 
     * @param assignment PlannerItem to calculate status for
     * @return String representing the assignment status
     */
    public String calculateAssignmentStatus(PlannerItem assignment) {
        if (assignment == null) {
            return "pending";
        }

        // Check if submitted and graded
        if (Boolean.TRUE.equals(assignment.getSubmitted()) && Boolean.TRUE.equals(assignment.getGraded())) {
            return "submitted";
        }

        // Check if marked as missing
        if (Boolean.TRUE.equals(assignment.getMissing())) {
            return "missing";
        }

        // Check if submitted late
        if (Boolean.TRUE.equals(assignment.getLate())) {
            return "late";
        }

        // Check if overdue (past due date and not submitted)
        if (assignment.getDueAt() != null && 
            assignment.getDueAt().isBefore(LocalDateTime.now()) && 
            !Boolean.TRUE.equals(assignment.getSubmitted())) {
            return "overdue";
        }

        // Default to pending
        return "pending";
    }

    /**
     * Gets the CSS badge class for assignment status visualization.
     * 
     * @param status Assignment status string
     * @return CSS class name for status badge
     */
    public String getStatusBadgeClass(String status) {
        return switch (status) {
            case "submitted" -> "badge-success";
            case "missing", "overdue" -> "badge-danger";
            case "late" -> "badge-warning";
            case "pending" -> "badge-primary";
            default -> "badge-secondary";
        };
    }

    /**
     * Updates an existing assignment with new data while preserving entity metadata.
     * 
     * @param existingAssignment Current assignment entity
     * @param newAssignment New assignment data
     * @return Updated assignment entity
     */
    private PlannerItem updateExistingAssignment(PlannerItem existingAssignment, PlannerItem newAssignment) {
        // Update mutable fields
        existingAssignment.setAssignmentTitle(newAssignment.getAssignmentTitle());
        existingAssignment.setContextName(newAssignment.getContextName());
        existingAssignment.setDueAt(newAssignment.getDueAt());
        existingAssignment.setPointsPossible(newAssignment.getPointsPossible());
        existingAssignment.setCurrentGrade(newAssignment.getCurrentGrade());
        existingAssignment.setSubmitted(newAssignment.getSubmitted());
        existingAssignment.setMissing(newAssignment.getMissing());
        existingAssignment.setLate(newAssignment.getLate());
        existingAssignment.setGraded(newAssignment.getGraded());

        return existingAssignment;
    }

    /**
     * Updates the assignment status based on current time and submission state.
     * This method modifies the assignment object in place.
     * 
     * @param assignment PlannerItem to update status for
     */
    private void updateAssignmentStatus(PlannerItem assignment) {
        // The status calculation is handled by the getStatus() method in PlannerItem
        // This method can be used for any additional real-time status updates
        // Currently, the PlannerItem.getStatus() method handles all status logic
    }

    /**
     * Gets assignments that are overdue for a specific student.
     * 
     * @param studentId Canvas user ID of the student
     * @return List of overdue PlannerItem entities
     */
    public List<PlannerItem> getOverdueAssignments(Long studentId) {
        log.debug("Retrieving overdue assignments for student {}", studentId);
        
        List<PlannerItem> overdueAssignments = plannerItemRepository
                .findOverdueAssignments(studentId, LocalDateTime.now());
        
        log.debug("Found {} overdue assignments for student {}", overdueAssignments.size(), studentId);
        return overdueAssignments;
    }

    /**
     * Gets the count of assignments for a student within a date range.
     * 
     * @param studentId Canvas user ID of the student
     * @param startDate Start date for counting (inclusive)
     * @param endDate End date for counting (inclusive)
     * @return Number of assignments in the date range
     */
    public long getAssignmentCount(Long studentId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        return plannerItemRepository.countByStudentIdAndDueAtBetween(
                studentId, startDateTime, endDateTime);
    }
}