package com.college.erp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Iteration 5 - payload used to create or update a faculty member (FR-08). */
public record FacultyRequest(
        @NotBlank(message = "is required") String employeeCode,
        @NotBlank(message = "is required") String name,
        @NotBlank(message = "is required") String department,
        String designation,
        @NotBlank(message = "is required") String username,
        @NotBlank(message = "is required") @Size(min = 6, message = "must be at least 6 characters") String password,
        @NotBlank(message = "is required") @Email(message = "must be a valid address") String email
) {
}
