package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.EnrollmentResponse;
import com.college.erp.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Iteration 2 - student course registration endpoints (FR-04, US-02).
 *
 * The registering student is always taken from the authenticated principal, so
 * a student can never register or drop on behalf of somebody else.
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /** Body of a registration request; the student is never supplied by the client. */
    public record RegisterRequest(@NotNull(message = "is required") Long courseId) {
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                                    Authentication authentication) {
        EnrollmentResponse created = enrollmentService.register(authentication.getName(), request.courseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> myEnrollments(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.findMyEnrollments(authentication.getName())));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> roster(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(enrollmentService.findCourseRoster(courseId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> drop(@PathVariable Long id, Authentication authentication) {
        enrollmentService.drop(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(ApiResponse.ok("Enrollment dropped"));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMIN::equals);
    }
}
