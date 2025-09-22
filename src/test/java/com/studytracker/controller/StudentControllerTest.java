package com.studytracker.controller;

import com.studytracker.dto.StudentDto;
import com.studytracker.service.CanvasApiService;
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
    void studentDashboard_WithValidToken_ShouldDisplayDashboard() throws Exception {
        // Arrange
        String validToken = "valid_canvas_token";
        Long studentId = 1001L;
        session.setAttribute("canvasToken", validToken);

        // Act & Assert
        mockMvc.perform(get("/students/{studentId}", studentId).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("student-dashboard"))
                .andExpect(model().attribute("studentId", studentId))
                .andExpect(model().attribute("message", "Student dashboard will be implemented in the next task."))
                .andExpect(model().attribute("messageType", "info"));
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
    }
}