package com.studytracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for sync API response containing assignment data with grades.
 * Used by the internal sync API to return formatted assignment information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResponseDto {

    /**
     * Student ID for which assignments were synced
     */
    @JsonProperty("student_id")
    private Long studentId;

    /**
     * Number of assignments synchronized
     */
    @JsonProperty("assignments_count")
    private Integer assignmentsCount;

    /**
     * Timestamp when sync was completed
     */
    @JsonProperty("sync_timestamp")
    private LocalDateTime syncTimestamp;

    /**
     * List of synchronized assignments
     */
    @JsonProperty("assignments")
    private List<AssignmentSyncDto> assignments;

    /**
     * Sync operation status
     */
    @JsonProperty("status")
    private String status;

    /**
     * Error message if sync failed
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * DTO representing a single assignment in sync response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentSyncDto {

        /**
         * Assignment ID from Canvas
         */
        @JsonProperty("assignment_id")
        private Long assignmentId;

        /**
         * Assignment title
         */
        @JsonProperty("title")
        private String title;

        /**
         * Course name
         */
        @JsonProperty("course_name")
        private String courseName;

        /**
         * Due date and time
         */
        @JsonProperty("due_at")
        private LocalDateTime dueAt;

        /**
         * Points possible
         */
        @JsonProperty("points_possible")
        private BigDecimal pointsPossible;

        /**
         * Current grade/score
         */
        @JsonProperty("current_grade")
        private BigDecimal currentGrade;

        /**
         * Assignment status (submitted, missing, late, overdue, pending)
         */
        @JsonProperty("status")
        private String status;

        /**
         * Whether assignment is submitted
         */
        @JsonProperty("submitted")
        private Boolean submitted;

        /**
         * Whether assignment is missing
         */
        @JsonProperty("missing")
        private Boolean missing;

        /**
         * Whether assignment was submitted late
         */
        @JsonProperty("late")
        private Boolean late;

        /**
         * Whether assignment is graded
         */
        @JsonProperty("graded")
        private Boolean graded;
    }
}