package com.college.erp.dto;

import com.college.erp.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Iteration 3 - payload for marking one student on one day (FR-05).
 * The same shape is reused by the update endpoint, where only {@code status} is applied.
 */
public record AttendanceRequest(
        @NotNull(message = "is required") Long studentId,
        @NotNull(message = "is required") Long courseId,
        @NotNull(message = "is required") LocalDate classDate,
        @NotNull(message = "is required") AttendanceStatus status) {
}
