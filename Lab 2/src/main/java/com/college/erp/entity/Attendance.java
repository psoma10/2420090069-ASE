package com.college.erp.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Iteration 3 - attendance records (FR-05).
 * One attendance record per student, per course, per day.
 */
@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_student_course_date",
                columnNames = {"student_id", "course_id", "class_date"})
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceStatus status;

    protected Attendance() {
    }

    public Attendance(Student student, Course course, LocalDate classDate, AttendanceStatus status) {
        this.student = student;
        this.course = course;
        this.classDate = classDate;
        this.status = status;
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

    public LocalDate getClassDate() {
        return classDate;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
