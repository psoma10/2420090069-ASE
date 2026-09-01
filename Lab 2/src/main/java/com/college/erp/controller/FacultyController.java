package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.FacultyRequest;
import com.college.erp.dto.FacultyResponse;
import com.college.erp.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Iteration 5 - faculty management endpoints (FR-08).
 */
@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public ApiResponse<List<FacultyResponse>> list(@RequestParam(required = false) String department) {
        return ApiResponse.ok(facultyService.findAll(department));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('FACULTY')")
    public ApiResponse<FacultyResponse> me(Principal principal) {
        return ApiResponse.ok(facultyService.findByUsername(principal.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<FacultyResponse> byId(@PathVariable Long id) {
        return ApiResponse.ok(facultyService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FacultyResponse> create(@Valid @RequestBody FacultyRequest request) {
        return ApiResponse.ok(facultyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FacultyResponse> update(@PathVariable Long id, @Valid @RequestBody FacultyRequest request) {
        return ApiResponse.ok(facultyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ApiResponse.ok("Faculty member removed");
    }
}
