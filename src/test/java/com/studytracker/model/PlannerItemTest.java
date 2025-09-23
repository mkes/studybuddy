package com.studytracker.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlannerItem entity validation and business logic.
 * Tests entity constraints, validation rules, and status calculation methods.
 */
@DisplayName("PlannerItem Entity Tests")
class PlannerItemTest {

    private Validator validator;
    private PlannerItem validPlannerItem;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Create a valid PlannerItem for testing
        validPlannerItem = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Math Homework Chapter 5")
                .contextName("Algebra I")
                .dueAt(LocalDateTime.now().plusDays(2))
                .pointsPossible(new BigDecimal("100.00"))
                .currentGrade(new BigDecimal("85.50"))
                .submitted(false)
                .missing(false)
                .late(false)
                .graded(false)
                .build();
    }

    @Test
    @DisplayName("Valid PlannerItem should pass validation")
    void testValidPlannerItem() {
        Set<ConstraintViolation<PlannerItem>> violations = validator.validate(validPlannerItem);
        assertTrue(violations.isEmpty(), "Valid PlannerItem should have no validation violations");
    }

    @Test
    @DisplayName("Student ID is required")
    void testStudentIdRequired() {
        validPlannerItem.setStudentId(null);
        
        Set<ConstraintViolation<PlannerItem>> violations = validator.validate(validPlannerItem);
        assertFalse(violations.isEmpty(), "Student ID should be required");
        
        boolean hasStudentIdViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("studentId"));
        assertTrue(hasStudentIdViolation, "Should have violation for studentId");
    }

    @Test
    @DisplayName("Plannable ID is required")
    void testPlannableIdRequired() {
        validPlannerItem.setPlannableId(null);
        
        Set<ConstraintViolation<PlannerItem>> violations = validator.validate(validPlannerItem);
        assertFalse(violations.isEmpty(), "Plannable ID should be required");
        
        boolean hasPlannableIdViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("plannableId"));
        assertTrue(hasPlannableIdViolation, "Should have violation for plannableId");
    }

    @Test
    @DisplayName("Assignment title is required")
    void testAssignmentTitleRequired() {
        validPlannerItem.setAssignmentTitle(null);
        
        Set<ConstraintViolation<PlannerItem>> violations = validator.validate(validPlannerItem);
        assertFalse(violations.isEmpty(), "Assignment title should be required");
        
        boolean hasTitleViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("assignmentTitle"));
        assertTrue(hasTitleViolation, "Should have violation for assignmentTitle");
    }

    @Test
    @DisplayName("Assignment title cannot exceed 500 characters")
    void testAssignmentTitleMaxLength() {
        String longTitle = "A".repeat(501); // 501 characters
        validPlannerItem.setAssignmentTitle(longTitle);
        
        Set<ConstraintViolation<PlannerItem>> violations = validator.validate(validPlannerItem);
        assertFalse(violations.isEmpty(), "Assignment title over 500 characters should be invalid");
        
        boolean hasSizeViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("assignmentTitle") 
                          && v.getMessage().contains("500 characters"));
        assertTrue(hasSizeViolation, "Should have size violation for assignmentTitle");
    }

    @Test
    @DisplayName("Context name cannot exceed 200 characters")
    void testContextNameMaxLength() {
        String longContextName = "B".repeat(201); // 201 characters
        validPlannerItem.setContextName(longContextName);
        
        Set<ConstraintViolation<PlannerItem>> violations = validator.validate(validPlannerItem);
        assertFalse(violations.isEmpty(), "Context name over 200 characters should be invalid");
        
        boolean hasSizeViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("contextName") 
                          && v.getMessage().contains("200 characters"));
        assertTrue(hasSizeViolation, "Should have size violation for contextName");
    }

    @Test
    @DisplayName("Boolean fields should have default values")
    void testBooleanDefaults() {
        PlannerItem item = PlannerItem.builder()
                .studentId(12345L)
                .plannableId(67890L)
                .assignmentTitle("Test Assignment")
                .build();
        
        assertFalse(item.getSubmitted(), "Submitted should default to false");
        assertFalse(item.getMissing(), "Missing should default to false");
        assertFalse(item.getLate(), "Late should default to false");
        assertFalse(item.getGraded(), "Graded should default to false");
    }

    @Test
    @DisplayName("Status calculation - submitted and graded")
    void testStatusSubmittedAndGraded() {
        validPlannerItem.setSubmitted(true);
        validPlannerItem.setGraded(true);
        
        assertEquals("submitted", validPlannerItem.getStatus(), 
                "Status should be 'submitted' when submitted and graded");
    }

    @Test
    @DisplayName("Status calculation - missing")
    void testStatusMissing() {
        validPlannerItem.setMissing(true);
        
        assertEquals("missing", validPlannerItem.getStatus(), 
                "Status should be 'missing' when marked as missing");
    }

    @Test
    @DisplayName("Status calculation - late")
    void testStatusLate() {
        validPlannerItem.setLate(true);
        
        assertEquals("late", validPlannerItem.getStatus(), 
                "Status should be 'late' when marked as late");
    }

    @Test
    @DisplayName("Status calculation - overdue")
    void testStatusOverdue() {
        validPlannerItem.setDueAt(LocalDateTime.now().minusDays(1)); // Past due date
        validPlannerItem.setSubmitted(false);
        validPlannerItem.setGraded(false);
        validPlannerItem.setCurrentGrade(null);
        
        assertEquals("overdue", validPlannerItem.getStatus(), 
                "Status should be 'overdue' when past due date and not submitted");
    }

    @Test
    @DisplayName("Status calculation - not overdue when graded with score")
    void testStatusNotOverdueWhenGradedWithScore() {
        validPlannerItem.setDueAt(LocalDateTime.now().minusDays(1)); // Past due date
        validPlannerItem.setSubmitted(false); // Not submitted
        validPlannerItem.setGraded(true); // But graded
        validPlannerItem.setCurrentGrade(new BigDecimal("85.0")); // With a score
        validPlannerItem.setMissing(false);
        validPlannerItem.setLate(false);
        
        assertEquals("pending", validPlannerItem.getStatus(), 
                "Status should be 'pending' (not overdue) when graded with score, even if past due date");
    }

    @Test
    @DisplayName("Status calculation - overdue when graded but no score")
    void testStatusOverdueWhenGradedButNoScore() {
        validPlannerItem.setDueAt(LocalDateTime.now().minusDays(1)); // Past due date
        validPlannerItem.setSubmitted(false); // Not submitted
        validPlannerItem.setGraded(true); // Graded
        validPlannerItem.setCurrentGrade(null); // But no score
        validPlannerItem.setMissing(false);
        validPlannerItem.setLate(false);
        
        assertEquals("overdue", validPlannerItem.getStatus(), 
                "Status should be 'overdue' when graded but no score available");
    }

    @Test
    @DisplayName("Status calculation - pending")
    void testStatusPending() {
        validPlannerItem.setDueAt(LocalDateTime.now().plusDays(1)); // Future due date
        validPlannerItem.setSubmitted(false);
        validPlannerItem.setMissing(false);
        validPlannerItem.setLate(false);
        
        assertEquals("pending", validPlannerItem.getStatus(), 
                "Status should be 'pending' when not submitted and not past due");
    }

    @Test
    @DisplayName("Status calculation - pending when no due date")
    void testStatusPendingNoDueDate() {
        validPlannerItem.setDueAt(null);
        validPlannerItem.setSubmitted(false);
        validPlannerItem.setMissing(false);
        validPlannerItem.setLate(false);
        
        assertEquals("pending", validPlannerItem.getStatus(), 
                "Status should be 'pending' when no due date is set");
    }

    @Test
    @DisplayName("Status badge class - success for submitted")
    void testStatusBadgeClassSuccess() {
        validPlannerItem.setSubmitted(true);
        validPlannerItem.setGraded(true);
        
        assertEquals("badge-success", validPlannerItem.getStatusBadgeClass(), 
                "Badge class should be 'badge-success' for submitted assignments");
    }

    @Test
    @DisplayName("Status badge class - danger for missing and overdue")
    void testStatusBadgeClassDanger() {
        // Test missing
        validPlannerItem.setMissing(true);
        assertEquals("badge-danger", validPlannerItem.getStatusBadgeClass(), 
                "Badge class should be 'badge-danger' for missing assignments");
        
        // Test overdue
        validPlannerItem.setMissing(false);
        validPlannerItem.setDueAt(LocalDateTime.now().minusDays(1));
        validPlannerItem.setSubmitted(false);
        assertEquals("badge-danger", validPlannerItem.getStatusBadgeClass(), 
                "Badge class should be 'badge-danger' for overdue assignments");
    }

    @Test
    @DisplayName("Status badge class - warning for late")
    void testStatusBadgeClassWarning() {
        validPlannerItem.setLate(true);
        
        assertEquals("badge-warning", validPlannerItem.getStatusBadgeClass(), 
                "Badge class should be 'badge-warning' for late assignments");
    }

    @Test
    @DisplayName("Status badge class - primary for pending")
    void testStatusBadgeClassPrimary() {
        validPlannerItem.setDueAt(LocalDateTime.now().plusDays(1));
        validPlannerItem.setSubmitted(false);
        validPlannerItem.setMissing(false);
        validPlannerItem.setLate(false);
        
        assertEquals("badge-primary", validPlannerItem.getStatusBadgeClass(), 
                "Badge class should be 'badge-primary' for pending assignments");
    }

    @Test
    @DisplayName("Builder pattern works correctly")
    void testBuilderPattern() {
        PlannerItem item = PlannerItem.builder()
                .studentId(999L)
                .plannableId(888L)
                .assignmentTitle("Test Builder")
                .contextName("Test Course")
                .pointsPossible(new BigDecimal("50.00"))
                .submitted(true)
                .build();
        
        assertEquals(999L, item.getStudentId());
        assertEquals(888L, item.getPlannableId());
        assertEquals("Test Builder", item.getAssignmentTitle());
        assertEquals("Test Course", item.getContextName());
        assertEquals(new BigDecimal("50.00"), item.getPointsPossible());
        assertTrue(item.getSubmitted());
        
        // Check defaults are still applied
        assertFalse(item.getMissing());
        assertFalse(item.getLate());
        assertFalse(item.getGraded());
    }

    @Test
    @DisplayName("Equals and hashCode work correctly")
    void testEqualsAndHashCode() {
        PlannerItem item1 = PlannerItem.builder()
                .studentId(123L)
                .plannableId(456L)
                .assignmentTitle("Test")
                .build();
        
        PlannerItem item2 = PlannerItem.builder()
                .studentId(123L)
                .plannableId(456L)
                .assignmentTitle("Test")
                .build();
        
        assertEquals(item1, item2, "Items with same data should be equal");
        assertEquals(item1.hashCode(), item2.hashCode(), "Items with same data should have same hash code");
    }
}