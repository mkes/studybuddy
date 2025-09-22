package com.studytracker.dto.mapper;

import com.studytracker.dto.AssignmentDto;
import com.studytracker.dto.StudentDto;
import com.studytracker.model.PlannerItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility class for converting Canvas API DTOs to internal domain models.
 * Handles data transformation between Canvas API responses and application entities.
 */
@Component
public class CanvasMapper {

    /**
     * Converts an AssignmentDto from Canvas API to a PlannerItem entity.
     * 
     * @param assignmentDto Canvas API assignment response
     * @param studentId Student ID to associate with the planner item
     * @return PlannerItem entity ready for persistence
     */
    public PlannerItem toPlannerItem(AssignmentDto assignmentDto, Long studentId) {
        if (assignmentDto == null) {
            return null;
        }

        PlannerItem.PlannerItemBuilder builder = PlannerItem.builder()
                .studentId(studentId)
                .plannableId(assignmentDto.getPlannableId())
                .assignmentTitle(assignmentDto.getAssignmentTitle())
                .contextName(assignmentDto.getContextName())
                .dueAt(assignmentDto.getDueAt())
                .pointsPossible(assignmentDto.getPointsPossible())
                .currentGrade(assignmentDto.getCurrentGrade());

        // Handle submission information if present
        if (assignmentDto.getSubmission() != null) {
            AssignmentDto.SubmissionDto submission = assignmentDto.getSubmission();
            builder.submitted(submission.getSubmitted() != null ? submission.getSubmitted() : false)
                   .missing(submission.getMissing() != null ? submission.getMissing() : false)
                   .late(submission.getLate() != null ? submission.getLate() : false)
                   .graded(submission.getGraded() != null ? submission.getGraded() : false);
            
            // Use submission score if current grade is not set
            if (assignmentDto.getCurrentGrade() == null && submission.getScore() != null) {
                builder.currentGrade(submission.getScore());
            }
        } else {
            // Default values when no submission info
            builder.submitted(false)
                   .missing(false)
                   .late(false)
                   .graded(false);
        }

        return builder.build();
    }

    /**
     * Converts a list of AssignmentDto objects to PlannerItem entities.
     * 
     * @param assignmentDtos List of Canvas API assignment responses
     * @param studentId Student ID to associate with all planner items
     * @return List of PlannerItem entities
     */
    public List<PlannerItem> toPlannerItems(List<AssignmentDto> assignmentDtos, Long studentId) {
        if (assignmentDtos == null) {
            return List.of();
        }

        return assignmentDtos.stream()
                .map(dto -> toPlannerItem(dto, studentId))
                .collect(Collectors.toList());
    }

    /**
     * Converts a PlannerItem entity back to an AssignmentDto.
     * Useful for API responses or data export.
     * 
     * @param plannerItem PlannerItem entity
     * @return AssignmentDto representation
     */
    public AssignmentDto toAssignmentDto(PlannerItem plannerItem) {
        if (plannerItem == null) {
            return null;
        }

        AssignmentDto.SubmissionDto submission = AssignmentDto.SubmissionDto.builder()
                .submitted(plannerItem.getSubmitted())
                .missing(plannerItem.getMissing())
                .late(plannerItem.getLate())
                .graded(plannerItem.getGraded())
                .score(plannerItem.getCurrentGrade())
                .build();

        return AssignmentDto.builder()
                .plannableId(plannerItem.getPlannableId())
                .studentId(plannerItem.getStudentId())
                .assignmentTitle(plannerItem.getAssignmentTitle())
                .contextName(plannerItem.getContextName())
                .dueAt(plannerItem.getDueAt())
                .pointsPossible(plannerItem.getPointsPossible())
                .currentGrade(plannerItem.getCurrentGrade())
                .submission(submission)
                .build();
    }

    /**
     * Converts a list of PlannerItem entities to AssignmentDto objects.
     * 
     * @param plannerItems List of PlannerItem entities
     * @return List of AssignmentDto objects
     */
    public List<AssignmentDto> toAssignmentDtos(List<PlannerItem> plannerItems) {
        if (plannerItems == null) {
            return List.of();
        }

        return plannerItems.stream()
                .map(this::toAssignmentDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing PlannerItem with data from an AssignmentDto.
     * Preserves the entity ID and timestamps while updating other fields.
     * 
     * @param existingItem Existing PlannerItem entity
     * @param assignmentDto New data from Canvas API
     * @return Updated PlannerItem entity
     */
    public PlannerItem updatePlannerItem(PlannerItem existingItem, AssignmentDto assignmentDto) {
        if (existingItem == null || assignmentDto == null) {
            return existingItem;
        }

        // Update fields that might change
        existingItem.setAssignmentTitle(assignmentDto.getAssignmentTitle());
        existingItem.setContextName(assignmentDto.getContextName());
        existingItem.setDueAt(assignmentDto.getDueAt());
        existingItem.setPointsPossible(assignmentDto.getPointsPossible());
        existingItem.setCurrentGrade(assignmentDto.getCurrentGrade());

        // Update submission information if present
        if (assignmentDto.getSubmission() != null) {
            AssignmentDto.SubmissionDto submission = assignmentDto.getSubmission();
            existingItem.setSubmitted(submission.getSubmitted() != null ? submission.getSubmitted() : false);
            existingItem.setMissing(submission.getMissing() != null ? submission.getMissing() : false);
            existingItem.setLate(submission.getLate() != null ? submission.getLate() : false);
            existingItem.setGraded(submission.getGraded() != null ? submission.getGraded() : false);
            
            // Use submission score if current grade is not set
            if (assignmentDto.getCurrentGrade() == null && submission.getScore() != null) {
                existingItem.setCurrentGrade(submission.getScore());
            }
        }

        return existingItem;
    }
}