package com.college.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Iteration 5 - faculty information and academic responsibilities (FR-08).
 */
@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 20)
    private String employeeCode;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String department;

    @Column(length = 80)
    private String designation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    protected Faculty() {
    }

    public Faculty(String employeeCode, String name, String department, String designation, User user) {
        this.employeeCode = employeeCode;
        this.name = name;
        this.department = department;
        this.designation = designation;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getDesignation() {
        return designation;
    }

    public User getUser() {
        return user;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
