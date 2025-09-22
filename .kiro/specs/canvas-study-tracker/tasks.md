# Implementation Plan

- [x] 1. Set up project structure and build configuration
  - Create Spring Boot project with Gradle build configuration
  - Configure dependencies for Spring Web, Thymeleaf, JPA, PostgreSQL
  - Set up application properties with database and Canvas API configuration
  - _Requirements: 6.1_

- [x] 2. Create core data models and database schema
  - Implement PlannerItem JPA entity with all required fields and annotations
  - Create database migration scripts for planner_items table with indexes
  - Write unit tests for PlannerItem entity validation and constraints
  - _Requirements: 4.3, 6.4_

- [x] 3. Implement Canvas API DTOs and mapping utilities
  - Create StudentDto and AssignmentDto classes for Canvas API responses
  - Implement mapper utilities to convert Canvas API responses to internal models
  - Write unit tests for DTO mapping and data transformation
  - _Requirements: 2.1, 3.1_

- [ ] 4. Build Canvas API service layer
  - Implement CanvasApiService with HTTP client for Canvas REST API calls
  - Add methods for getObservedStudents, getStudentAssignments, and validateToken
  - Implement Bearer token authentication and error handling for Canvas API
  - Write unit tests with mocked HTTP responses for all Canvas API methods
  - _Requirements: 1.5, 2.1, 3.1, 4.1, 4.4_

- [ ] 5. Create repository layer for assignment data access
  - Implement PlannerItemRepository extending JpaRepository with custom query methods
  - Add method findByStudentIdAndDueDateBetweenOrderByDueDateDesc for date filtering
  - Implement bulk save operations for efficient assignment synchronization
  - Write integration tests for repository methods with embedded database
  - _Requirements: 4.3, 3.3, 5.3_

- [ ] 6. Implement assignment service with business logic
  - Create AssignmentService with syncAssignments and getAssignmentsByStudent methods
  - Implement assignment status calculation logic (submitted, missing, late, overdue, pending)
  - Add date range filtering with default Aug 1 - Sep 30, 2025 range
  - Write unit tests for business logic and status calculations
  - _Requirements: 3.2, 3.4, 3.5, 4.1, 4.2_

- [ ] 7. Build authentication controller and login flow
  - Implement AuthController with GET / for login page and POST /login for authentication
  - Create login.html Thymeleaf template with Canvas token input form
  - Add Canvas token validation and session management
  - Implement error handling for invalid tokens with user-friendly messages
  - Write controller tests for authentication flow and error scenarios
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.5_

- [ ] 8. Create student selection controller and interface
  - Implement StudentController with GET /students endpoint
  - Create students.html Thymeleaf template with dropdown selection interface
  - Add logic to retrieve and display observed students from Canvas API
  - Implement error handling for Canvas API unavailability
  - Write controller tests for student retrieval and selection flow
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 9. Build student dashboard controller with assignment display
  - Implement GET /students/{studentId} endpoint in StudentController
  - Create student-dashboard.html Thymeleaf template for assignment display
  - Add automatic assignment sync trigger on dashboard page load
  - Implement assignment list display with chronological descending order
  - Add visual status badges (Green/Red/Yellow/Blue) for assignment status
  - Write controller tests for dashboard loading and assignment display
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1_

- [ ] 10. Implement internal sync API controller
  - Create SyncController with POST /api/sync/{studentId} endpoint
  - Implement JSON response format for assignment data with grades
  - Add error handling and rate limiting management for Canvas API calls
  - Ensure sync completes within 10-second timeout requirement
  - Write API tests for sync endpoint and JSON response format
  - _Requirements: 4.1, 4.2, 4.4, 5.2_

- [ ] 11. Add responsive UI styling and mobile support
  - Implement responsive CSS for mobile and desktop compatibility
  - Style assignment cards with proper status badge colors and layout
  - Add Bootstrap or similar framework for consistent responsive design
  - Test mobile interface rendering and usability
  - _Requirements: 5.4, 6.5_

- [ ] 12. Implement comprehensive error handling and logging
  - Create custom exception classes (CanvasApiException, ApplicationException)
  - Add global exception handler with user-friendly error pages
  - Implement logging for Canvas API requests, database operations, and errors
  - Add graceful handling for Canvas API downtime and database connection failures
  - Write tests for error scenarios and exception handling
  - _Requirements: 6.2, 6.3, 6.4, 5.5_

- [ ] 13. Add performance optimizations and caching
  - Implement HTTP client connection pooling for Canvas API calls
  - Add database query optimization with proper indexing
  - Implement bulk operations for assignment synchronization
  - Add performance monitoring to ensure 3-second load time requirement
  - Write performance tests for assignment loading and sync operations
  - _Requirements: 5.1, 5.2, 5.3, 4.2_

- [ ] 14. Configure security and CORS settings
  - Set up CORS configuration for development (localhost) and production
  - Implement secure session management and token storage
  - Add input validation for all user inputs and API parameters
  - Configure environment variable handling for database credentials
  - Write security tests for authentication and data protection
  - _Requirements: 6.1, 6.5, 1.4, 1.5_

- [ ] 15. Create integration tests for complete user workflows
  - Write end-to-end tests for login → student selection → dashboard flow
  - Test automatic assignment sync and data display functionality
  - Add tests for error scenarios (invalid tokens, Canvas downtime)
  - Test date range filtering and assignment status calculations
  - Verify performance requirements are met in integration environment
  - _Requirements: 1.1-1.5, 2.1-2.5, 3.1-3.5, 4.1-4.4, 5.1-5.5_