package com.college.erp.dto;

import jakarta.validation.constraints.NotBlank;

/** Iteration 1 - credentials posted to /api/auth/login (US-01). */
public record LoginRequest(
        @NotBlank(message = "is required") String username,
        @NotBlank(message = "is required") String password
) {
}
