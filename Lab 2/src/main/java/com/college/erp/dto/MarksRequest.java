package com.college.erp.dto;

import com.college.erp.entity.Marks;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Iteration 4 - payload for entering or correcting one student's marks in one course (FR-06).
 * The same shape is reused by the update endpoint, where the ids identify nothing new
 * and only the two mark components are applied.
 */
public record MarksRequest(
        @NotNull(message = "is required") Long studentId,
        @NotNull(message = "is required") Long courseId,
        @NotNull(message = "is required")
        @Min(value = 0, message = "cannot be negative")
        @Max(value = Marks.MAX_INTERNAL, message = "cannot exceed " + Marks.MAX_INTERNAL)
        Integer internalMarks,
        @NotNull(message = "is required")
        @Min(value = 0, message = "cannot be negative")
        @Max(value = Marks.MAX_EXTERNAL, message = "cannot exceed " + Marks.MAX_EXTERNAL)
        Integer externalMarks) {
}
