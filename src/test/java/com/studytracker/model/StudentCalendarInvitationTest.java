package com.studytracker.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StudentCalendarInvitationTest {
    
    private StudentCalendarInvitation invitation;
    private LocalDateTime futureTime;
    private LocalDateTime pastTime;
    
    @BeforeEach
    void setUp() {
        futureTime = LocalDateTime.now().plusDays(7);
        pastTime = LocalDateTime.now().minusDays(1);
        
        invitation = new StudentCalendarInvitation(
                "parent123",
                1L,
                "student@example.com",
                "secure_token_123",
                futureTime
        );
    }
    
    @Test
    void testDefaultConstructor() {
        StudentCalendarInvitation inv = new StudentCalendarInvitation();
        assertNotNull(inv);
        assertNull(inv.getId());
        assertNull(inv.getUserId());
        assertNull(inv.getStudentId());
        assertNull(inv.getStudentEmail());
        assertNull(inv.getInvitationToken());
        assertEquals(InvitationStatus.PENDING, inv.getStatus());
        assertNull(inv.getExpiresAt());
        assertNull(inv.getAcceptedAt());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        assertEquals("parent123", invitation.getUserId());
        assertEquals(1L, invitation.getStudentId());
        assertEquals("student@example.com", invitation.getStudentEmail());
        assertEquals("secure_token_123", invitation.getInvitationToken());
        assertEquals(futureTime, invitation.getExpiresAt());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
    }
    
    @Test
    void testGettersAndSetters() {
        invitation.setId(1L);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        
        LocalDateTime acceptedTime = LocalDateTime.now();
        invitation.setAcceptedAt(acceptedTime);
        
        assertEquals(1L, invitation.getId());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        assertEquals(acceptedTime, invitation.getAcceptedAt());
    }
    
    @Test
    void testIsExpired_WithFutureTime() {
        invitation.setExpiresAt(futureTime);
        assertFalse(invitation.isExpired());
    }
    
    @Test
    void testIsExpired_WithPastTime() {
        invitation.setExpiresAt(pastTime);
        assertTrue(invitation.isExpired());
    }
    
    @Test
    void testIsExpired_WithNullTime() {
        invitation.setExpiresAt(null);
        assertFalse(invitation.isExpired());
    }
    
    @Test
    void testIsPending_WhenPendingAndNotExpired() {
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(futureTime);
        assertTrue(invitation.isPending());
    }
    
    @Test
    void testIsPending_WhenPendingButExpired() {
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(pastTime);
        assertFalse(invitation.isPending());
    }
    
    @Test
    void testIsPending_WhenNotPending() {
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setExpiresAt(futureTime);
        assertFalse(invitation.isPending());
    }
    
    @Test
    void testAccept() {
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertNull(invitation.getAcceptedAt());
        
        LocalDateTime beforeAccept = LocalDateTime.now();
        invitation.accept();
        LocalDateTime afterAccept = LocalDateTime.now();
        
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        assertNotNull(invitation.getAcceptedAt());
        assertTrue(invitation.getAcceptedAt().isAfter(beforeAccept) || 
                  invitation.getAcceptedAt().isEqual(beforeAccept));
        assertTrue(invitation.getAcceptedAt().isBefore(afterAccept) || 
                  invitation.getAcceptedAt().isEqual(afterAccept));
    }
    
    @Test
    void testExpire() {
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        
        invitation.expire();
        
        assertEquals(InvitationStatus.EXPIRED, invitation.getStatus());
    }
    
    @Test
    void testRevoke() {
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        
        invitation.revoke();
        
        assertEquals(InvitationStatus.REVOKED, invitation.getStatus());
    }
    
    @Test
    void testInvitationStatusEnum() {
        assertEquals(4, InvitationStatus.values().length);
        assertEquals(InvitationStatus.PENDING, InvitationStatus.valueOf("PENDING"));
        assertEquals(InvitationStatus.ACCEPTED, InvitationStatus.valueOf("ACCEPTED"));
        assertEquals(InvitationStatus.EXPIRED, InvitationStatus.valueOf("EXPIRED"));
        assertEquals(InvitationStatus.REVOKED, InvitationStatus.valueOf("REVOKED"));
    }
    
    @Test
    void testTimestampFields() {
        LocalDateTime now = LocalDateTime.now();
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);
        
        assertEquals(now, invitation.getCreatedAt());
        assertEquals(now, invitation.getUpdatedAt());
    }
    
    @Test
    void testToString() {
        invitation.setId(1L);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        LocalDateTime acceptedTime = LocalDateTime.now();
        invitation.setAcceptedAt(acceptedTime);
        
        String toString = invitation.toString();
        assertTrue(toString.contains("StudentCalendarInvitation{"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("userId='parent123'"));
        assertTrue(toString.contains("studentId=1"));
        assertTrue(toString.contains("studentEmail='student@example.com'"));
        assertTrue(toString.contains("invitationToken='secure_token_123'"));
        assertTrue(toString.contains("status=ACCEPTED"));
        assertTrue(toString.contains("acceptedAt=" + acceptedTime));
    }
    
    @Test
    void testEmailValidation() {
        // Test with valid email
        invitation.setStudentEmail("valid@example.com");
        assertEquals("valid@example.com", invitation.getStudentEmail());
        
        // Test with another valid email format
        invitation.setStudentEmail("student.name+tag@university.edu");
        assertEquals("student.name+tag@university.edu", invitation.getStudentEmail());
    }
}