package com.studytracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing an assignment from Canvas API planner items response.
 * Used for deserializing Canvas API /planner/items endpoint responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDto {

    /**
     * Canvas plannable ID (assignment ID)
     */
    @JsonProperty("plannable_id")
    private Long plannableId;

    /**
     * Student ID this assignment belongs to
     */
    @JsonProperty("user_id")
    private Long studentId;

    /**
     * Assignment title/name
     */
    @JsonProperty("plannable_title")
    private String assignmentTitle;

    /**
     * Course or context name
     */
    @JsonProperty("context_name")
    private String contextName;

    /**
     * Assignment due date and time
     */
    @JsonProperty("plannable_date")
    private LocalDateTime dueAt;

    /**
     * Maximum points possible for the assignment
     */
    @JsonProperty("points_possible")
    private BigDecimal pointsPossible;

    /**
     * Current grade/score for the assignment
     */
    @JsonProperty("current_grade")
    private BigDecimal currentGrade;

    /**
     * Whether the assignment has been submitted
     */
    @JsonProperty("submissions")
    private SubmissionDto submission;

    /**
     * Nested DTO for submission information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionDto {
        
        /**
         * Whether the assignment was submitted
         */
        private Boolean submitted;

        /**
         * Whether the assignment is marked as missing
         */
        private Boolean missing;

        /**
         * Whether the assignment was submitted late
         */
        private Boolean late;

        /**
         * Whether the assignment has been graded
         */
        private Boolean graded;

        /**
         * Current score for the assignment
         */
        private BigDecimal score;

        /**
         * Grade received (could be letter grade or points)
         */
        private String grade;

        /**
         * Submission workflow state
         */
        @JsonProperty("workflow_state")
        private String workflowState;
    }
}