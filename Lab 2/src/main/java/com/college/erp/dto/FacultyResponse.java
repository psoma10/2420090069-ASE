package com.college.erp.dto;

/** Iteration 5 - faculty view returned by the API. Never carries the password. */
public record FacultyResponse(
        Long id,
        String employeeCode,
        String name,
        String department,
        String designation,
        String username,
        String email
) {
}
