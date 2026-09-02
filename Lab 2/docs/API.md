# College ERP System — REST API Reference

Base URL: `http://localhost:8080`

## Conventions

**Response envelope.** Every endpoint returns the same JSON envelope, defined by
`com.college.erp.dto.ApiResponse`:

```json
{ "success": true,  "data": { }, "error": null }
{ "success": false, "data": null, "error": "Roll number CSE21001 is already registered" }
```

Clients check `success` first: on `true` the payload is in `data`, on `false` a
human-readable message is in `error`. Validation failures, not-found errors and
business-rule violations are all normalised into this shape by
`GlobalExceptionHandler`.

**Authentication.** Login creates a server-side HTTP session and returns a
`JSESSIONID` cookie. Every subsequent request must carry that cookie — the
browser client sends `credentials: 'same-origin'` on each `fetch`. There is no
token header. Requests without a valid session receive **HTTP 401** with an empty
body (`HttpStatusEntryPoint`), which the frontend treats as "return to login".

**Authorisation.** Roles are `STUDENT`, `FACULTY` and `ADMIN`, enforced with
Spring Security method security. A request from a signed-in user whose role is
not permitted receives **HTTP 403**.

**Content type.** All request and response bodies are `application/json`. Dates
use ISO-8601 (`yyyy-MM-dd`); timestamps use `yyyy-MM-ddTHH:mm:ss`.

---

## 1. Authentication — FR-01, US-01

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| POST | `/api/auth/login` | Public | `{ "username": "student1", "password": "student123" }` | `{ "username", "fullName", "role" }` |
| POST | `/api/auth/logout` | Public (any session) | *(none)* | `null` — session invalidated |
| GET | `/api/auth/me` | Public | *(none)* | `{ "username", "fullName", "role" }`, or `null` when no session |

`GET /api/auth/me` lets the single-page frontend restore an existing session
after a browser refresh instead of forcing a fresh login.

---

## 2. Students — FR-02

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| GET | `/api/students` | ADMIN, FACULTY | *(none)* — optional `?department=CSE` | `StudentResponse[]` |
| GET | `/api/students/me` | STUDENT | *(none)* | `StudentResponse` for the signed-in student |
| GET | `/api/students/{id}` | ADMIN, FACULTY | *(none)* | `StudentResponse` |
| GET | `/api/students/roll/{rollNumber}` | ADMIN, FACULTY | *(none)* | `StudentResponse` |
| POST | `/api/students` | ADMIN | `StudentRequest` | created `StudentResponse` |
| PUT | `/api/students/{id}` | ADMIN | `StudentRequest` | updated `StudentResponse` |
| DELETE | `/api/students/{id}` | ADMIN | *(none)* | confirmation message (`String`) |

**`StudentRequest`**

```json
{
  "rollNumber": "CSE21001",
  "name": "Anita Rao",
  "department": "CSE",
  "semester": 3,
  "phone": "9876543210",
  "username": "student1",
  "password": "student123",
  "email": "anita.rao@college.edu"
}
```

Validation: `rollNumber`, `name`, `department`, `username`, `password`, `email`
are required; `semester` is 1–8; `password` is at least 6 characters; `email`
must be a valid address; `phone` is optional.

**`StudentResponse`**

```json
{
  "id": 1, "rollNumber": "CSE21001", "name": "Anita Rao",
  "department": "CSE", "semester": 3, "phone": "9876543210",
  "username": "student1", "email": "anita.rao@college.edu"
}
```

The password is never returned.

---

## 3. Courses — FR-03

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| GET | `/api/courses` | Any authenticated | *(none)* — optional `?department=CSE&semester=3` | `CourseResponse[]` |
| GET | `/api/courses/{id}` | Any authenticated | *(none)* | `CourseResponse` |
| POST | `/api/courses` | ADMIN | `CourseRequest` | created `CourseResponse` |
| PUT | `/api/courses/{id}` | ADMIN | `CourseRequest` | updated `CourseResponse` |
| DELETE | `/api/courses/{id}` | ADMIN | *(none)* | `null` |
| PUT | `/api/courses/{id}/faculty/{facultyId}` | ADMIN | *(none)* | `CourseResponse` with the faculty assigned |

**`CourseRequest`**

```json
{ "code": "CS301", "title": "Database Systems", "credits": 4, "semester": 3, "department": "CSE" }
```

Validation: `code` (≤ 20 chars) and `title` (≤ 120 chars) required; `credits`
positive; `semester` 1–12; `department` required.

**`CourseResponse`**

```json
{
  "id": 1, "code": "CS301", "title": "Database Systems",
  "credits": 4, "semester": 3, "department": "CSE",
  "facultyId": 2, "facultyName": "Dr. R. Menon"
}
```

`facultyId` and `facultyName` are `null` until a teacher is assigned. The
association is flattened in the service layer because `spring.jpa.open-in-view`
is `false`.

---

## 4. Enrollments — FR-04, US-02

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| POST | `/api/enrollments/register` | STUDENT | `{ "courseId": 1 }` | created `EnrollmentResponse` |
| GET | `/api/enrollments/me` | STUDENT | *(none)* | `EnrollmentResponse[]` for the signed-in student |
| GET | `/api/enrollments/course/{courseId}` | FACULTY, ADMIN | *(none)* | `EnrollmentResponse[]` — the course roster |
| DELETE | `/api/enrollments/{id}` | STUDENT (own), ADMIN | *(none)* | `null` |

**`EnrollmentResponse`**

```json
{
  "id": 10, "studentId": 1, "rollNumber": "CSE21001", "studentName": "Anita Rao",
  "course": { "id": 1, "code": "CS301", "title": "Database Systems",
              "credits": 4, "semester": 3, "department": "CSE",
              "facultyId": 2, "facultyName": "Dr. R. Menon" },
  "registeredOn": "2025-08-14"
}
```

Registering twice for the same course fails with
`{ "success": false, "error": "…already registered…" }`, backed by the database
unique constraint `uk_enrollment_student_course`.

---

## 5. Attendance — FR-05, US-03

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| POST | `/api/attendance` | FACULTY, ADMIN | `AttendanceRequest` | created `AttendanceResponse` |
| POST | `/api/attendance/bulk` | FACULTY, ADMIN | `BulkAttendanceRequest` | `AttendanceResponse[]` |
| PUT | `/api/attendance/{id}` | FACULTY, ADMIN | `AttendanceRequest` (only `status` is applied) | updated `AttendanceResponse` |
| GET | `/api/attendance/me` | STUDENT | *(none)* | `AttendanceResponse[]` |
| GET | `/api/attendance/me/summary` | STUDENT | *(none)* | `AttendanceSummary[]` |
| GET | `/api/attendance/student/{studentId}` | FACULTY, ADMIN | *(none)* | `AttendanceResponse[]` |
| GET | `/api/attendance/student/{studentId}/summary` | FACULTY, ADMIN | *(none)* | `AttendanceSummary[]` |
| GET | `/api/attendance/course/{courseId}` | FACULTY, ADMIN | *(none)* — optional `?date=2025-08-20` | `AttendanceResponse[]` |

**`AttendanceRequest`**

```json
{ "studentId": 1, "courseId": 1, "classDate": "2025-08-20", "status": "PRESENT" }
```

`status` is `PRESENT` or `ABSENT`. All four fields are required.

**`BulkAttendanceRequest`** — marks a whole class in one call:

```json
{
  "courseId": 1,
  "classDate": "2025-08-20",
  "entries": [
    { "studentId": 1, "status": "PRESENT" },
    { "studentId": 2, "status": "ABSENT" }
  ]
}
```

`entries` must contain at least one entry.

**`AttendanceResponse`**

```json
{
  "id": 55, "studentId": 1, "rollNumber": "CSE21001", "studentName": "Anita Rao",
  "courseId": 1, "courseCode": "CS301", "courseTitle": "Database Systems",
  "classDate": "2025-08-20", "status": "PRESENT"
}
```

**`AttendanceSummary`** — one row per course the student is registered for:

```json
{
  "courseId": 1, "courseCode": "CS301", "courseTitle": "Database Systems",
  "totalClasses": 20, "presentCount": 17, "percentage": 85.0
}
```

`percentage` is rounded to two decimals and reported as `0` when no classes have
been held (divide-by-zero guard).

---

## 6. Marks — FR-06, US-04

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| POST | `/api/marks` | FACULTY, ADMIN | `MarksRequest` | created/updated `MarksResponse` |
| PUT | `/api/marks/{id}` | FACULTY, ADMIN | `MarksRequest` | updated `MarksResponse` |
| GET | `/api/marks/me` | STUDENT | *(none)* | `MarksResponse[]` |
| GET | `/api/marks/{id}` | FACULTY, ADMIN | *(none)* | `MarksResponse` |
| GET | `/api/marks/student/{studentId}` | FACULTY, ADMIN | *(none)* | `MarksResponse[]` |
| GET | `/api/marks/course/{courseId}` | FACULTY, ADMIN | *(none)* | `MarksResponse[]` — the course marks sheet |

**`MarksRequest`**

```json
{ "studentId": 1, "courseId": 1, "internalMarks": 34, "externalMarks": 48 }
```

Validation: `internalMarks` 0–40, `externalMarks` 0–60. A student may hold only
one marks row per course (`uk_marks_student_course`), so posting again for the
same pair updates the existing entry rather than duplicating it.

**`MarksResponse`**

```json
{
  "id": 7, "studentId": 1, "rollNumber": "CSE21001",
  "courseId": 1, "courseCode": "CS301", "courseTitle": "Database Systems",
  "internalMarks": 34, "externalMarks": 48, "total": 82
}
```

`total` is `internalMarks + externalMarks`, out of 100.

---

## 7. Results — FR-07, US-05

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| POST | `/api/results/publish/{courseId}` | FACULTY, ADMIN | *(none)* | confirmation message, e.g. `"Published 24 result(s)"` |
| POST | `/api/results/publish/student/{studentId}/course/{courseId}` | FACULTY, ADMIN | *(none)* | `ResultResponse` for that one student |
| GET | `/api/results/me` | STUDENT | *(none)* | `MyResults` — `{ results, summary }` |
| GET | `/api/results/student/{studentId}` | FACULTY, ADMIN | *(none)* | `ResultResponse[]` |
| GET | `/api/results/student/{studentId}/summary` | FACULTY, ADMIN | *(none)* | `ResultSummary` |
| GET | `/api/results/course/{courseId}` | FACULTY, ADMIN | *(none)* | `ResultResponse[]` — every published result for the course |

Publishing a whole course returns a **count message rather than the rows**, so a
client that needs the published list reads it back from
`GET /api/results/course/{courseId}`.

**`ResultResponse`**

```json
{
  "id": 3, "studentId": 1, "rollNumber": "CSE21001",
  "courseId": 1, "courseCode": "CS301", "courseTitle": "Database Systems",
  "totalMarks": 82, "grade": "A", "passed": true,
  "publishedOn": "2025-09-01T10:15:30"
}
```

**`MyResults`** — what a student receives from `/api/results/me`: the full list
plus a pre-computed aggregate, so the portal does not recalculate it client-side.

```json
{
  "results": [ /* ResultResponse[] */ ],
  "summary": { "totalCourses": 5, "coursesPassed": 5, "totalMarks": 401, "percentage": 80.2 }
}
```

**`ResultSummary`** — `percentage` is the mean total across published courses,
rounded to two decimals, and is `0` when the student has no published results
(divide-by-zero guard).

Publishing reads every marks row for the course and derives a result per
student. Re-publishing after a marks correction refreshes the existing result
row (`uk_result_student_course`) instead of creating a duplicate.

**Grading scale** (`Result.gradeFor`), pass mark 40:

| Total | 90+ | 80–89 | 70–79 | 60–69 | 50–59 | 40–49 | < 40 |
|---|---|---|---|---|---|---|---|
| Grade | A+ | A | B | C | D | E | F |
| Passed | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✘ |

---

## 8. Faculty — FR-08

| Method | Path | Role | Request body | Response `data` |
|---|---|---|---|---|
| GET | `/api/faculty` | ADMIN | *(none)* — optional `?department=CSE` | `FacultyResponse[]` |
| GET | `/api/faculty/me` | FACULTY | *(none)* | `FacultyResponse` for the signed-in faculty member |
| GET | `/api/faculty/{id}` | ADMIN | *(none)* | `FacultyResponse` |
| POST | `/api/faculty` | ADMIN | `FacultyRequest` | created `FacultyResponse` |
| PUT | `/api/faculty/{id}` | ADMIN | `FacultyRequest` | updated `FacultyResponse` |
| DELETE | `/api/faculty/{id}` | ADMIN | *(none)* | confirmation message (`String`) |

**`FacultyRequest`**

```json
{
  "employeeCode": "FAC101",
  "name": "Dr. R. Menon",
  "department": "CSE",
  "designation": "Associate Professor",
  "username": "faculty1",
  "password": "faculty123",
  "email": "r.menon@college.edu"
}
```

Validation: `employeeCode`, `name`, `department`, `username`, `password` (≥ 6
characters) and a valid `email` are required; `designation` is optional.

**`FacultyResponse`**

```json
{
  "id": 2, "employeeCode": "FAC101", "name": "Dr. R. Menon",
  "department": "CSE", "designation": "Associate Professor",
  "username": "faculty1", "email": "r.menon@college.edu"
}
```

The password is never returned.

---

## HTTP status codes

| Status | Meaning | Envelope |
|---|---|---|
| 200 | Success | `success: true` |
| 201 | Resource created | `success: true` |
| 400 | Validation failure or business-rule violation | `success: false`, `error` describes the problem |
| 401 | No valid session — the client must sign in | empty body |
| 403 | Signed in, but the role is not permitted | `success: false` |
| 404 | Resource not found | `success: false` |
| 409 | Unique-constraint conflict (duplicate roll number, code, registration) | `success: false` |
