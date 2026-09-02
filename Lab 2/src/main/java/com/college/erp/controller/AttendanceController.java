package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.AttendanceRequest;
import com.college.erp.dto.AttendanceResponse;
import com.college.erp.dto.AttendanceSummary;
import com.college.erp.dto.BulkAttendanceRequest;
import com.college.erp.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Iteration 3 - attendance endpoints (FR-05, US-03).
 *
 * Marking and staff reports require FACULTY or ADMIN. The {@code /me} endpoints
 * resolve the student from the authenticated principal, so a student can only ever
 * read their own attendance.
 */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> mark(@Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse body = attendanceService.mark(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> markBulk(
            @Valid @RequestBody BulkAttendanceRequest request) {
        List<AttendanceResponse> body = attendanceService.markBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.updateStatus(id, request.status())));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> myAttendance(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.findForUsername(principal.getName())));
    }

    @GetMapping("/me/summary")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<AttendanceSummary>>> mySummary(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.summaryForUsername(principal.getName())));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> byStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.findByStudent(studentId)));
    }

    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceSummary>>> studentSummary(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.summaryForStudent(studentId)));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> byCourse(
            @PathVariable Long courseId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.findByCourse(courseId, date)));
    }
}
