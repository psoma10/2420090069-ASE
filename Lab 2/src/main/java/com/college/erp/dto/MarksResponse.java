package com.college.erp.dto;

/**
 * Iteration 4 - marks of one student in one course, flattened out of the entity
 * so that no lazy association escapes the transaction (FR-06).
 */
public record MarksResponse(
        Long id,
        Long studentId,
        String rollNumber,
        Long courseId,
        String courseCode,
        String courseTitle,
        Integer internalMarks,
        Integer externalMarks,
        int total) {
}
