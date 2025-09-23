package com.studytracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @InjectMocks
    private EmailService emailService;
    
    private static final String FROM_EMAIL = "noreply@studytracker.com";
    private static final String APP_NAME = "StudyTracker";
    private static final String STUDENT_EMAIL = "student@example.com";
    private static final String STUDENT_NAME = "John Doe";
    private static final String INVITATION_URL = "http://localhost:8080/calendar/invitation/accept?token=abc123";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "appName", APP_NAME);
    }
    
    @Test
    void sendCalendarInvitation_Success() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.of(2024, 12, 25, 15, 30);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        
        // When
        emailService.sendCalendarInvitation(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt);
        
        // Then
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertEquals(STUDENT_EMAIL, sentMessage.getTo()[0]);
        assertEquals("Calendar Sync Invitation - " + APP_NAME, sentMessage.getSubject());
        
        String messageText = sentMessage.getText();
        assertNotNull(messageText);
        assertTrue(messageText.contains("Hello " + STUDENT_NAME));
        assertTrue(messageText.contains(INVITATION_URL));
        assertTrue(messageText.contains("Dec 25, 2024 at 3:30 PM"));
        assertTrue(messageText.contains("calendar integration"));
        assertTrue(messageText.contains("Google Calendar"));
        assertTrue(messageText.contains("assignments"));
        assertTrue(messageText.contains("reminders"));
    }
    
    @Test
    void sendCalendarInvitation_NullStudentName_HandlesGracefully() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.of(2024, 12, 25, 15, 30);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        
        // When
        emailService.sendCalendarInvitation(STUDENT_EMAIL, null, INVITATION_URL, expiresAt);
        
        // Then
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String messageText = sentMessage.getText();
        assertNotNull(messageText);
        assertTrue(messageText.contains("Hello ,") || messageText.contains("Hello"));
        assertTrue(messageText.contains(INVITATION_URL));
    }
    
    @Test
    void sendCalendarInvitation_EmailSendingFails_ThrowsRuntimeException() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                emailService.sendCalendarInvitation(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt));
        
        assertEquals("Failed to send invitation email", exception.getMessage());
        assertEquals("SMTP server unavailable", exception.getCause().getMessage());
    }
    
    @Test
    void sendInvitationReminder_Success() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.of(2024, 12, 25, 15, 30);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        
        // When
        emailService.sendInvitationReminder(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt);
        
        // Then
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertEquals(STUDENT_EMAIL, sentMessage.getTo()[0]);
        assertEquals("Reminder: Calendar Sync Invitation - " + APP_NAME, sentMessage.getSubject());
        
        String messageText = sentMessage.getText();
        assertNotNull(messageText);
        assertTrue(messageText.contains("Hello " + STUDENT_NAME));
        assertTrue(messageText.contains("reminder"));
        assertTrue(messageText.contains("pending calendar sync invitation"));
        assertTrue(messageText.contains(INVITATION_URL));
        assertTrue(messageText.contains("Dec 25, 2024 at 3:30 PM"));
        assertTrue(messageText.contains("⚠️"));
        assertTrue(messageText.contains("expire"));
    }
    
    @Test
    void sendInvitationReminder_EmailSendingFails_ThrowsRuntimeException() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        doThrow(new RuntimeException("Network timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                emailService.sendInvitationReminder(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt));
        
        assertEquals("Failed to send reminder email", exception.getMessage());
        assertEquals("Network timeout", exception.getCause().getMessage());
    }
    
    @Test
    void buildInvitationEmailBody_ContainsRequiredElements() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.of(2024, 12, 25, 15, 30);
        
        // When
        emailService.sendCalendarInvitation(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt);
        
        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        String messageText = messageCaptor.getValue().getText();
        
        // Check for key elements
        assertTrue(messageText.contains("Benefits of calendar sync:"));
        assertTrue(messageText.contains("• Automatic reminders"));
        assertTrue(messageText.contains("• Never miss a due date"));
        assertTrue(messageText.contains("• Better organization"));
        assertTrue(messageText.contains("• Seamless integration"));
        assertTrue(messageText.contains("The " + APP_NAME + " Team"));
        assertTrue(messageText.contains("automated message"));
        assertTrue(messageText.contains("do not reply"));
        assertTrue(messageText.contains("did not expect this invitation"));
    }
    
    @Test
    void buildReminderEmailBody_ContainsRequiredElements() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.of(2024, 12, 25, 15, 30);
        
        // When
        emailService.sendInvitationReminder(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt);
        
        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        String messageText = messageCaptor.getValue().getText();
        
        // Check for key elements
        assertTrue(messageText.contains("friendly reminder"));
        assertTrue(messageText.contains("Don't miss out on:"));
        assertTrue(messageText.contains("• Automatic assignment reminders"));
        assertTrue(messageText.contains("• Better organization"));
        assertTrue(messageText.contains("• Seamless calendar integration"));
        assertTrue(messageText.contains("The " + APP_NAME + " Team"));
        assertTrue(messageText.contains("automated message"));
        assertTrue(messageText.contains("do not reply"));
    }
    
    @Test
    void emailFormatting_HandlesSpecialCharacters() {
        // Given
        String specialName = "José María O'Connor";
        String specialEmail = "jose.maria@université.edu";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(2);
        
        // When
        emailService.sendCalendarInvitation(specialEmail, specialName, INVITATION_URL, expiresAt);
        
        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(specialEmail, sentMessage.getTo()[0]);
        assertTrue(sentMessage.getText().contains(specialName));
    }
    
    @Test
    void dateFormatting_DisplaysCorrectFormat() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.of(2024, 1, 15, 9, 5); // Jan 15, 2024 at 9:05 AM
        
        // When
        emailService.sendCalendarInvitation(STUDENT_EMAIL, STUDENT_NAME, INVITATION_URL, expiresAt);
        
        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        String messageText = messageCaptor.getValue().getText();
        assertTrue(messageText.contains("Jan 15, 2024 at 9:05 AM"));
    }
}