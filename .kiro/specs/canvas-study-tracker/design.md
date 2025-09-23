# Design Document

## Overview

StudyTracker is a Spring Boot web application that provides parents and observers with a centralized dashboard to monitor their students' Canvas LMS assignments and academic progress. The system integrates with Canvas API to fetch real-time assignment data and presents it through an intuitive web interface with automatic synchronization and local caching for optimal performance.

## Architecture

### System Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web Browser   │◄──►│  Spring Boot App │◄──►│  PostgreSQL DB  │
│                 │    │                  │    │                 │
│ - Thymeleaf UI  │    │ - REST API       │    │ - Assignment    │
│ - localStorage  │    │ - Canvas Client  │    │   Cache         │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Canvas LMS    │
                       │   REST API      │
                       └─────────────────┘
```

### Technology Stack

- **Frontend**: Spring Boot 3.2 with Thymeleaf templates
- **Backend**: Spring Boot 3.2, JPA/Hibernate
- **Build Tool**: Gradle 8+
- **Database**: PostgreSQL 12+
- **API Integration**: Canvas LMS REST API
- **Authentication**: Canvas API tokens with Bearer auth

## Components and Interfaces

### Web Controllers

#### AuthController
- **Purpose**: Handle user authentication flow
- **Endpoints**:
  - `GET /` - Display login page with Canvas token input form
  - `POST /login` - Process Canvas token authentication
- **Responsibilities**:
  - Validate Canvas API tokens
  - Store tokens in session/localStorage
  - Handle authentication errors

#### StudentController
- **Purpose**: Manage student selection and dashboard
- **Endpoints**:
  - `GET /students` - Display observed students selection page
  - `GET /students/{studentId}` - Display student dashboard with assignments
- **Responsibilities**:
  - Retrieve observed students from Canvas
  - Trigger automatic assignment sync on dashboard load
  - Handle student selection and navigation

#### SyncController (Internal API)
- **Purpose**: Handle assignment synchronization
- **Endpoints**:
  - `POST /api/sync/{studentId}` - Internal sync endpoint for automatic updates
- **Responsibilities**:
  - Orchestrate Canvas API calls
  - Return formatted assignment data
  - Handle sync errors and rate limiting

### Service Layer

#### CanvasApiService
- **Purpose**: Interface with Canvas LMS API
- **Methods**:
  - `getObservedStudents(token)` - Retrieve list of observed students
  - `getStudentPlannerItems(token, studentId, dateRange)` - Fetch planner items from /api/v1/users/{userId}/planner/items
  - `getStudentSubmissions(token, courseId, studentId)` - Fetch detailed grades from /api/v1/courses/{courseId}/students/submissions
  - `validateToken(token)` - Verify Canvas API token validity
- **Responsibilities**:
  - HTTP client management for Canvas API
  - Bearer token authentication
  - Rate limiting and error handling
  - Response parsing and mapping
  - Mapping plannable_id from planner items to assignment_id in submissions for accurate grade retrieval

#### AssignmentService
- **Purpose**: Business logic for assignment management
- **Methods**:
  - `syncAssignments(studentId, token)` - Orchestrate sync process using both planner and submissions APIs
  - `getAssignmentsByStudent(studentId, dateRange)` - Retrieve cached assignments
  - `calculateAssignmentStatus(assignment)` - Determine status badges
  - `mapGradesToAssignments(plannerItems, submissions)` - Map entered_score from submissions to planner items using plannable_id/assignment_id relationship
- **Responsibilities**:
  - Assignment data processing with accurate grade mapping
  - Status calculation logic
  - Date range filtering
  - Cache management coordination
  - Grade synchronization between planner items and submissions data

### Repository Layer

#### PlannerItemRepository
- **Purpose**: Data access for assignment storage
- **Methods**:
  - `findByStudentIdAndDueDateBetweenOrderByDueDateDesc()` - Query assignments by date range
  - `saveAll()` - Bulk insert/update assignments
  - `deleteByStudentIdAndPlannable()` - Remove outdated assignments
- **Responsibilities**:
  - Database CRUD operations
  - Query optimization
  - Data persistence

## Data Models

### PlannerItem Entity

```java
@Entity
@Table(name = "planner_items")
public class PlannerItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "plannable_id", nullable = false)
    private Long plannableId;
    
    @Column(name = "assignment_title", length = 500, nullable = false)
    private String assignmentTitle;
    
    @Column(name = "context_name", length = 200)
    private String contextName;
    
    @Column(name = "due_at")
    private LocalDateTime dueAt;
    
    @Column(name = "points_possible", precision = 10, scale = 2)
    private BigDecimal pointsPossible;
    
    @Column(name = "current_grade", precision = 10, scale = 2)
    private BigDecimal currentGrade;
    
    @Column(name = "submitted", nullable = false)
    private Boolean submitted = false;
    
    @Column(name = "missing", nullable = false)
    private Boolean missing = false;
    
    @Column(name = "late", nullable = false)
    private Boolean late = false;
    
    @Column(name = "graded", nullable = false)
    private Boolean graded = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Canvas API DTOs

#### StudentDto
```java
public class StudentDto {
    private Long id;
    private String name;
    private String sortableName;
    private String avatarUrl;
}
```

#### AssignmentDto
```java
public class AssignmentDto {
    private Long id;
    private Long studentId;
    private Long plannableId;
    private String assignmentTitle;
    private String contextName;
    private LocalDateTime dueAt;
    private BigDecimal pointsPossible;
    private BigDecimal currentGrade;
    private Boolean submitted;
    private Boolean missing;
    private Boolean late;
    private Boolean graded;
}
```

### Database Schema

```sql
CREATE TABLE planner_items (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    plannable_id BIGINT NOT NULL,
    assignment_title VARCHAR(500) NOT NULL,
    context_name VARCHAR(200),
    due_at TIMESTAMP,
    points_possible DECIMAL(10,2),
    current_grade DECIMAL(10,2),
    submitted BOOLEAN DEFAULT FALSE,
    missing BOOLEAN DEFAULT FALSE,
    late BOOLEAN DEFAULT FALSE,
    graded BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, plannable_id)
);

CREATE INDEX idx_planner_items_student_due_desc 
ON planner_items(student_id, due_at DESC);
```

## Error Handling

### Exception Hierarchy

#### CanvasApiException
- **Purpose**: Handle Canvas API-specific errors
- **Types**:
  - `InvalidTokenException` (401) - Invalid or expired Canvas token
  - `InsufficientPermissionsException` (403) - Token lacks observer permissions
  - `CanvasUnavailableException` (502) - Canvas API downtime
  - `RateLimitExceededException` (429) - API rate limiting

#### ApplicationException
- **Purpose**: Handle application-specific errors
- **Types**:
  - `StudentNotFoundException` (404) - Student not found or not observed
  - `DatabaseConnectionException` (500) - Database connectivity issues
  - `SyncTimeoutException` (503) - Sync operation timeout

### Error Response Strategy

1. **Client Errors (4xx)**: Display user-friendly messages with corrective actions
2. **Server Errors (5xx)**: Log detailed errors, display generic user message
3. **Canvas API Errors**: Retry with exponential backoff for transient failures
4. **Database Errors**: Graceful degradation with cached data when possible

## Testing Strategy

### Unit Testing

#### Service Layer Tests
- **CanvasApiService**: Mock HTTP responses, test error handling
- **AssignmentService**: Test business logic, status calculations
- **Repository Tests**: Test database queries and constraints

#### Controller Tests
- **Web Controllers**: Test request/response handling, view rendering
- **API Controllers**: Test JSON responses, error codes

### Integration Testing

#### Canvas API Integration
- **Mock Canvas API**: Test complete sync workflow
- **Error Scenarios**: Test rate limiting, timeouts, invalid responses
- **Authentication Flow**: Test token validation and error handling

#### Database Integration
- **Repository Tests**: Test with embedded PostgreSQL
- **Transaction Tests**: Test data consistency during sync operations
- **Performance Tests**: Test query performance with large datasets

### End-to-End Testing

#### User Workflows
- **Authentication Flow**: Login → Student Selection → Dashboard
- **Assignment Sync**: Automatic sync on dashboard load
- **Error Handling**: Invalid tokens, Canvas downtime scenarios

## Performance Considerations

### Caching Strategy
- **Database Caching**: Store assignments locally to reduce Canvas API calls
- **Session Caching**: Cache student lists during user session
- **HTTP Caching**: Implement appropriate cache headers for static resources

### API Optimization
- **Batch Requests**: Single API call for assignments and grades
- **Rate Limiting**: Implement exponential backoff for Canvas API
- **Connection Pooling**: Configure HTTP client connection pooling

### Database Optimization
- **Indexing**: Index on (student_id, due_at DESC) for fast queries
- **Bulk Operations**: Use batch inserts/updates for assignment sync
- **Query Optimization**: Limit result sets with date range filtering

## Security Considerations

### Authentication Security
- **Token Storage**: Store Canvas tokens in secure browser localStorage
- **Token Validation**: Validate tokens on each API request
- **Session Management**: Implement secure session handling

### Data Protection
- **Environment Variables**: Externalize database credentials
- **Input Validation**: Validate all user inputs and API responses
- **SQL Injection Prevention**: Use parameterized queries with JPA

### CORS Configuration
- **Development**: Allow localhost:3000 for frontend development
- **Production**: Restrict to application domain only
- **Headers**: Configure appropriate CORS headers for security