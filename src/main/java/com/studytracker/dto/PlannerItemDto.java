package com.studytracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO representing a Canvas planner item from the /planner/items API endpoint.
 * This can be either an assignment or an announcement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlannerItemDto {

    @JsonProperty("context_type")
    private String contextType;

    @JsonProperty("course_id")
    private Long courseId;

    @JsonProperty("plannable_id")
    private Long plannableId;

    @JsonProperty("planner_override")
    private Object plannerOverride;

    @JsonProperty("plannable_type")
    private String plannableType;

    @JsonProperty("new_activity")
    private Boolean newActivity;

    @JsonProperty("submissions")
    @JsonDeserialize(using = SubmissionDeserializer.class)
    private SubmissionDto submissions;

    @JsonProperty("plannable_date")
    private LocalDateTime plannableDate;

    @JsonProperty("plannable")
    private PlannableDto plannable;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("context_name")
    private String contextName;

    @JsonProperty("context_image")
    private String contextImage;

    /**
     * Nested DTO for the plannable object (assignment or announcement details)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlannableDto {
        
        @JsonProperty("id")
        private Long id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;

        @JsonProperty("points_possible")
        private Double pointsPossible;

        @JsonProperty("due_at")
        private LocalDateTime dueAt;

        // For announcements
        @JsonProperty("unread_count")
        private Integer unreadCount;

        @JsonProperty("read_state")
        private String readState;
    }

    /**
     * Nested DTO for submission information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmissionDto {
        
        @JsonProperty("submitted")
        private Boolean submitted;

        @JsonProperty("excused")
        private Boolean excused;

        @JsonProperty("graded")
        private Boolean graded;

        @JsonProperty("posted_at")
        private LocalDateTime postedAt;

        @JsonProperty("late")
        private Boolean late;

        @JsonProperty("missing")
        private Boolean missing;

        @JsonProperty("needs_grading")
        private Boolean needsGrading;

        @JsonProperty("has_feedback")
        private Boolean hasFeedback;

        @JsonProperty("redo_request")
        private Boolean redoRequest;
    }
}