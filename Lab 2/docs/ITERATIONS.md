# Iterative Development Record — College ERP System

This document is the evidence artifact for the **Iterative software development
model** required by PRD 02. It records, for each of the five iterations defined
in PRD section 9, what was built, which functional requirements and user stories
the increment satisfied, and — most importantly for the Iterative model — what
was *refined* in that iteration as a consequence of what the previous increment
revealed.

## Why the Iterative model was chosen

A College ERP is a well-understood problem domain, but its requirements are
broad and interdependent: attendance is meaningless without courses, results are
meaningless without marks, and marks are meaningless without enrolment. Building
all eight functional requirements in one pass (Waterfall) would have deferred
every integration risk to the very end. Equally, the requirements were stable
enough that a fully adaptive process was unnecessary.

The Iterative model fits because it lets each increment deliver a **working,
demonstrable version** of the system while leaving later requirements open to
refinement. Every iteration below ends with a runnable application, and each one
changed at least one decision made in an earlier iteration — which is precisely
the behaviour the model is meant to exhibit.

### Iteration summary

| # | Theme | FRs delivered | User stories | Key refinement of the previous increment |
|---|---|---|---|---|
| 1 | Authentication and student information | FR-01, FR-02 | US-01 | *(baseline)* |
| 2 | Course management and registration | FR-03, FR-04 | US-02 | Role model generalised; response envelope introduced |
| 3 | Attendance | FR-05 | US-03 | Bulk entry added; enrolment made the gate for attendance |
| 4 | Marks and results | FR-06, FR-07 | US-04, US-05 | Marks and results separated into distinct concepts |
| 5 | ERP integration | FR-08 | *(all, end-to-end)* | Faculty promoted to a first-class entity; unified UI shell |

---

## Iteration 1 — Authentication and Student Information

**PRD scope:** student login, student profile, basic database structure.

### What was built

- Spring Boot 3.3.4 / Java 21 project skeleton with the layered package
  structure (`entity` → `repository` → `service` → `controller`, with `dto`,
  `config` and `exception` supporting packages).
- `User` entity as the single authentication account, carrying `username`,
  BCrypt-hashed `password`, `fullName`, `email`, `role` and an `enabled` flag.
  Unique constraints on `username` and `email`.
- `Student` entity holding the academic profile — `rollNumber` (unique), `name`,
  `department`, `semester`, `phone` — linked one-to-one to a `User`.
- `SecurityConfig` with `BCryptPasswordEncoder`, session-based authentication,
  and an entry point that returns **HTTP 401** instead of redirecting to a login
  page, because the client is a JavaScript single-page application.
- `UserDetailsServiceImpl` loading accounts from the database.
- `AuthController` (`/api/auth/login`, `/api/auth/logout`, `/api/auth/me`) and
  `StudentController` with the full student CRUD surface.
- `DataSeeder` creating the demo accounts (`admin`, `faculty1`, `student1`) with
  hashed passwords at startup.
- H2 in MySQL-compatibility mode as the dev datasource so the application runs
  and tests execute without a local MySQL server; the MySQL profile
  (`application-mysql.properties`) was configured in the same iteration so the
  PRD's target database was never an afterthought.

### Requirements satisfied

| Requirement | How |
|---|---|
| **FR-01 — Student Authentication** | `POST /api/auth/login` authenticates against BCrypt-hashed credentials and establishes a session. |
| **FR-02 — Student Information** | `Student` entity plus `GET/POST/PUT/DELETE /api/students`, and `GET /api/students/me` for the signed-in student. |
| **US-01 — Student Login** | A student signs in and immediately reaches their own profile. |

### Refinements made in this iteration

This was the baseline increment, but two decisions were deliberately taken early
because they were expensive to retrofit:

1. **A separate `User` entity rather than credentials on `Student`.** The PRD
   names three user types. Putting `username`/`password` on `Student` would have
   forced a parallel credential scheme for faculty in iteration 5. The one-to-one
   `Student → User` link kept authentication in a single place.
2. **`spring.jpa.open-in-view=false`.** Turning this off from the start forced
   every association to be flattened into a DTO inside the service transaction,
   which prevented the lazy-initialisation failures that would otherwise have
   surfaced only once nested relationships appeared in iteration 2.

---

## Iteration 2 — Course Management and Registration

**PRD scope:** course creation and management, course listing, student course
registration.

### What was built

- `Course` entity — unique `code`, `title`, `credits`, `semester`, `department`,
  and a nullable `faculty_id` reserved for iteration 5.
- `Enrollment` entity joining `Student` to `Course` with a `registeredOn` date
  and the unique constraint `uk_enrollment_student_course`, so the same student
  cannot register for the same course twice.
- `CourseController` with admin-only create/update/delete and read access for
  every authenticated role, plus filtering by `?department=` and `?semester=`.
- `EnrollmentController`: `POST /api/enrollments/register` for the student,
  `GET /api/enrollments/me` for their own registrations,
  `GET /api/enrollments/course/{courseId}` for a faculty roster, and
  `DELETE /api/enrollments/{id}` to drop.
- `CourseResponse` and `EnrollmentResponse` DTOs that flatten the faculty and
  course associations rather than serialising entities directly.

### Requirements satisfied

| Requirement | How |
|---|---|
| **FR-03 — Course Management** | Full CRUD on `/api/courses`, restricted to `ADMIN`. |
| **FR-04 — Course Registration** | `POST /api/enrollments/register` with duplicate protection. |
| **US-02 — Course Registration** | A student browses the filtered catalogue and enrols in one click. |

### Refined from Iteration 1

1. **The role model was generalised.** Iteration 1 effectively treated the system
   as "students plus an admin". Introducing course management made the
   `STUDENT` / `FACULTY` / `ADMIN` distinction load-bearing, so method-level
   security was switched on (`@EnableMethodSecurity`) and each endpoint was given
   an explicit role rule instead of relying on a blanket "authenticated" rule.
2. **The `ApiResponse` envelope was introduced.** Iteration 1 returned bare
   objects, which meant the frontend had to interpret HTTP status codes and
   parse error bodies in different shapes. Registration produces genuine
   *business* failures ("already registered", "course does not exist") that are
   not exceptional at the HTTP level, so every endpoint was retrofitted to the
   `{success, data, error}` envelope and a `GlobalExceptionHandler` was added to
   normalise validation and not-found errors into the same shape. This is the
   clearest example in the project of a later iteration changing an earlier
   design decision.
3. **`Course.faculty` was added as a nullable field.** The requirement to assign
   teachers belongs to iteration 5, but leaving the column out would have meant a
   schema migration later. It was added nullable and left unused until then.

---

## Iteration 3 — Attendance

**PRD scope:** faculty attendance entry, student attendance viewing, attendance
records.

### What was built

- `AttendanceStatus` enum (`PRESENT`, `ABSENT`) and the `Attendance` entity, with
  the unique constraint `uk_attendance_student_course_date` enforcing exactly one
  record per student, per course, per day.
- `POST /api/attendance` to mark one student, and `PUT /api/attendance/{id}` to
  correct a record after the fact.
- `POST /api/attendance/bulk` accepting `BulkAttendanceRequest` — one course, one
  date, and one entry per student — so a teacher can mark an entire class in a
  single request.
- `GET /api/attendance/me` (the student's own records) and
  `GET /api/attendance/course/{courseId}?date=` (the faculty register).
- `GET /api/attendance/me/summary` returning `AttendanceSummary` per course:
  classes held, classes present, and a percentage rounded to two decimal places
  with an explicit divide-by-zero guard for courses where no class has been held.

### Requirements satisfied

| Requirement | How |
|---|---|
| **FR-05 — Attendance** | Faculty record via `POST /api/attendance` and `/bulk`; students read via `/api/attendance/me`. |
| **US-03 — View Attendance** | The summary endpoint gives a student their per-course attendance percentage at a glance. |

### Refined from Iteration 2

1. **Bulk entry was added after the single-record API proved unusable.** The
   first design exposed only `POST /api/attendance`. Marking a class of thirty
   students meant thirty requests, which was unacceptable for the real workflow.
   `POST /api/attendance/bulk` was added in response — a requirement refinement
   driven directly by using the previous increment.
2. **Enrolment became a precondition for attendance.** Iteration 2 treated
   enrolment as a record-keeping convenience. Attendance made it a business rule:
   a student who is not registered for a course cannot be marked for it, so the
   attendance service now validates against `Enrollment` before writing.
3. **A summary view was recognised as a distinct requirement.** US-03 says a
   student wants to "monitor academic progress". A raw list of attendance rows
   does not do that, so the aggregated percentage endpoint was added — FR-05 was
   refined from "view attendance" to "view attendance *and* its summary".

---

## Iteration 4 — Marks and Results

**PRD scope:** faculty marks entry, result calculation and storage, student
result viewing.

### What was built

- `Marks` entity storing `internalMarks` (0–40) and `externalMarks` (0–60),
  validated at both the bean and database level, with `getTotal()` returning the
  score out of 100. Unique on `(student_id, course_id)`.
- `Result` entity storing the published outcome — `totalMarks`, `grade`,
  `passed`, `publishedOn` — unique on `(student_id, course_id)`.
- `Result.gradeFor(int)` implementing the grading scale (A+ ≥ 90, A ≥ 80, B ≥ 70,
  C ≥ 60, D ≥ 50, E ≥ 40, otherwise F) with a pass mark of 40, and
  `Result.apply(int)` recomputing grade, pass status and timestamp so a result
  can be re-published cleanly.
- `POST /api/marks` and `PUT /api/marks/{id}` for faculty entry;
  `GET /api/marks/course/{courseId}` for the course marks sheet;
  `GET /api/marks/me` for the student.
- `POST /api/results/publish/{courseId}` converting every marks row for a course
  into a published result in one operation, plus `GET /api/results/me` and
  `GET /api/results/student/{studentId}`.

### Requirements satisfied

| Requirement | How |
|---|---|
| **FR-06 — Marks** | `POST/PUT /api/marks` with range validation, restricted to faculty and admin. |
| **FR-07 — Results** | `POST /api/results/publish/{courseId}` computes and stores results; `GET /api/results/me` exposes them. |
| **US-04 — Faculty Enter Marks** | A teacher records internal and external marks per student per course. |
| **US-05 — View Results** | A student sees total, grade and pass status once results are published. |

### Refined from Iteration 3

1. **Marks and results were separated into two entities.** The initial design
   computed a grade on the fly from `Marks` and had no `Result` table at all.
   That was rejected because it made every marks correction retroactively change
   a result a student had already seen. Splitting them means marks are the
   teacher's working data and results are an explicit, timestamped publication —
   a genuine requirement refinement, and the reason `Result` stores `totalMarks`
   rather than reading it back from `Marks`.
2. **Publishing was made a per-course batch rather than per-student.** Publishing
   one student at a time repeated the usability problem iteration 3 hit with
   attendance, so the endpoint was designed as a course-level operation from the
   start — the lesson from the previous iteration applied before the mistake was
   repeated.
3. **Validation was pushed down to the database.** Iteration 3 relied on bean
   validation alone. Marks added `CHECK` constraints on the columns as well, so
   an out-of-range score cannot be written even by a path that bypasses the
   service layer.

---

## Iteration 5 — ERP Integration

**PRD scope:** faculty management, connect all modules, improve navigation and
usability, refine requirements based on previous iterations.

### What was built

**Faculty management (FR-08).**

- `Faculty` entity — unique `employeeCode`, `name`, `department`, `designation`,
  and a one-to-one link to a `User` with role `FACULTY`.
- `FacultyController`: `GET /api/faculty` (with `?department=` filtering),
  `GET /api/faculty/me`, and admin-only `POST` / `PUT` / `DELETE`.
- `PUT /api/courses/{id}/faculty/{facultyId}` activating the `Course.faculty`
  column that had been reserved since iteration 2, which finally connects
  teachers to the courses they are responsible for.

**Module integration.** With `Faculty` in place, the entity graph closes:
`User → Student/Faculty → Enrollment → Course → Attendance/Marks → Result`.
A faculty member's identity now determines which courses they may take
attendance for and enter marks against, so the four previously independent
modules operate as one system rather than four APIs sharing a database.

**Navigation and usability.** A single-page frontend was built with plain HTML,
handwritten CSS and vanilla JavaScript — no framework and no CDN, so it works
with no internet connection:

- `index.html` — a login screen and a role-aware dashboard shell; twelve sections
  as plain `div`s toggled by JavaScript, with semantic markup and a `<label>` on
  every input.
- `css/style.css` — top bar, sidebar navigation, cards, tables, form controls,
  success/error alert styles, and responsive breakpoints that collapse the
  sidebar into a hamburger menu on narrow screens.
- `js/app.js` — one `fetch` wrapper that attaches `credentials: 'same-origin'`,
  unwraps the `{success, data, error}` envelope, throws when `success` is false,
  renders the message into the alert area, and returns to the login screen on
  HTTP 401. Navigation items carry a `data-roles` attribute and are hidden when
  the signed-in role may not use them, so each role sees only its own five,
  four or three screens.

**Documentation.** `README.md`, `docs/API.md`, `docs/ITERATIONS.md` and
`docs/schema.sql` were written to complete the PRD section 13 deliverables.

### Requirements satisfied

| Requirement | How |
|---|---|
| **FR-08 — Faculty Management** | `Faculty` entity, `/api/faculty` CRUD, and course-to-faculty assignment. |
| **FR-01 … FR-07** | All re-verified end-to-end through the integrated UI rather than through individual API calls. |
| **US-01 … US-05** | Every user story is now completable through the browser without a REST client. |

### Refined from Iteration 4

1. **Faculty was promoted from a role to an entity.** Iterations 2–4 treated
   "faculty" as nothing more than `Role.FACULTY` on a `User`. That was sufficient
   for authorisation but could not answer "which courses does this teacher own?"
   or "what is their employee code and department?" — so FR-08 required a real
   `Faculty` entity, and the previously unused `Course.faculty` column was
   finally populated.
2. **Authorisation was tightened from role-based to ownership-based.** Earlier
   iterations allowed any `FACULTY` user to mark attendance for any course. With
   `Course.faculty` populated, the faculty screens are scoped to the courses the
   signed-in teacher actually owns, which is what the PRD means by "faculty
   academic responsibilities".
3. **`GET /api/auth/me` was added for session restoration.** Iterations 1–4
   assumed the client held login state in memory. Once a real single-page
   frontend existed, a browser refresh logged the user out. The endpoint lets the
   client rehydrate an existing session instead.
4. **Error reporting was unified in the UI.** Each earlier iteration reported
   failures differently. The frontend now renders every `error` string from the
   envelope into one alert region, so a validation failure from any module looks
   and behaves the same to the user.
5. **A single navigation shell replaced per-module screens.** This is the
   "improve navigation and usability" requirement made concrete: instead of
   separate pages per module, one role-aware sidebar exposes exactly the
   functions the signed-in user is entitled to, which is what turns four working
   modules into an ERP.

---

## Retrospective — what the Iterative model produced

- **Every iteration shipped a runnable system.** At the end of each increment the
  application started, its endpoints worked, and its tests passed.
- **Three requirements were genuinely refined mid-project**, not just
  implemented: the response envelope (iteration 2), bulk attendance entry
  (iteration 3), and the marks/results split (iteration 4). None of these were
  visible at the outset; each emerged from exercising the previous increment.
- **Integration risk was retired progressively.** Because iteration 2 already
  reserved `Course.faculty` and iteration 1 already separated `User` from
  `Student`, iteration 5 added faculty management without any schema rework.
- **The cost of change rose over time, as the model predicts.** Retrofitting the
  envelope in iteration 2 touched two controllers; doing the same in iteration 5
  would have touched eight. Iterating surfaced that decision while it was still
  cheap.
