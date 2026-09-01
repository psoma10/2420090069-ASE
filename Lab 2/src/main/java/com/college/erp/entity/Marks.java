package com.college.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Iteration 4 - faculty marks entry (FR-06).
 * Internal marks are out of 40 and external marks out of 60, giving a total out of 100.
 */
@Entity
@Table(
        name = "marks",
        uniqueConstraints = @UniqueConstraint(name = "uk_marks_student_course",
                columnNames = {"student_id", "course_id"})
)
public class Marks {

    public static final int MAX_INTERNAL = 40;
    public static final int MAX_EXTERNAL = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @Min(0)
    @Max(MAX_INTERNAL)
    @Column(nullable = false)
    private Integer internalMarks;

    @NotNull
    @Min(0)
    @Max(MAX_EXTERNAL)
    @Column(nullable = false)
    private Integer externalMarks;

    protected Marks() {
    }

    public Marks(Student student, Course course, Integer internalMarks, Integer externalMarks) {
        this.student = student;
        this.course = course;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
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

    public Integer getInternalMarks() {
        return internalMarks;
    }

    public Integer getExternalMarks() {
        return externalMarks;
    }

    public void setInternalMarks(Integer internalMarks) {
        this.internalMarks = internalMarks;
    }

    public void setExternalMarks(Integer externalMarks) {
        this.externalMarks = externalMarks;
    }

    /** Total out of 100. */
    public int getTotal() {
        return internalMarks + externalMarks;
    }
}
