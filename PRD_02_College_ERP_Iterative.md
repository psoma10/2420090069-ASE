# PRD 02 — College ERP System using Iterative Model

## 1. Project Overview
Design and implement a College ERP System using the Iterative software development model. The system shall manage core academic activities including student registration/login, course management, attendance, marks/results, faculty management and student information.

## 2. Objective
- Identify a suitable real-world case study.
- Select the Iterative process model.
- Select a suitable programming language.
- Develop the system through repeated iterations.
- Improve the system version by version as requirements are refined.

## 3. Case Study
The College ERP System provides a centralized platform for managing student and academic information. Students can access their academic information, while faculty and administrators manage courses, attendance, marks and related records.

## 4. Technology Stack
- Programming Language: Java
- Backend Framework: Spring Boot
- Frontend: HTML, CSS, JavaScript
- Database: MySQL
- Version Control: Git/GitHub

## 5. Users

### Student
- Log in
- Manage/view student information
- Register for courses
- View attendance
- View marks and results

### Faculty
- Manage courses
- Record student attendance
- Enter student marks
- View academic information

### Administrator
- Manage students
- Manage faculty
- Manage courses
- Manage academic records

## 6. Core Functional Requirements

### FR-01 — Student Authentication
Students shall be able to securely log in and access their academic information.

### FR-02 — Student Information
The system shall maintain student profile and academic information.

### FR-03 — Course Management
The system shall allow courses to be created, updated and managed.

### FR-04 — Course Registration
Students shall be able to register for available courses.

### FR-05 — Attendance
Faculty shall be able to record attendance and students shall be able to view their attendance.

### FR-06 — Marks
Faculty shall be able to enter marks for students.

### FR-07 — Results
Students shall be able to view their examination results.

### FR-08 — Faculty Management
The system shall maintain faculty information and their academic responsibilities.

## 7. User Stories

### US-01 — Student Login
**As a student, I want to log in securely, so that I can access my academic information.**

### US-02 — Course Registration
**As a student, I want to register for courses, so that I can enroll in my subjects.**

### US-03 — View Attendance
**As a student, I want to view my attendance, so that I can monitor my academic progress.**

### US-04 — Faculty Enter Marks
**As a faculty member, I want to enter student marks, so that students can view their academic performance.**

### US-05 — View Results
**As a student, I want to view my examination results, so that I can evaluate my performance.**

## 8. Iterative Development Model
The Iterative Model starts with initial requirements and develops the system through repeated iterations. Each iteration produces an improved version of the software and allows changes or refinements to functional requirements.

## 9. Iteration Plan

### Iteration 1 — Authentication and Student Information
- Student login
- Student profile
- Basic database structure

### Iteration 2 — Course Management
- Course creation and management
- Course listing
- Student course registration

### Iteration 3 — Attendance
- Faculty attendance entry
- Student attendance viewing
- Attendance records

### Iteration 4 — Marks and Results
- Faculty marks entry
- Result calculation/storage
- Student result viewing

### Iteration 5 — ERP Integration
- Faculty management
- Connect all modules
- Improve navigation and usability
- Refine requirements based on previous iterations

## 10. Suggested Data Entities
- Student
- Faculty
- Course
- Enrollment
- Attendance
- Marks
- Result
- User/Admin

## 11. Implementation Plan
1. Create Spring Boot project.
2. Configure MySQL database.
3. Create entity and repository layers.
4. Implement authentication.
5. Implement course and enrollment modules.
6. Implement attendance module.
7. Implement marks and results modules.
8. Implement faculty and administration modules.
9. Integrate modules and refine each iteration.
10. Prepare the final integrated ERP application.

## 12. Definition of Done
A feature is considered complete when:
- Required functionality is implemented.
- Database operations work correctly.
- Input validation is provided.
- The feature integrates with existing modules.
- Basic functional tests pass.
- Code is committed to version control.

## 13. Deliverables
- Java/Spring Boot source code
- MySQL database schema
- Frontend pages
- Authentication module
- Course and enrollment module
- Attendance module
- Marks/results module
- Faculty/admin module
- Git repository
