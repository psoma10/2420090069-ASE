package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.MarksRequest;
import com.college.erp.dto.MarksResponse;
import com.college.erp.service.MarksService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Iteration 4 - marks entry endpoints (FR-06, US-04).
 *
 * A student may only reach {@code /me}, which resolves their own record from the
 * principal, so one student can never read another student's marks.
 */
@RestController
@RequestMapping("/api/marks")
public class MarksController {

    private final MarksService marksService;

    public MarksController(MarksService marksService) {
        this.marksService = marksService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<MarksResponse> enter(@Valid @RequestBody MarksRequest request) {
        return ApiResponse.ok(marksService.enter(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<MarksResponse> update(@PathVariable Long id, @Valid @RequestBody MarksRequest request) {
        return ApiResponse.ok(marksService.update(id, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<MarksResponse>> me(Principal principal) {
        return ApiResponse.ok(marksService.findForUsername(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<MarksResponse> byId(@PathVariable Long id) {
        return ApiResponse.ok(marksService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<List<MarksResponse>> byStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(marksService.findByStudent(studentId));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<List<MarksResponse>> byCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(marksService.findByCourse(courseId));
    }
}
