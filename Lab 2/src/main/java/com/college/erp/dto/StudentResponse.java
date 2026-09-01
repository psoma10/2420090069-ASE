package com.college.erp.dto;

/** Iteration 1 - student view returned by the API. Never carries the password. */
public record StudentResponse(
        Long id,
        String rollNumber,
        String name,
        String department,
        Integer semester,
        String phone,
        String username,
        String email
) {
}
