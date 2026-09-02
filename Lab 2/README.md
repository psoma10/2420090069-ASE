# College ERP System

A College Enterprise Resource Planning system built with Spring Boot, developed
using the **Iterative software development model** for ASE Lab 2 (PRD 02).

The system provides a centralised platform for managing academic information.
Students log in to view their profile, register for courses, and check their
attendance, marks and results. Faculty manage the courses they teach, record
attendance, enter marks and publish results. Administrators manage students,
faculty and the course catalogue.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 (Web, Data JPA, Security, Validation) |
| Persistence | Hibernate / JPA |
| Database | MySQL 8 (production profile), H2 in MySQL-compatibility mode (default dev profile) |
| Security | Spring Security — session authentication, BCrypt password hashing, method-level role checks |
| Frontend | HTML5, handwritten CSS, vanilla JavaScript (no framework, no CDN — works offline) |
| Build | Maven |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test |
| Version control | Git |

---

## How the Iterative model was applied

The Iterative model starts from an initial set of requirements and grows the
system through repeated cycles, each producing a working, improved version and
each allowing requirements to be refined in the light of what the previous cycle
revealed.

This project was delivered in **five iterations**:

| Iteration | Theme | Functional requirements |
|---|---|---|
| 1 | Authentication and student information | FR-01, FR-02 |
| 2 | Course management and registration | FR-03, FR-04 |
| 3 | Attendance | FR-05 |
| 4 | Marks and results | FR-06, FR-07 |
| 5 | ERP integration: faculty, navigation, usability | FR-08 |

Every iteration ended with a runnable application. Three requirements were
genuinely *refined* mid-project rather than merely implemented — the
`{success, data, error}` response envelope (added in iteration 2 once
registration produced business failures that HTTP status codes could not
express), bulk attendance entry (added in iteration 3 once marking a class one
student at a time proved unusable), and the separation of `Marks` from `Result`
(iteration 4, so that correcting a mark does not silently rewrite a result a
student has already seen).

**[docs/ITERATIONS.md](docs/ITERATIONS.md) is the full iteration record**, with
what was built, which FRs and user stories each increment satisfied, and what
each iteration changed about the one before it.

---

## Prerequisites

- **JDK 21** or later — check with `java -version`
- **Maven 3.9+** — check with `mvn -version` (or use the bundled `mvnw` wrapper)
- **MySQL 8** — only required for the `mysql` profile; the default profile needs
  no database server at all

---

## Running the application

### Default (H2 development profile)

The default profile uses an in-memory H2 database running in MySQL
compatibility mode, so the ERP starts with no database server installed:

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080**.

The H2 web console is available at http://localhost:8080/h2-console with JDBC
URL `jdbc:h2:mem:collegeerp`, user `sa` and an empty password. Data is held in
memory and is discarded when the application stops.

### MySQL profile

MySQL is the production database named in PRD section 4. Credentials come from
the environment, never from source control:

```bash
# Windows PowerShell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
mvn spring-boot:run -D"spring-boot.run.profiles=mysql"

# macOS / Linux
export DB_USERNAME=root
export DB_PASSWORD=your-password
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Optional overrides: `DB_HOST` (default `localhost`), `DB_PORT` (default `3306`),
`DB_NAME` (default `college_erp`). The connection string uses
`createDatabaseIfNotExist=true` and Hibernate creates the tables on first start,
so no manual setup is required. To provision the schema explicitly instead, run
the documented DDL:

```bash
mysql -u root -p < docs/schema.sql
```

### Running the tests

```bash
mvn test
```

Tests run against the default H2 profile and need no external database.

### Packaging

```bash
mvn clean package
java -jar target/college-erp-1.0.0.jar
```

---

## Demo logins

A `DataSeeder` component creates three accounts at startup if they do not
already exist. Passwords are **BCrypt-hashed before being stored** — the plain
values below exist only so the application can be demonstrated.

| Username | Password | Role | Sees |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Students, Faculty, Courses |
| `faculty1` | `faculty123` | FACULTY | My Courses, Mark Attendance, Enter Marks, Publish Results |
| `student1` | `student123` | STUDENT | Profile, Courses & Register, My Attendance, My Marks, My Results |

Change these before deploying anywhere real.

---

## Package layout

```
Lab 2/
├── pom.xml
├── README.md
├── docs/
│   ├── API.md                     REST endpoint reference
│   ├── ITERATIONS.md              Iterative-model development record
│   └── schema.sql                 MySQL DDL matching the JPA entities
└── src/
    ├── main/
    │   ├── java/com/college/erp/
    │   │   ├── CollegeErpApplication.java
    │   │   ├── config/            SecurityConfig, AuthenticationConfig, DataSeeder
    │   │   ├── controller/        REST endpoints (@RestController)
    │   │   ├── dto/               Request/response records + the ApiResponse envelope
    │   │   ├── entity/            JPA entities: User, Student, Faculty, Course,
    │   │   │                      Enrollment, Attendance, Marks, Result, and the
    │   │   │                      Role / AttendanceStatus enums
    │   │   ├── exception/         GlobalExceptionHandler, NotFoundException,
    │   │   │                      BadRequestException
    │   │   ├── repository/        Spring Data JPA repositories
    │   │   └── service/           Business logic and transaction boundaries
    │   └── resources/
    │       ├── application.properties          default (H2) profile
    │       ├── application-mysql.properties    MySQL profile
    │       └── static/
    │           ├── index.html     single-page ERP shell
    │           ├── css/style.css  portal stylesheet
    │           └── js/app.js      fetch wrapper, routing, forms and tables
    └── test/java/com/college/erp/               unit and integration tests
```

The architecture is a conventional layered Spring Boot design: controllers
handle HTTP and delegate immediately, services own the business rules and the
transaction boundary, repositories own persistence, and DTOs cross the web
boundary so no JPA entity or lazy proxy is ever serialised
(`spring.jpa.open-in-view` is `false`).

---

## Functional requirements traceability

| FR | Requirement | Implemented by | API |
|---|---|---|---|
| **FR-01** | Student authentication — secure login | `config/SecurityConfig.java`, `service/AuthService.java`, `service/UserDetailsServiceImpl.java`, `controller/AuthController.java`, `entity/User.java` | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| **FR-02** | Student profile and academic information | `entity/Student.java`, `repository/StudentRepository.java`, `service/StudentService.java`, `controller/StudentController.java`, `dto/StudentRequest.java`, `dto/StudentResponse.java` | `GET/POST /api/students`, `GET /api/students/me`, `GET/PUT/DELETE /api/students/{id}` |
| **FR-03** | Course creation, update and management | `entity/Course.java`, `repository/CourseRepository.java`, `service/CourseService.java`, `controller/CourseController.java`, `dto/CourseRequest.java`, `dto/CourseResponse.java` | `GET/POST /api/courses`, `GET/PUT/DELETE /api/courses/{id}` |
| **FR-04** | Student course registration | `entity/Enrollment.java`, `repository/EnrollmentRepository.java`, `service/EnrollmentService.java`, `controller/EnrollmentController.java`, `dto/EnrollmentResponse.java` | `POST /api/enrollments/register`, `GET /api/enrollments/me`, `GET /api/enrollments/course/{courseId}`, `DELETE /api/enrollments/{id}` |
| **FR-05** | Attendance recording and viewing | `entity/Attendance.java`, `entity/AttendanceStatus.java`, `repository/AttendanceRepository.java`, `service/AttendanceService.java`, `controller/AttendanceController.java`, `dto/AttendanceRequest.java`, `dto/BulkAttendanceRequest.java`, `dto/AttendanceSummary.java` | `POST /api/attendance`, `POST /api/attendance/bulk`, `PUT /api/attendance/{id}`, `GET /api/attendance/me`, `GET /api/attendance/me/summary`, `GET /api/attendance/course/{courseId}` |
| **FR-06** | Faculty marks entry | `entity/Marks.java`, `repository/MarksRepository.java`, `service/MarksService.java`, `controller/MarksController.java` | `POST /api/marks`, `PUT /api/marks/{id}`, `GET /api/marks/me`, `GET /api/marks/course/{courseId}` |
| **FR-07** | Examination results | `entity/Result.java` (grading scale and pass mark), `repository/ResultRepository.java`, `service/ResultService.java`, `controller/ResultController.java` | `POST /api/results/publish/{courseId}`, `GET /api/results/me`, `GET /api/results/student/{studentId}` |
| **FR-08** | Faculty information and responsibilities | `entity/Faculty.java`, `repository/FacultyRepository.java`, `service/FacultyService.java`, `controller/FacultyController.java`, `dto/FacultyRequest.java`, `dto/FacultyResponse.java`, plus `Course.faculty` assignment | `GET/POST /api/faculty`, `GET /api/faculty/me`, `PUT/DELETE /api/faculty/{id}`, `PUT /api/courses/{id}/faculty/{facultyId}` |

Full request and response shapes for every endpoint are in
**[docs/API.md](docs/API.md)**.

### User stories

| Story | Satisfied by |
|---|---|
| **US-01** — As a student, I want to log in securely | FR-01; login screen → role-aware dashboard |
| **US-02** — As a student, I want to register for courses | FR-04; *Courses & Register* screen |
| **US-03** — As a student, I want to view my attendance | FR-05; *My Attendance* screen, with per-course percentages |
| **US-04** — As a faculty member, I want to enter student marks | FR-06; *Enter Marks* screen |
| **US-05** — As a student, I want to view my results | FR-07; *My Results* screen, with grade and pass status |

---

## Notes on security

- Passwords are stored **BCrypt-hashed** and are never returned by any endpoint.
- Database credentials for the MySQL profile are read from the environment
  (`DB_USERNAME`, `DB_PASSWORD`); nothing sensitive is committed.
- Every endpoint carries an explicit role rule; unauthenticated requests receive
  HTTP 401 and the frontend returns to the login screen.
- All input is validated with Jakarta Bean Validation at the controller boundary,
  and the critical rules (marks ranges, uniqueness of roll numbers, course codes,
  registrations, attendance per day) are additionally enforced by database
  constraints — see [docs/schema.sql](docs/schema.sql).
- CSRF protection is disabled for the lab API because the client is a
  same-origin JavaScript application with no cross-site form posts. A production
  deployment should re-enable it with a cookie token repository.
