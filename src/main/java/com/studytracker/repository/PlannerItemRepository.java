package com.studytracker.repository;

import com.studytracker.model.PlannerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for PlannerItem entity operations.
 * Provides data access methods for assignment management with custom queries
 * for date filtering and bulk operations for efficient synchronization.
 */
@Repository
public interface PlannerItemRepository extends JpaRepository<PlannerItem, Long> {

    /**
     * Finds planner items for a specific student within a date range,
     * ordered by due date in descending order (newest first).
     * 
     * @param studentId the ID of the student
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return List of PlannerItem objects ordered by due date descending
     */
    List<PlannerItem> findByStudentIdAndDueAtBetweenOrderByDueAtDesc(
            Long studentId, 
            LocalDateTime startDate, 
            LocalDateTime endDate
    );

    /**
     * Finds all planner items for a specific student,
     * ordered by due date in descending order.
     * 
     * @param studentId the ID of the student
     * @return List of PlannerItem objects ordered by due date descending
     */
    List<PlannerItem> findByStudentIdOrderByDueAtDesc(Long studentId);

    /**
     * Finds planner items by student ID and plannable ID.
     * Used to check for existing assignments during sync operations.
     * 
     * @param studentId the ID of the student
     * @param plannableId the ID of the plannable (assignment)
     * @return PlannerItem if found, null otherwise
     */
    PlannerItem findByStudentIdAndPlannableId(Long studentId, Long plannableId);

    /**
     * Deletes all planner items for a specific student and plannable ID.
     * Used for cleanup during assignment synchronization.
     * 
     * @param studentId the ID of the student
     * @param plannableId the ID of the plannable (assignment)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PlannerItem p WHERE p.studentId = :studentId AND p.plannableId = :plannableId")
    void deleteByStudentIdAndPlannableId(@Param("studentId") Long studentId, @Param("plannableId") Long plannableId);

    /**
     * Deletes all planner items for a specific student.
     * Used for complete data refresh during synchronization.
     * 
     * @param studentId the ID of the student
     */
    @Modifying
    @Transactional
    void deleteByStudentId(Long studentId);

    /**
     * Counts the number of assignments for a student within a date range.
     * Used for performance monitoring and pagination.
     * 
     * @param studentId the ID of the student
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return count of assignments in the date range
     */
    long countByStudentIdAndDueAtBetween(
            Long studentId, 
            LocalDateTime startDate, 
            LocalDateTime endDate
    );

    /**
     * Finds assignments that are overdue (due date passed but not submitted).
     * Used for identifying assignments that need attention.
     * 
     * @param studentId the ID of the student
     * @param currentTime the current timestamp
     * @return List of overdue PlannerItem objects
     */
    @Query("SELECT p FROM PlannerItem p WHERE p.studentId = :studentId " +
           "AND p.dueAt < :currentTime AND p.submitted = false " +
           "ORDER BY p.dueAt DESC")
    List<PlannerItem> findOverdueAssignments(
            @Param("studentId") Long studentId, 
            @Param("currentTime") LocalDateTime currentTime
    );
}