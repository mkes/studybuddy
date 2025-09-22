package com.studytracker.controller;

import com.studytracker.dto.StudentDto;
import com.studytracker.model.PlannerItem;
import com.studytracker.service.CanvasApiService;
import com.studytracker.service.AssignmentService;
import com.studytracker.exception.CanvasApiException;
import com.studytracker.exception.InvalidTokenException;
import com.studytracker.exception.InsufficientPermissionsException;
import com.studytracker.exception.CanvasUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for handling student selection and dashboard functionality.
 * Manages the display of observed students and navigation to individual student dashboards.
 */
@Slf4j
@Controller
public class StudentController {

    @Autowired
    private CanvasApiService canvasApiService;
    
    @Autowired
    private AssignmentService assignmentService;

    /**
     * Display the student selection page with a dropdown of observed students.
     * Retrieves all students that the authenticated user observes from Canvas API.
     * 
     * @param session HTTP session containing the Canvas API token
     * @param model Thymeleaf model for passing data to the view
     * @param redirectAttributes For flash messages on redirect
     * @return students template name or redirect to login if not authenticated
     */
    @GetMapping("/students")
    public String studentsPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        // Check if user is authenticated
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            log.warn("Unauthenticated access attempt to students page");
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }

        try {
            // Retrieve observed students from Canvas API
            log.debug("Fetching observed students for authenticated user");
            List<StudentDto> students = canvasApiService.getObservedStudents(canvasToken);
            
            if (students.isEmpty()) {
                log.info("No observed students found for user");
                model.addAttribute("message", "No students found. Make sure you have observer permissions for at least one student in Canvas.");
                model.addAttribute("messageType", "info");
            } else {
                log.debug("Successfully retrieved {} observed students", students.size());
            }
            
            model.addAttribute("students", students);
            return "students";
            
        } catch (InvalidTokenException e) {
            log.warn("Invalid token encountered while fetching students", e);
            session.invalidate(); // Clear invalid session
            redirectAttributes.addFlashAttribute("error", "Your Canvas token has expired. Please log in again.");
            return "redirect:/";
            
        } catch (InsufficientPermissionsException e) {
            log.warn("Insufficient permissions to access observed students", e);
            model.addAttribute("error", "Your Canvas account does not have observer permissions. Please contact your Canvas administrator.");
            model.addAttribute("students", List.of()); // Empty list to prevent template errors
            return "students";
            
        } catch (CanvasUnavailableException e) {
            log.error("Canvas API unavailable while fetching students", e);
            model.addAttribute("error", "Canvas is currently unavailable. Please try again later.");
            model.addAttribute("students", List.of()); // Empty list to prevent template errors
            return "students";
            
        } catch (CanvasApiException e) {
            log.error("Canvas API error while fetching students", e);
            model.addAttribute("error", "Unable to retrieve students from Canvas. Please try again later.");
            model.addAttribute("students", List.of()); // Empty list to prevent template errors
            return "students";
            
        } catch (Exception e) {
            log.error("Unexpected error while fetching students", e);
            model.addAttribute("error", "An unexpected error occurred. Please try again later.");
            model.addAttribute("students", List.of()); // Empty list to prevent template errors
            return "students";
        }
    }

    /**
     * Display the dashboard for a specific student with assignments.
     * Automatically syncs assignments from Canvas API and displays them with status badges.
     * 
     * @param studentId Canvas user ID of the student
     * @param session HTTP session containing the Canvas API token
     * @param model Thymeleaf model for passing data to the view
     * @param redirectAttributes For flash messages on redirect
     * @return student dashboard template name or redirect if not authenticated
     */
    @GetMapping("/students/{studentId}")
    public String studentDashboard(@PathVariable Long studentId, 
                                 HttpSession session, 
                                 Model model, 
                                 RedirectAttributes redirectAttributes) {
        // Check if user is authenticated
        String canvasToken = (String) session.getAttribute("canvasToken");
        if (canvasToken == null || canvasToken.trim().isEmpty()) {
            log.warn("Unauthenticated access attempt to student dashboard for student {}", studentId);
            redirectAttributes.addFlashAttribute("error", "Please log in to access StudyTracker.");
            return "redirect:/";
        }

        log.debug("Student dashboard requested for student ID: {}", studentId);
        
        try {
            // Automatically sync assignments from Canvas API
            log.debug("Starting automatic assignment sync for student {}", studentId);
            List<PlannerItem> assignments = assignmentService.syncAssignments(studentId, canvasToken);
            
            // Add assignment status information for each assignment
            assignments.forEach(assignment -> {
                String status = assignmentService.calculateAssignmentStatus(assignment);
                assignment.setStatus(status);
                assignment.setStatusBadgeClass(assignmentService.getStatusBadgeClass(status));
            });
            
            log.info("Successfully loaded dashboard for student {} with {} assignments", 
                    studentId, assignments.size());
            
            // Get student information for display
            List<StudentDto> students = canvasApiService.getObservedStudents(canvasToken);
            StudentDto currentStudent = students.stream()
                    .filter(student -> student.getId().equals(studentId))
                    .findFirst()
                    .orElse(null);
            
            model.addAttribute("studentId", studentId);
            model.addAttribute("student", currentStudent);
            model.addAttribute("assignments", assignments);
            model.addAttribute("assignmentCount", assignments.size());
            
            if (assignments.isEmpty()) {
                model.addAttribute("message", "No assignments found for the current date range (Aug 1 - Sep 30, 2025).");
                model.addAttribute("messageType", "info");
            }
            
            return "student-dashboard";
            
        } catch (InvalidTokenException e) {
            log.warn("Invalid token encountered while loading dashboard for student {}", studentId, e);
            session.invalidate(); // Clear invalid session
            redirectAttributes.addFlashAttribute("error", "Your Canvas token has expired. Please log in again.");
            return "redirect:/";
            
        } catch (InsufficientPermissionsException e) {
            log.warn("Insufficient permissions to access student {} dashboard", studentId, e);
            redirectAttributes.addFlashAttribute("error", "You do not have permission to view this student's information.");
            return "redirect:/students";
            
        } catch (CanvasUnavailableException e) {
            log.error("Canvas API unavailable while loading dashboard for student {}", studentId, e);
            model.addAttribute("error", "Canvas is currently unavailable. Please try again later.");
            model.addAttribute("studentId", studentId);
            model.addAttribute("assignments", List.of()); // Empty list to prevent template errors
            return "student-dashboard";
            
        } catch (CanvasApiException e) {
            log.error("Canvas API error while loading dashboard for student {}", studentId, e);
            model.addAttribute("error", "Unable to retrieve assignment data from Canvas. Please try again later.");
            model.addAttribute("studentId", studentId);
            model.addAttribute("assignments", List.of()); // Empty list to prevent template errors
            return "student-dashboard";
            
        } catch (Exception e) {
            log.error("Unexpected error while loading dashboard for student {}", studentId, e);
            model.addAttribute("error", "An unexpected error occurred while loading the dashboard. Please try again later.");
            model.addAttribute("studentId", studentId);
            model.addAttribute("assignments", List.of()); // Empty list to prevent template errors
            return "student-dashboard";
        }
    }
}