package com.college.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Iteration 2 - course creation and management (FR-03).
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String title;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer credits;

    @NotNull
    @Column(nullable = false)
    private Integer semester;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    protected Course() {
    }

    public Course(String code, String title, Integer credits, Integer semester, String department) {
        this.code = code;
        this.title = title;
        this.credits = credits;
        this.semester = semester;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public Integer getCredits() {
        return credits;
    }

    public Integer getSemester() {
        return semester;
    }

    public String getDepartment() {
        return department;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }
}
