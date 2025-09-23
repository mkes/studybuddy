# Design Document

## Overview

The Google Calendar Integration feature extends StudyTracker with the ability to automatically sync student assignments and quizzes to Google Calendar. This integration uses Google Calendar API v3 and OAuth 2.0 to create calendar events with reminders, helping parents stay informed about upcoming due dates through their preferred calendar application.

## Architecture

### System Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web Browser   │◄──►│  Spring Boot App │◄──►│  PostgreSQL DB  │
│                 │    │                  │    │                 │
│ - Calendar UI   │    │ - OAuth Flow     │    │ - OAuth Tokens  │
│ - Sync Settings │    │ - Calendar Sync  │    │ - Sync Settings │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │ Google Calendar │
                       │     API v3      │
                       └─────────────────┘
```

### Integration Flow

```
1. User clicks "Connect Google Calendar"
2. OAuth 2.0 flow → Google authentication
3. Store encrypted access/refresh tokens
4. Create dedicated student calendar
5. Sync assignments → Calendar events
6. Set up automatic sync schedule
7. Handle token refresh and errors
```

## Components and Interfaces

### Web Controllers

#### CalendarController
- **Purpose**: Handle Google Calendar integration UI and OAuth flow for both parent and student accounts
- **Endpoints**:
  - `GET /students/{studentId}/calendar` - Display calendar integration page
  - `GET /auth/google/calendar/parent` - Initiate parent Google OAuth flow
  - `GET /auth/google/calendar/student` - Initiate student Google OAuth flow
  - `GET /auth/google/callback` - Handle OAuth callback for both account types
  - `POST /students/{studentId}/calendar/invite-student` - Send calendar invitation to student
  - `POST /students/{studentId}/calendar/sync` - Manual sync trigger
  - `POST /students/{studentId}/calendar/disconnect` - Disconnect parent or student calendar
  - `POST /students/{studentId}/calendar/settings` - Update sync settings
- **Responsibilities**:
  - Dual OAuth 2.0 flow management (parent and student)
  - Student invitation and email validation
  - Calendar connection status display for both accounts
  - Sync settings configuration
  - Manual sync triggers

### Service Layer

#### GoogleCalendarService
- **Purpose**: Interface with Google Calendar API for both parent and student accounts
- **Methods**:
  - `initiateOAuthFlow(accountType, redirectUri)` - Start OAuth authentication for parent or student
  - `handleOAuthCallback(code, state, accountType)` - Process OAuth callback
  - `createParentCalendar(studentName)` - Create dedicated calendar in parent's account
  - `createStudentCalendar()` - Create dedicated calendar in student's account
  - `createAssignmentEvent(assignment, calendarId, accountType)` - Create calendar event with account-specific settings
  - `updateAssignmentEvent(assignment, eventId)` - Update existing event
  - `deleteAssignmentEvent(eventId)` - Remove calendar event
  - `refreshAccessToken(refreshToken)` - Refresh expired tokens
  - `sendStudentInvitation(studentEmail, invitationLink)` - Send calendar sync invitation to student
- **Responsibilities**:
  - Google Calendar API communication for multiple accounts
  - OAuth token management for parent and student
  - Calendar and event CRUD operations
  - Student invitation management
  - Error handling and retry logic

#### CalendarSyncService
- **Purpose**: Orchestrate assignment synchronization
- **Methods**:
  - `syncStudentAssignments(studentId)` - Sync all assignments for student
  - `syncSingleAssignment(assignment)` - Sync individual assignment
  - `scheduleAutomaticSync(studentId)` - Set up recurring sync
  - `handleSyncErrors(error, studentId)` - Process sync failures
  - `applySyncSettings(settings, studentId)` - Apply user preferences
- **Responsibilities**:
  - Assignment filtering based on settings
  - Batch synchronization operations
  - Sync scheduling and automation
  - Error recovery and logging

#### CalendarTokenService
- **Purpose**: Secure token storage and management
- **Methods**:
  - `storeTokens(userId, accessToken, refreshToken)` - Encrypt and store tokens
  - `getValidAccessToken(userId)` - Retrieve valid access token
  - `refreshTokenIfNeeded(userId)` - Auto-refresh expired tokens
  - `revokeTokens(userId)` - Revoke and delete tokens
  - `isCalendarConnected(userId)` - Check connection status
- **Responsibilities**:
  - Token encryption/decryption
  - Automatic token refresh
  - Secure token storage
  - Connection status tracking

### Repository Layer

#### CalendarTokenRepository
- **Purpose**: Data access for OAuth tokens
- **Methods**:
  - `findByUserId(userId)` - Get tokens for user
  - `save(calendarToken)` - Store encrypted tokens
  - `deleteByUserId(userId)` - Remove user tokens
- **Responsibilities**:
  - Encrypted token persistence
  - User token lookup
  - Token cleanup

#### CalendarSyncSettingsRepository
- **Purpose**: Data access for sync preferences
- **Methods**:
  - `findByUserIdAndStudentId(userId, studentId)` - Get sync settings
  - `save(syncSettings)` - Store user preferences
  - `deleteByUserIdAndStudentId(userId, studentId)` - Remove settings
- **Responsibilities**:
  - Sync preference persistence
  - Settings retrieval and updates

#### CalendarEventMappingRepository
- **Purpose**: Track assignment-to-event mappings
- **Methods**:
  - `findByAssignmentId(assignmentId)` - Get event mapping
  - `save(eventMapping)` - Store assignment-event relationship
  - `deleteByAssignmentId(assignmentId)` - Remove mapping
- **Responsibilities**:
  - Assignment-event relationship tracking
  - Event ID storage for updates/deletions

## Data Models

### CalendarToken Entity

```java
@Entity
@Table(name = "calendar_tokens")
public class CalendarToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId; // Canvas user ID or session ID
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType; // PARENT or STUDENT
    
    @Column(name = "google_email")
    private String googleEmail; // Google account email
    
    @Column(name = "encrypted_access_token", nullable = false)
    private String encryptedAccessToken;
    
    @Column(name = "encrypted_refresh_token", nullable = false)
    private String encryptedRefreshToken;
    
    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;
    
    @Column(name = "calendar_id")
    private String calendarId; // Google Calendar ID for assignments
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

public enum AccountType {
    PARENT, STUDENT
}
```

### CalendarSyncSettings Entity

```java
@Entity
@Table(name = "calendar_sync_settings")
public class CalendarSyncSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled = true;
    
    @Column(name = "included_courses")
    private String includedCourses; // JSON array of course names
    
    @Column(name = "excluded_assignment_types")
    private String excludedAssignmentTypes; // JSON array
    
    @Column(name = "reminder_minutes")
    private String reminderMinutes = "[1440,120]"; // 24h, 2h default
    
    @Column(name = "sync_completed_assignments")
    private Boolean syncCompletedAssignments = false;
    
    @Column(name = "auto_sync_enabled")
    private Boolean autoSyncEnabled = true;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### CalendarEventMapping Entity

```java
@Entity
@Table(name = "calendar_event_mappings")
public class CalendarEventMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId; // PlannerItem.plannableId
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType; // PARENT or STUDENT
    
    @Column(name = "google_event_id", nullable = false)
    private String googleEventId;
    
    @Column(name = "google_calendar_id", nullable = false)
    private String googleCalendarId;
    
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### StudentCalendarInvitation Entity

```java
@Entity
@Table(name = "student_calendar_invitations")
public class StudentCalendarInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId; // Parent's user ID
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "student_email", nullable = false)
    private String studentEmail;
    
    @Column(name = "invitation_token", nullable = false, unique = true)
    private String invitationToken; // Secure token for invitation link
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvitationStatus status = InvitationStatus.PENDING;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

public enum InvitationStatus {
    PENDING, ACCEPTED, EXPIRED, REVOKED
}
```

### Google Calendar DTOs

#### CalendarEventDto
```java
public class CalendarEventDto {
    private String summary; // Assignment title
    private String description; // Course name, points, etc.
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private List<Integer> reminderMinutes;
    private String colorId;
    private Map<String, String> extendedProperties; // Store assignment metadata
}
```

#### SyncSettingsDto
```java
public class SyncSettingsDto {
    private Boolean syncEnabled;
    private List<String> includedCourses;
    private List<String> excludedAssignmentTypes;
    private List<Integer> reminderMinutes;
    private Boolean syncCompletedAssignments;
    private Boolean autoSyncEnabled;
}
```

### Database Schema

```sql
CREATE TABLE calendar_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('PARENT', 'STUDENT')),
    google_email VARCHAR(255),
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    token_expires_at TIMESTAMP NOT NULL,
    calendar_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, student_id, account_type)
);

CREATE TABLE calendar_sync_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    sync_enabled BOOLEAN DEFAULT TRUE,
    sync_to_parent_calendar BOOLEAN DEFAULT TRUE,
    sync_to_student_calendar BOOLEAN DEFAULT TRUE,
    parent_reminder_minutes VARCHAR(255) DEFAULT '[1440,120]', -- 24h, 2h
    student_reminder_minutes VARCHAR(255) DEFAULT '[120,30]',   -- 2h, 30min
    included_courses TEXT,
    excluded_assignment_types TEXT,
    sync_completed_assignments BOOLEAN DEFAULT FALSE,
    auto_sync_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, student_id)
);

CREATE TABLE calendar_event_mappings (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('PARENT', 'STUDENT')),
    google_event_id VARCHAR(255) NOT NULL,
    google_calendar_id VARCHAR(255) NOT NULL,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(assignment_id, student_id, account_type)
);

CREATE TABLE student_calendar_invitations (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    student_email VARCHAR(255) NOT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, student_id)
);

CREATE INDEX idx_calendar_tokens_user_student ON calendar_tokens(user_id, student_id);
CREATE INDEX idx_calendar_tokens_account_type ON calendar_tokens(account_type);
CREATE INDEX idx_sync_settings_user_student ON calendar_sync_settings(user_id, student_id);
CREATE INDEX idx_event_mappings_assignment ON calendar_event_mappings(assignment_id, student_id, account_type);
CREATE INDEX idx_invitations_token ON student_calendar_invitations(invitation_token);
CREATE INDEX idx_invitations_status ON student_calendar_invitations(status, expires_at);
```

## Google Calendar API Integration

### OAuth 2.0 Configuration

```yaml
google:
  oauth:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-uri: ${BASE_URL}/auth/google/callback
    scopes:
      - https://www.googleapis.com/auth/calendar
      - https://www.googleapis.com/auth/calendar.events
```

### API Endpoints Used

1. **OAuth 2.0**: `https://accounts.google.com/o/oauth2/v2/auth`
2. **Token Exchange**: `https://oauth2.googleapis.com/token`
3. **Calendar API**: `https://www.googleapis.com/calendar/v3/`
   - `POST /calendars` - Create calendar
   - `GET /calendars/{calendarId}/events` - List events
   - `POST /calendars/{calendarId}/events` - Create event
   - `PUT /calendars/{calendarId}/events/{eventId}` - Update event
   - `DELETE /calendars/{calendarId}/events/{eventId}` - Delete event

### Event Creation Logic

```java
public CalendarEventDto createAssignmentEvent(PlannerItem assignment) {
    return CalendarEventDto.builder()
        .summary(assignment.getAssignmentTitle())
        .description(buildEventDescription(assignment))
        .startDateTime(assignment.getDueAt().minusHours(1)) // 1 hour before due
        .endDateTime(assignment.getDueAt())
        .reminderMinutes(getReminderSettings(assignment.getStudentId()))
        .colorId("4") // Blue for assignments
        .extendedProperties(Map.of(
            "assignmentId", assignment.getPlannableId().toString(),
            "courseId", assignment.getCourseId().toString(),
            "pointsPossible", assignment.getPointsPossible().toString()
        ))
        .build();
}
```

## Error Handling

### Exception Hierarchy

#### GoogleCalendarException
- **Purpose**: Handle Google Calendar API errors
- **Types**:
  - `TokenExpiredException` (401) - Access token expired
  - `InsufficientScopeException` (403) - Missing calendar permissions
  - `CalendarNotFoundException` (404) - Calendar not found
  - `RateLimitExceededException` (429) - API rate limiting
  - `CalendarApiException` (500) - General API errors

#### CalendarSyncException
- **Purpose**: Handle synchronization errors
- **Types**:
  - `SyncTimeoutException` - Sync operation timeout
  - `AssignmentMappingException` - Event mapping failures
  - `SettingsValidationException` - Invalid sync settings

### Error Recovery Strategy

1. **Token Expiration**: Automatic refresh using refresh token
2. **Rate Limiting**: Exponential backoff with jitter
3. **Network Errors**: Retry with circuit breaker pattern
4. **Sync Failures**: Queue for retry with manual override option
5. **Permission Errors**: Prompt user for re-authentication

## Security Considerations

### Token Security
- **Encryption**: AES-256 encryption for stored tokens
- **Key Management**: Environment-based encryption keys
- **Token Rotation**: Automatic refresh token rotation
- **Secure Storage**: Database-level encryption at rest

### OAuth Security
- **PKCE**: Use Proof Key for Code Exchange for mobile security
- **State Parameter**: CSRF protection in OAuth flow
- **Scope Limitation**: Request minimal required permissions
- **Redirect URI Validation**: Strict redirect URI matching

### Privacy Protection
- **Data Minimization**: Store only necessary calendar data
- **User Consent**: Clear permission requests and explanations
- **Data Retention**: Automatic cleanup of revoked tokens
- **Audit Logging**: Track calendar access and modifications

## Performance Considerations

### Sync Optimization
- **Batch Operations**: Group multiple events in single API calls
- **Incremental Sync**: Only sync changed assignments
- **Caching**: Cache calendar IDs and event mappings
- **Background Processing**: Async sync operations

### API Rate Limiting
- **Request Quotas**: Respect Google Calendar API limits
- **Exponential Backoff**: Handle rate limit responses
- **Request Batching**: Combine multiple operations
- **Monitoring**: Track API usage and quotas

### Database Performance
- **Indexing**: Optimize queries for user and student lookups
- **Connection Pooling**: Efficient database connections
- **Query Optimization**: Minimize database round trips
- **Cleanup Jobs**: Regular cleanup of expired tokens and mappings

## Testing Strategy

### Unit Testing
- **Service Layer**: Mock Google Calendar API responses
- **Token Management**: Test encryption/decryption and refresh
- **Sync Logic**: Test assignment filtering and event creation
- **Error Handling**: Test various API error scenarios

### Integration Testing
- **OAuth Flow**: Test complete authentication process
- **Calendar Operations**: Test event CRUD operations
- **Sync Process**: Test end-to-end assignment synchronization
- **Error Recovery**: Test token refresh and error handling

### Security Testing
- **Token Security**: Verify encryption and secure storage
- **OAuth Security**: Test CSRF protection and state validation
- **Permission Testing**: Verify minimal scope requirements
- **Data Protection**: Test secure data handling and cleanup