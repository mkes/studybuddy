# StudyTracker

A Spring Boot web application that integrates with Canvas LMS to help parents and observers track their students' assignments, grades, and academic progress.

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Canvas LMS API access token

## Getting Started

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE studytracker_dev;
CREATE USER studytracker WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE studytracker_dev TO studytracker;
```

### Environment Variables

Set the following environment variables or update `application.yml`:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/studytracker_dev
export DATABASE_USERNAME=studytracker
export DATABASE_PASSWORD=password
export CANVAS_API_BASE_URL=https://your-canvas-instance.com/api/v1
```

### Running the Application

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Running Tests

```bash
./gradlew test
```

## Project Structure

```
src/
├── main/
│   ├── java/com/studytracker/
│   │   ├── controller/          # Web controllers
│   │   ├── service/             # Business logic services
│   │   ├── repository/          # Data access layer
│   │   ├── model/               # JPA entities
│   │   ├── dto/                 # Data transfer objects
│   │   ├── config/              # Configuration classes
│   │   ├── exception/           # Custom exceptions
│   │   └── StudyTrackerApplication.java
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── templates/           # Thymeleaf templates
│       └── static/              # CSS, JS, images
└── test/
    ├── java/com/studytracker/
    │   ├── controller/          # Controller tests
    │   ├── service/             # Service tests
    │   ├── repository/          # Repository tests
    │   └── integration/         # Integration tests
    └── resources/
        └── application-test.yml
```