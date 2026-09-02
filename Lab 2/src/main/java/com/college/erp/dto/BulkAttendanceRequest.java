package com.college.erp.dto;

import com.college.erp.entity.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Iteration 3 - payload for marking a whole class in one request (FR-05).
 * One course, one date, and one entry per student.
 */
public record BulkAttendanceRequest(
        @NotNull(message = "is required") Long courseId,
        @NotNull(message = "is required") LocalDate classDate,
        @NotEmpty(message = "must contain at least one student") @Valid List<Entry> entries) {

    /** A single student's status within a bulk submission. */
    public record Entry(
            @NotNull(message = "is required") Long studentId,
            @NotNull(message = "is required") AttendanceStatus status) {
    }
}
