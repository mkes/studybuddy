package com.studytracker.controller;

import com.studytracker.dto.StudentDto;
import com.studytracker.model.PlannerItem;
import com.studytracker.service.CanvasApiService;
import com.studytracker.service.AssignmentService;
import com.studytracker.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for StudentController.
 * Tests student retrieval, selection flow, and error handling scenarios.
 */
@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private CanvasApiService canvasApiService;
    
    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private StudentController studentController;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        // Configure MockMvc with view resolver to prevent view resolution issues
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setSuffix(".html");
        
        mockMvc = MockMvcBuilders.standaloneSetup(studentController)
                .setViewResolvers(viewResolver)
                .build();
        
        session = new MockHttpSession();
    }

    @Test
    void studentsPage_WithValidToken_ShouldDisplayStudents() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        session.setAttribute("canvasToken", validToken);
        
        List<StudentDto> mockStudents = Arrays.asList(
            StudentDto.builder()
                .id(1001L)
                .name("John Doe")
                .shortName("John")
                .sortableName("Doe, John")
                .avatarUrl("https://example.com/avatar1.jpg")
                .build(),
            StudentDto.builder()
                .id(1002L)
                .name("Jane Smith")
                .shortName("Jane")
                .sortableName("Smith, Jane")
                .avatarUrl("https://example.com/avatar2.jpg")
                .build()
        );
        
        when(canvasApiService.getObservedStudents(validToken)).thenReturn(mockStudents);

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("students", mockStudents))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeDoesNotExist("message"));

        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentsPage_WithNoToken_ShouldRedirectToLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));

        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentsPage_WithEmptyToken_ShouldRedirectToLogin() throws Exception {
        // Arrange
        session.setAttribute("canvasToken", "");

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));

        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentsPage_WithWhitespaceToken_ShouldRedirectToLogin() throws Exception {
        // Arrange
        session.setAttribute("canvasToken", "   ");

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));

        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentsPage_WithEmptyStudentList_ShouldShowInfoMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        session.setAttribute("canvasToken", validToken);
        
        when(canvasApiService.getObservedStudents(validToken)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("students", List.of()))
                .andExpect(model().attribute("message", "No students found. Make sure you have observer permissions for at least one student in Canvas."))
                .andExpect(model().attribute("messageType", "info"));

        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentsPage_WithInvalidToken_ShouldInvalidateSessionAndRedirect() throws Exception {
        // Arrange
        String invalidToken = "invalid_canvas_token";
        session.setAttribute("canvasToken", invalidToken);
        
        when(canvasApiService.getObservedStudents(invalidToken))
                .thenThrow(new InvalidTokenException("Canvas API token is invalid or expired"));

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Your Canvas token has expired. Please log in again."));

        verify(canvasApiService).getObservedStudents(invalidToken);
    }

    @Test
    void studentsPage_WithInsufficientPermissions_ShouldShowErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        session.setAttribute("canvasToken", validToken);
        
        when(canvasApiService.getObservedStudents(validToken))
                .thenThrow(new InsufficientPermissionsException("Insufficient permissions to access Canvas data"));

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("students", List.of()))
                .andExpect(model().attribute("error", "Your Canvas account does not have observer permissions. Please contact your Canvas administrator."));

        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentsPage_WithCanvasUnavailable_ShouldShowErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        session.setAttribute("canvasToken", validToken);
        
        when(canvasApiService.getObservedStudents(validToken))
                .thenThrow(new CanvasUnavailableException("Canvas API is currently unavailable"));

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("students", List.of()))
                .andExpect(model().attribute("error", "Canvas is currently unavailable. Please try again later."));

        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentsPage_WithCanvasApiException_ShouldShowErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        session.setAttribute("canvasToken", validToken);
        
        when(canvasApiService.getObservedStudents(validToken))
                .thenThrow(new CanvasApiException("Canvas API error", 500));

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("students", List.of()))
                .andExpect(model().attribute("error", "Unable to retrieve students from Canvas. Please try again later."));

        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentsPage_WithUnexpectedException_ShouldShowGenericErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        session.setAttribute("canvasToken", validToken);
        
        when(canvasApiService.getObservedStudents(validToken))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(get("/students").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attribute("students", List.of()))
                .andExpect(model().attribute("error", "An unexpected error occurred. Please try again later."));

        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentDashboard_WithValidTokenAndAssignments_ShouldDisplayDashboardWithAssignments() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        List<StudentDto> mockStudents = Arrays.asList(
            StudentDto.builder()
                .id(1001L)
                .name("John Doe")
                .shortName("John")
                .sortableName("Doe, John")
                .avatarUrl("https://example.com/avatar1.jpg")
                .build()
        );
        
        List<PlannerItem> mockAssignments = Arrays.asList(
            PlannerItem.builder()
                .id(1L)
                .studentId(studentId)
                .plannableId(2001L)
                .assignmentTitle("Math Homework")
                .contextName("Mathematics 101")
                .dueAt(LocalDateTime.now().plusDays(2))
                .pointsPossible(new BigDecimal("100"))
                .currentGrade(new BigDecimal("85"))
                .submitted(true)
                .graded(true)
                .build(),
            PlannerItem.builder()
                .id(2L)
                .studentId(studentId)
                .plannableId(2002L)
                .assignmentTitle("Science Project")
                .contextName("Science 101")
                .dueAt(LocalDateTime.now().plusDays(5))
                .pointsPossible(new BigDecimal("50"))
                .submitted(false)
                .missing(false)
                .build()
        );
        
        when(assignmentService.syncAssignments(studentId, validToken)).thenReturn(mockAssignments);
        when(canvasApiService.getObservedStudents(validToken)).thenReturn(mockStudents);
        when(assignmentService.calculateAssignmentStatus(any(PlannerItem.class)))
            .thenReturn("submitted")
            .thenReturn("pending");
        when(assignmentService.getStatusBadgeClass("submitted")).thenReturn("badge-success");
        when(assignmentService.getStatusBadgeClass("pending")).thenReturn("badge-primary");

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("studentId", studentId))
                .andExpect(model().attribute("student", mockStudents.get(0)))
                .andExpect(model().attribute("assignments", mockAssignments))
                .andExpect(model().attribute("assignmentCount", 2))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeDoesNotExist("message"));

        verify(assignmentService).syncAssignments(studentId, validToken);
        verify(canvasApiService).getObservedStudents(validToken);
        verify(assignmentService, times(2)).calculateAssignmentStatus(any(PlannerItem.class));
        verify(assignmentService, times(2)).getStatusBadgeClass(anyString());
    }

    @Test
    void studentDashboard_WithValidTokenButNoAssignments_ShouldDisplayEmptyState() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        List<StudentDto> mockStudents = Arrays.asList(
            StudentDto.builder()
                .id(1001L)
                .name("John Doe")
                .shortName("John")
                .sortableName("Doe, John")
                .avatarUrl("https://example.com/avatar1.jpg")
                .build()
        );
        
        when(assignmentService.syncAssignments(studentId, validToken)).thenReturn(List.of());
        when(canvasApiService.getObservedStudents(validToken)).thenReturn(mockStudents);

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("studentId", studentId))
                .andExpect(model().attribute("student", mockStudents.get(0)))
                .andExpect(model().attribute("assignments", List.of()))
                .andExpect(model().attribute("assignmentCount", 0))
                .andExpect(model().attribute("message", "No assignments found for the current date range (Aug 1 - Sep 30, 2025)."))
                .andExpect(model().attribute("messageType", "info"));

        verify(assignmentService).syncAssignments(studentId, validToken);
        verify(canvasApiService).getObservedStudents(validToken);
    }

    @Test
    void studentDashboard_WithNoToken_ShouldRedirectToLogin() throws Exception {
        // Arrange
        Long studentId = 1001L;

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));

        verify(assignmentService, never()).syncAssignments(anyLong(), anyString());
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithEmptyToken_ShouldRedirectToLogin() throws Exception {
        // Arrange
        Long studentId = 1001L;
        session.setAttribute("canvasToken", "");

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Please log in to access StudyTracker."));

        verify(assignmentService, never()).syncAssignments(anyLong(), anyString());
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithInvalidToken_ShouldInvalidateSessionAndRedirect() throws Exception {
        // Arrange
        String invalidToken = "invalid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", invalidToken);
        
        when(assignmentService.syncAssignments(studentId, invalidToken))
                .thenThrow(new InvalidTokenException("Canvas API token is invalid or expired"));

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Your Canvas token has expired. Please log in again."));

        verify(assignmentService).syncAssignments(studentId, invalidToken);
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithInsufficientPermissions_ShouldRedirectToStudents() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        when(assignmentService.syncAssignments(studentId, validToken))
                .thenThrow(new InsufficientPermissionsException("Insufficient permissions to access student data"));

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attribute("error", "You do not have permission to view this student's information."));

        verify(assignmentService).syncAssignments(studentId, validToken);
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithCanvasUnavailable_ShouldShowErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        when(assignmentService.syncAssignments(studentId, validToken))
                .thenThrow(new CanvasUnavailableException("Canvas API is currently unavailable"));

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("studentId", studentId))
                .andExpect(model().attribute("assignments", List.of()))
                .andExpect(model().attribute("error", "Canvas is currently unavailable. Please try again later."));

        verify(assignmentService).syncAssignments(studentId, validToken);
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithCanvasApiException_ShouldShowErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        when(assignmentService.syncAssignments(studentId, validToken))
                .thenThrow(new CanvasApiException("Canvas API error", 500));

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("studentId", studentId))
                .andExpect(model().attribute("assignments", List.of()))
                .andExpect(model().attribute("error", "Unable to retrieve assignment data from Canvas. Please try again later."));

        verify(assignmentService).syncAssignments(studentId, validToken);
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithUnexpectedException_ShouldShowGenericErrorMessage() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        when(assignmentService.syncAssignments(studentId, validToken))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("studentId", studentId))
                .andExpect(model().attribute("assignments", List.of()))
                .andExpect(model().attribute("error", "An unexpected error occurred while loading the dashboard. Please try again later."));

        verify(assignmentService).syncAssignments(studentId, validToken);
        verify(canvasApiService, never()).getObservedStudents(anyString());
    }

    @Test
    void studentDashboard_WithAssignmentSyncSuccess_ShouldSetStatusAndBadgeClass() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);
        
        List<StudentDto> mockStudents = Arrays.asList(
            StudentDto.builder()
                .id(1001L)
                .name("John Doe")
                .build()
        );
        
        PlannerItem assignment = PlannerItem.builder()
            .id(1L)
            .studentId(studentId)
            .plannableId(2001L)
            .assignmentTitle("Test Assignment")
            .contextName("Test Course")
            .dueAt(LocalDateTime.now().minusDays(1))
            .submitted(false)
            .missing(true)
            .build();
        
        List<PlannerItem> mockAssignments = Arrays.asList(assignment);
        
        when(assignmentService.syncAssignments(studentId, validToken)).thenReturn(mockAssignments);
        when(canvasApiService.getObservedStudents(validToken)).thenReturn(mockStudents);
        when(assignmentService.calculateAssignmentStatus(assignment)).thenReturn("missing");
        when(assignmentService.getStatusBadgeClass("missing")).thenReturn("badge-danger");

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("assignments", mockAssignments));

        // Verify that status and badge class were set on the assignment
        verify(assignmentService).calculateAssignmentStatus(assignment);
        verify(assignmentService).getStatusBadgeClass("missing");
    }
}