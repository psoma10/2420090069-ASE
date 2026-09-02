package com.college.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Iteration 3 - per-course attendance percentage for a student (FR-05, US-03).
 */
public record AttendanceSummary(
        Long courseId,
        String courseCode,
        String courseTitle,
        long totalClasses,
        long presentCount,
        double percentage) {

    /**
     * Builds a summary, rounding the percentage to two decimal places and
     * reporting 0 when no classes have been held yet (divide-by-zero guard).
     */
    public static AttendanceSummary of(Long courseId, String courseCode, String courseTitle,
                                       long totalClasses, long presentCount) {
        double percentage = 0.0;
        if (totalClasses > 0) {
            percentage = BigDecimal.valueOf(presentCount * 100.0 / totalClasses)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new AttendanceSummary(courseId, courseCode, courseTitle, totalClasses, presentCount, percentage);
    }
}
