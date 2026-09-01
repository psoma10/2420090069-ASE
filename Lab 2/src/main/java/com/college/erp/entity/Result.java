package com.college.erp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Iteration 4 - published examination result for one student in one course (FR-07).
 * Derived from {@link Marks} when the result is published, so that a published
 * result stays stable even if marks are later corrected and re-published.
 */
@Entity
@Table(
        name = "results",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_student_course",
                columnNames = {"student_id", "course_id"})
)
public class Result {

    private static final int PASS_MARK = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer totalMarks;

    @Column(nullable = false, length = 2)
    private String grade;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private LocalDateTime publishedOn;

    protected Result() {
    }

    public Result(Student student, Course course, int totalMarks) {
        this.student = student;
        this.course = course;
        apply(totalMarks);
    }

    /** Recomputes grade and pass status for a new total, used when re-publishing. */
    public void apply(int totalMarks) {
        this.totalMarks = totalMarks;
        this.grade = gradeFor(totalMarks);
        this.passed = totalMarks >= PASS_MARK;
        this.publishedOn = LocalDateTime.now();
    }

    /** Standard 10-point style grading used by the college. */
    public static String gradeFor(int total) {
        if (total >= 90) {
            return "A+";
        }
        if (total >= 80) {
            return "A";
        }
        if (total >= 70) {
            return "B";
        }
        if (total >= 60) {
            return "C";
        }
        if (total >= 50) {
            return "D";
        }
        if (total >= PASS_MARK) {
            return "E";
        }
        return "F";
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Course getCourse() {
        return course;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public String getGrade() {
        return grade;
    }

    public boolean isPassed() {
        return passed;
    }

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }
}
