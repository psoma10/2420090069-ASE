package com.college.erp.dto;

import jakarta.validation.constraints.*;

/** Iteration 1 - payload used to create or update a student (FR-02). */
public record StudentRequest(
        @NotBlank(message = "is required") String rollNumber,
        @NotBlank(message = "is required") String name,
        @NotBlank(message = "is required") String department,
        @NotNull(message = "is required") @Min(value = 1, message = "must be at least 1")
        @Max(value = 8, message = "must be at most 8") Integer semester,
        String phone,
        @NotBlank(message = "is required") String username,
        @NotBlank(message = "is required") @Size(min = 6, message = "must be at least 6 characters") String password,
        @NotBlank(message = "is required") @Email(message = "must be a valid address") String email
) {
}
