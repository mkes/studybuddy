package com.studytracker.controller;

import com.studytracker.service.CanvasApiService;
import com.studytracker.exception.InvalidTokenException;
import com.studytracker.exception.CanvasApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private CanvasApiService canvasApiService;

    @GetMapping("/")
    public String loginPage(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("canvasToken") String canvasToken, 
                       HttpSession session, 
                       RedirectAttributes redirectAttributes) {
        try {
            // Validate the Canvas token
            boolean isValid = canvasApiService.validateToken(canvasToken);
            
            if (isValid) {
                // Store token in session
                session.setAttribute("canvasToken", canvasToken);
                return "redirect:/students";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid Canvas API token. Please check your token and try again.");
                return "redirect:/";
            }
        } catch (InvalidTokenException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid Canvas API token. Please check your token and try again.");
            return "redirect:/";
        } catch (CanvasApiException e) {
            redirectAttributes.addFlashAttribute("error", "Unable to connect to Canvas. Please try again later.");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
            return "redirect:/";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}