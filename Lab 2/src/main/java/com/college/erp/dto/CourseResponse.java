package com.college.erp.dto;

/**
 * Iteration 2 - course view returned by the API (FR-03).
 *
 * The assigned faculty is flattened to id and name here so no lazy Hibernate
 * proxy escapes the service transaction (spring.jpa.open-in-view is false).
 * Both faculty fields are null while a course has no faculty assigned.
 */
public record CourseResponse(
        Long id,
        String code,
        String title,
        Integer credits,
        Integer semester,
        String department,
        Long facultyId,
        String facultyName) {
}
