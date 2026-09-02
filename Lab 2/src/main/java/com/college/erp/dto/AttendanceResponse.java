package com.college.erp.dto;

import com.college.erp.entity.Attendance;

import java.time.LocalDate;

/**
 * Iteration 3 - flat view of one attendance record (FR-05).
 * Built inside the service transaction so no lazy proxy reaches the web layer.
 */
public record AttendanceResponse(
        Long id,
        Long studentId,
        String rollNumber,
        String studentName,
        Long courseId,
        String courseCode,
        String courseTitle,
        LocalDate classDate,
        String status) {

    /** Copies the record and its associations into an immutable DTO. */
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getStudent().getId(),
                attendance.getStudent().getRollNumber(),
                attendance.getStudent().getName(),
                attendance.getCourse().getId(),
                attendance.getCourse().getCode(),
                attendance.getCourse().getTitle(),
                attendance.getClassDate(),
                attendance.getStatus().name());
    }
}
