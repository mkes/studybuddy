package com.studytracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO representing a student from Canvas API responses.
 * Used for deserializing Canvas API /users/{user_id}/observees endpoint responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDto {

    /**
     * Canvas user ID for the student
     */
    private Long id;

    /**
     * Full name of the student
     */
    private String name;

    /**
     * Sortable name format (Last, First)
     */
    @JsonProperty("sortable_name")
    private String sortableName;

    /**
     * URL to student's avatar image
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /**
     * Short name or display name
     */
    @JsonProperty("short_name")
    private String shortName;

    /**
     * Login ID for the student
     */
    @JsonProperty("login_id")
    private String loginId;
}