package com.studytracker.controller;

import com.studytracker.service.CanvasApiService;
import com.studytracker.exception.InvalidTokenException;
import com.studytracker.exception.CanvasApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CanvasApiService canvasApiService;

    @Test
    void loginPage_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void login_WithValidToken_ShouldRedirectToStudents() throws Exception {
        when(canvasApiService.validateToken("valid-token")).thenReturn(true);

        mockMvc.perform(post("/login")
                .param("canvasToken", "valid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(request().sessionAttribute("canvasToken", "valid-token"));
    }

    @Test
    void login_WithInvalidToken_ShouldRedirectToLoginWithError() throws Exception {
        when(canvasApiService.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/login")
                .param("canvasToken", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Invalid Canvas API token. Please check your token and try again."));
    }

    @Test
    void login_WithInvalidTokenException_ShouldRedirectToLoginWithError() throws Exception {
        when(canvasApiService.validateToken(anyString()))
                .thenThrow(new InvalidTokenException("Token is invalid"));

        mockMvc.perform(post("/login")
                .param("canvasToken", "bad-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Invalid Canvas API token. Please check your token and try again."));
    }

    @Test
    void login_WithCanvasApiException_ShouldRedirectToLoginWithError() throws Exception {
        when(canvasApiService.validateToken(anyString()))
                .thenThrow(new CanvasApiException("Canvas API unavailable"));

        mockMvc.perform(post("/login")
                .param("canvasToken", "some-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Unable to connect to Canvas. Please try again later."));
    }

    @Test
    void login_WithUnexpectedException_ShouldRedirectToLoginWithGenericError() throws Exception {
        when(canvasApiService.validateToken(anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/login")
                .param("canvasToken", "some-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "An unexpected error occurred. Please try again."));
    }

    @Test
    void login_WithEmptyToken_ShouldHandleGracefully() throws Exception {
        when(canvasApiService.validateToken("")).thenReturn(false);

        mockMvc.perform(post("/login")
                .param("canvasToken", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Invalid Canvas API token. Please check your token and try again."));
    }

    @Test
    void logout_ShouldInvalidateSessionAndRedirectToLogin() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}