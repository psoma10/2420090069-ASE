-- =========================================================================
-- College ERP System - MySQL schema
--
-- This DDL mirrors the JPA entities under com.college.erp.entity. Hibernate
-- generates the same tables automatically (spring.jpa.hibernate.ddl-auto=update),
-- so this file exists as the documented, reviewable schema deliverable required
-- by PRD section 13 and as a way to provision the database up front.
--
-- Usage:
--   mysql -u root -p < docs/schema.sql
-- =========================================================================

CREATE DATABASE IF NOT EXISTS college_erp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE college_erp;

-- Drop in reverse dependency order so the script is re-runnable.
DROP TABLE IF EXISTS results;
DROP TABLE IF EXISTS marks;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS faculty;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;

-- -------------------------------------------------------------------------
-- Iteration 1 - authentication (FR-01)
-- One login account per person, shared by students, faculty and admins.
-- Passwords are stored BCrypt-hashed, never in plain text.
-- -------------------------------------------------------------------------
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     BIT(1)       NOT NULL DEFAULT b'1',
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'FACULTY', 'ADMIN'))
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Iteration 1 - student profile and academic information (FR-02)
-- -------------------------------------------------------------------------
CREATE TABLE students (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    roll_number VARCHAR(20) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    department  VARCHAR(60) NOT NULL,
    semester    INT         NOT NULL,
    phone       VARCHAR(15) NULL,
    user_id     BIGINT      NULL,
    CONSTRAINT pk_students PRIMARY KEY (id),
    CONSTRAINT uk_students_roll_number UNIQUE (roll_number),
    CONSTRAINT uk_students_user        UNIQUE (user_id),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE INDEX ix_students_department ON students (department);

-- -------------------------------------------------------------------------
-- Iteration 5 - faculty information and responsibilities (FR-08)
-- -------------------------------------------------------------------------
CREATE TABLE faculty (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    employee_code VARCHAR(20)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    department    VARCHAR(60)  NOT NULL,
    designation   VARCHAR(80)  NULL,
    user_id       BIGINT       NULL,
    CONSTRAINT pk_faculty PRIMARY KEY (id),
    CONSTRAINT uk_faculty_employee_code UNIQUE (employee_code),
    CONSTRAINT uk_faculty_user          UNIQUE (user_id),
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE INDEX ix_faculty_department ON faculty (department);

-- -------------------------------------------------------------------------
-- Iteration 2 - course creation and management (FR-03)
-- faculty_id is nullable: a course may exist before a teacher is assigned.
-- -------------------------------------------------------------------------
CREATE TABLE courses (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(20)  NOT NULL,
    title      VARCHAR(120) NOT NULL,
    credits    INT          NOT NULL,
    semester   INT          NOT NULL,
    department VARCHAR(60)  NOT NULL,
    faculty_id BIGINT       NULL,
    CONSTRAINT pk_courses PRIMARY KEY (id),
    CONSTRAINT uk_courses_code UNIQUE (code),
    CONSTRAINT fk_courses_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id),
    CONSTRAINT ck_courses_credits CHECK (credits > 0)
) ENGINE = InnoDB;

CREATE INDEX ix_courses_department_semester ON courses (department, semester);

-- -------------------------------------------------------------------------
-- Iteration 2 - student course registration (FR-04)
-- A student may register for a given course only once.
-- -------------------------------------------------------------------------
CREATE TABLE enrollments (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    student_id    BIGINT NOT NULL,
    course_id     BIGINT NOT NULL,
    registered_on DATE   NOT NULL,
    CONSTRAINT pk_enrollments PRIMARY KEY (id),
    CONSTRAINT uk_enrollment_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollments_course  FOREIGN KEY (course_id)  REFERENCES courses (id)
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Iteration 3 - attendance records (FR-05)
-- One attendance record per student, per course, per day.
-- -------------------------------------------------------------------------
CREATE TABLE attendance (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    student_id BIGINT      NOT NULL,
    course_id  BIGINT      NOT NULL,
    class_date DATE        NOT NULL,
    status     VARCHAR(10) NOT NULL,
    CONSTRAINT pk_attendance PRIMARY KEY (id),
    CONSTRAINT uk_attendance_student_course_date UNIQUE (student_id, course_id, class_date),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_attendance_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT'))
) ENGINE = InnoDB;

CREATE INDEX ix_attendance_course_date ON attendance (course_id, class_date);

-- -------------------------------------------------------------------------
-- Iteration 4 - faculty marks entry (FR-06)
-- Internal marks are out of 40, external out of 60, total out of 100.
-- -------------------------------------------------------------------------
CREATE TABLE marks (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    student_id     BIGINT NOT NULL,
    course_id      BIGINT NOT NULL,
    internal_marks INT    NOT NULL,
    external_marks INT    NOT NULL,
    CONSTRAINT pk_marks PRIMARY KEY (id),
    CONSTRAINT uk_marks_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_marks_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_marks_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT ck_marks_internal CHECK (internal_marks BETWEEN 0 AND 40),
    CONSTRAINT ck_marks_external CHECK (external_marks BETWEEN 0 AND 60)
) ENGINE = InnoDB;

-- -------------------------------------------------------------------------
-- Iteration 4 - published examination results (FR-07)
-- Derived from marks at publish time so a published result stays stable
-- even if marks are later corrected and re-published.
-- -------------------------------------------------------------------------
CREATE TABLE results (
    id           BIGINT     NOT NULL AUTO_INCREMENT,
    student_id   BIGINT     NOT NULL,
    course_id    BIGINT     NOT NULL,
    total_marks  INT        NOT NULL,
    grade        VARCHAR(2) NOT NULL,
    passed       BIT(1)     NOT NULL,
    published_on DATETIME(6) NOT NULL,
    CONSTRAINT pk_results PRIMARY KEY (id),
    CONSTRAINT uk_result_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_results_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT ck_results_total CHECK (total_marks BETWEEN 0 AND 100)
) ENGINE = InnoDB;

CREATE INDEX ix_results_course ON results (course_id);
