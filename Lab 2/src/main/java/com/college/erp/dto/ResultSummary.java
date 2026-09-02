package com.college.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Iteration 4 - aggregate view of a student's published results (FR-07, US-05).
 */
public record ResultSummary(
        int totalCourses,
        int coursesPassed,
        int totalMarks,
        double percentage) {

    /**
     * Builds a summary over the published results of one student, rounding the
     * aggregate percentage to two decimal places and reporting 0 when the student
     * has no published results yet (divide-by-zero guard).
     */
    public static ResultSummary of(Collection<ResultResponse> results) {
        int totalCourses = results.size();
        int coursesPassed = (int) results.stream().filter(ResultResponse::passed).count();
        int totalMarks = results.stream().mapToInt(ResultResponse::totalMarks).sum();

        double percentage = 0.0;
        if (totalCourses > 0) {
            percentage = BigDecimal.valueOf(totalMarks / (double) totalCourses)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new ResultSummary(totalCourses, coursesPassed, totalMarks, percentage);
    }
}
