package com.studytracker.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StudentDto class.
 * Tests JSON serialization/deserialization and data integrity.
 */
class StudentDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testStudentDtoCreation() {
        // Given
        Long id = 12345L;
        String name = "John Doe";
        String sortableName = "Doe, John";
        String avatarUrl = "https://canvas.example.com/avatar.jpg";
        String shortName = "John";
        String loginId = "john.doe@example.com";

        // When
        StudentDto student = StudentDto.builder()
                .id(id)
                .name(name)
                .sortableName(sortableName)
                .avatarUrl(avatarUrl)
                .shortName(shortName)
                .loginId(loginId)
                .build();

        // Then
        assertThat(student.getId()).isEqualTo(id);
        assertThat(student.getName()).isEqualTo(name);
        assertThat(student.getSortableName()).isEqualTo(sortableName);
        assertThat(student.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(student.getShortName()).isEqualTo(shortName);
        assertThat(student.getLoginId()).isEqualTo(loginId);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        // Given
        String json = """
                {
                    "id": 12345,
                    "name": "John Doe",
                    "sortable_name": "Doe, John",
                    "avatar_url": "https://canvas.example.com/avatar.jpg",
                    "short_name": "John",
                    "login_id": "john.doe@example.com"
                }
                """;

        // When
        StudentDto student = objectMapper.readValue(json, StudentDto.class);

        // Then
        assertThat(student.getId()).isEqualTo(12345L);
        assertThat(student.getName()).isEqualTo("John Doe");
        assertThat(student.getSortableName()).isEqualTo("Doe, John");
        assertThat(student.getAvatarUrl()).isEqualTo("https://canvas.example.com/avatar.jpg");
        assertThat(student.getShortName()).isEqualTo("John");
        assertThat(student.getLoginId()).isEqualTo("john.doe@example.com");
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Given
        StudentDto student = StudentDto.builder()
                .id(12345L)
                .name("John Doe")
                .sortableName("Doe, John")
                .avatarUrl("https://canvas.example.com/avatar.jpg")
                .shortName("John")
                .loginId("john.doe@example.com")
                .build();

        // When
        String json = objectMapper.writeValueAsString(student);

        // Then
        assertThat(json).contains("\"id\":12345");
        assertThat(json).contains("\"name\":\"John Doe\"");
        assertThat(json).contains("\"sortable_name\":\"Doe, John\"");
        assertThat(json).contains("\"avatar_url\":\"https://canvas.example.com/avatar.jpg\"");
        assertThat(json).contains("\"short_name\":\"John\"");
        assertThat(json).contains("\"login_id\":\"john.doe@example.com\"");
    }

    @Test
    void testStudentDtoWithNullValues() {
        // Given & When
        StudentDto student = StudentDto.builder()
                .id(12345L)
                .name("John Doe")
                .build();

        // Then
        assertThat(student.getId()).isEqualTo(12345L);
        assertThat(student.getName()).isEqualTo("John Doe");
        assertThat(student.getSortableName()).isNull();
        assertThat(student.getAvatarUrl()).isNull();
        assertThat(student.getShortName()).isNull();
        assertThat(student.getLoginId()).isNull();
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        StudentDto student1 = StudentDto.builder()
                .id(12345L)
                .name("John Doe")
                .sortableName("Doe, John")
                .build();

        StudentDto student2 = StudentDto.builder()
                .id(12345L)
                .name("John Doe")
                .sortableName("Doe, John")
                .build();

        StudentDto student3 = StudentDto.builder()
                .id(67890L)
                .name("Jane Smith")
                .sortableName("Smith, Jane")
                .build();

        // Then
        assertThat(student1).isEqualTo(student2);
        assertThat(student1.hashCode()).isEqualTo(student2.hashCode());
        assertThat(student1).isNotEqualTo(student3);
    }
}