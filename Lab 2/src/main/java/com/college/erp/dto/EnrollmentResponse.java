package com.college.erp.dto;

/**
 * Iteration 2 - one student-to-course registration as returned by the API (FR-04).
 *
 * Flattened in the service layer so no lazy association is serialised outside
 * the transaction (spring.jpa.open-in-view is false).
 */
public record EnrollmentResponse(
        Long id,
        Long studentId,
        String rollNumber,
        String studentName,
        CourseResponse course,
        String registeredOn) {
}
