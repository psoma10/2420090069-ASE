package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.StudentRequest;
import com.college.erp.dto.StudentResponse;
import com.college.erp.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Iteration 1 - student information endpoints (FR-02).
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<List<StudentResponse>> list(@RequestParam(required = false) String department) {
        return ApiResponse.ok(studentService.findAll(department));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentResponse> me(Principal principal) {
        return ApiResponse.ok(studentService.findByUsername(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<StudentResponse> byId(@PathVariable Long id) {
        return ApiResponse.ok(studentService.findById(id));
    }

    @GetMapping("/roll/{rollNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<StudentResponse> byRollNumber(@PathVariable String rollNumber) {
        return ApiResponse.ok(studentService.findByRollNumber(rollNumber));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.ok("Student removed");
    }
}
