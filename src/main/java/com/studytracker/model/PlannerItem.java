package com.studytracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a planner item (assignment) from Canvas LMS.
 * This entity stores assignment information for students including grades,
 * due dates, and submission status.
 */
@Entity
@Table(name = "planner_items", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_planner_items_student_plannable", 
           columnNames = {"student_id", "plannable_id"}
       ),
       indexes = {
           @Index(name = "idx_planner_items_student_due", 
                  columnList = "student_id, due_at"),
           @Index(name = "idx_planner_items_student", 
                  columnList = "student_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @NotNull
    @Column(name = "plannable_id", nullable = false)
    private Long plannableId;

    @NotNull
    @Size(max = 500, message = "Assignment title cannot exceed 500 characters")
    @Column(name = "assignment_title", length = 500, nullable = false)
    private String assignmentTitle;

    @Size(max = 200, message = "Context name cannot exceed 200 characters")
    @Column(name = "context_name", length = 200)
    private String contextName;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "points_possible", precision = 10, scale = 2)
    private BigDecimal pointsPossible;

    @Column(name = "current_grade", precision = 10, scale = 2)
    private BigDecimal currentGrade;

    @NotNull
    @Builder.Default
    @Column(name = "submitted", nullable = false)
    private Boolean submitted = false;

    @NotNull
    @Builder.Default
    @Column(name = "missing", nullable = false)
    private Boolean missing = false;

    @NotNull
    @Builder.Default
    @Column(name = "late", nullable = false)
    private Boolean late = false;

    @NotNull
    @Builder.Default
    @Column(name = "graded", nullable = false)
    private Boolean graded = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Calculates the assignment status based on submission and grading flags.
     * 
     * @return String representing the status: "submitted", "missing", "late", "overdue", or "pending"
     */
    public String getStatus() {
        if (submitted && graded) {
            return "submitted";
        } else if (missing) {
            return "missing";
        } else if (late) {
            return "late";
        } else if (dueAt != null && dueAt.isBefore(LocalDateTime.now()) && !submitted) {
            return "overdue";
        } else {
            return "pending";
        }
    }

    /**
     * Gets the CSS class for status badge styling.
     * 
     * @return String CSS class name for the status
     */
    public String getStatusBadgeClass() {
        return switch (getStatus()) {
            case "submitted" -> "badge-success";
            case "missing", "overdue" -> "badge-danger";
            case "late" -> "badge-warning";
            case "pending" -> "badge-primary";
            default -> "badge-secondary";
        };
    }
}