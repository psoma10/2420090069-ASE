package com.college.erp.dto;

import java.time.LocalDateTime;

/**
 * Iteration 4 - one published result row for a student in a course (FR-07, US-05).
 */
public record ResultResponse(
        Long id,
        Long studentId,
        String rollNumber,
        Long courseId,
        String courseCode,
        String courseTitle,
        Integer totalMarks,
        String grade,
        boolean passed,
        LocalDateTime publishedOn) {
}
