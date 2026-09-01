package com.college.erp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Iteration 2 - payload used by admins to create or update a course (FR-03).
 */
public record CourseRequest(

        @NotBlank(message = "is required")
        @Size(max = 20, message = "must be at most 20 characters")
        String code,

        @NotBlank(message = "is required")
        @Size(max = 120, message = "must be at most 120 characters")
        String title,

        @NotNull(message = "is required")
        @Positive(message = "must be greater than zero")
        Integer credits,

        @NotNull(message = "is required")
        @Min(value = 1, message = "must be between 1 and 12")
        @Max(value = 12, message = "must be between 1 and 12")
        Integer semester,

        @NotBlank(message = "is required")
        @Size(max = 60, message = "must be at most 60 characters")
        String department) {
}
