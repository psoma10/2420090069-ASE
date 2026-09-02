package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.ResultResponse;
import com.college.erp.dto.ResultSummary;
import com.college.erp.service.ResultService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Iteration 4 - result publication and student result view (FR-07, US-05).
 *
 * A student may only reach {@code /me}, which resolves their own record from the
 * principal, so one student can never read another student's results.
 */
@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /** A student's own result card: every published result plus the aggregate summary. */
    public record MyResults(List<ResultResponse> results, ResultSummary summary) {
    }

    @PostMapping("/publish/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<String> publishCourse(@PathVariable Long courseId) {
        int published = resultService.publishCourse(courseId);
        return ApiResponse.ok("Published " + published + " result(s)");
    }

    @PostMapping("/publish/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<ResultResponse> publishStudent(@PathVariable Long studentId,
                                                      @PathVariable Long courseId) {
        return ApiResponse.ok(resultService.publishStudent(studentId, courseId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<MyResults> me(Principal principal) {
        String username = principal.getName();
        List<ResultResponse> results = resultService.findForUsername(username);
        return ApiResponse.ok(new MyResults(results, ResultSummary.of(results)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<List<ResultResponse>> byStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(resultService.findByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<ResultSummary> summaryByStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(resultService.summaryForStudent(studentId));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ApiResponse<List<ResultResponse>> byCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(resultService.findByCourse(courseId));
    }
}
