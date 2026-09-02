package com.college.erp.dto;

/** Iteration 1 - identity returned after a successful login. Never carries the password. */
public record LoginResponse(String username, String fullName, String role) {
}
