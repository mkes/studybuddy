package com.studytracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Canvas API submissions response.
 * Maps to Canvas API /api/v1/courses/{courseId}/students/submissions endpoint response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionDto {
    
    private Long id;
    
    @JsonProperty("assignment_id")
    private Long assignmentId;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("entered_score")
    private BigDecimal enteredScore;
    
    @JsonProperty("entered_grade")
    private String enteredGrade;
    
    private String grade;
    private BigDecimal score;
    
    @JsonProperty("workflow_state")
    private String workflowState;
    
    @JsonProperty("submitted_at")
    private LocalDateTime submittedAt;
    
    @JsonProperty("graded_at")
    private LocalDateTime gradedAt;
    
    @JsonProperty("grade_matches_current_submission")
    private Boolean gradeMatchesCurrentSubmission;
    
    private Boolean late;
    private Boolean missing;
    private Boolean excused;
    
    @JsonProperty("seconds_late")
    private Long secondsLate;
    
    @JsonProperty("cached_due_date")
    private LocalDateTime cachedDueDate;
    
    /**
     * Checks if the submission has been graded.
     * 
     * @return true if submission is graded
     */
    public boolean isGraded() {
        return "graded".equals(workflowState) && enteredScore != null;
    }
    
    /**
     * Checks if the submission has been submitted.
     * 
     * @return true if submission has been submitted
     */
    public boolean isSubmitted() {
        return submittedAt != null || "submitted".equals(workflowState);
    }
    
    /**
     * Gets the effective grade score, preferring entered_score over score.
     * 
     * @return the most accurate grade score
     */
    public BigDecimal getEffectiveScore() {
        return enteredScore != null ? enteredScore : score;
    }
    
    /**
     * Gets the effective grade, preferring entered_grade over grade.
     * 
     * @return the most accurate grade string
     */
    public String getEffectiveGrade() {
        return enteredGrade != null && !enteredGrade.trim().isEmpty() ? enteredGrade : grade;
    }
}