package com.college.erp.service;

import com.college.erp.dto.AttendanceRequest;
import com.college.erp.dto.AttendanceResponse;
import com.college.erp.dto.AttendanceSummary;
import com.college.erp.dto.BulkAttendanceRequest;
import com.college.erp.entity.Attendance;
import com.college.erp.entity.AttendanceStatus;
import com.college.erp.entity.Course;
import com.college.erp.entity.Student;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.AttendanceRepository;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Iteration 3 - attendance marking and reporting (FR-05, US-03).
 *
 * Entities are converted to DTOs inside the transactional boundary because
 * {@code spring.jpa.open-in-view} is false, so no lazy association may be
 * dereferenced after the service returns.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Marks one student for one course on one day.
     *
     * @throws NotFoundException   when the student or the course does not exist
     * @throws BadRequestException when the date is in the future, or a record already
     *                             exists for that student, course and date
     */
    @Transactional
    public AttendanceResponse mark(AttendanceRequest request) {
        rejectFutureDate(request.classDate());
        Student student = requireStudent(request.studentId());
        Course course = requireCourse(request.courseId());

        if (attendanceRepository.existsByStudentIdAndCourseIdAndClassDate(
                student.getId(), course.getId(), request.classDate())) {
            throw new BadRequestException("Attendance for student " + student.getRollNumber()
                    + " in course " + course.getCode() + " on " + request.classDate()
                    + " is already recorded; use PUT /api/attendance/{id} to correct it");
        }

        Attendance saved = attendanceRepository.save(
                new Attendance(student, course, request.classDate(), request.status()));
        return AttendanceResponse.from(saved);
    }

    /**
     * Marks a whole class for one course on one day.
     *
     * <p>Duplicate handling: an entry whose student already has a record for that
     * course and date is <em>updated in place</em> rather than rejected. Roll call is
     * commonly re-submitted after a correction, so overwriting keeps the single record
     * per student/course/day guaranteed by the unique constraint and keeps the whole
     * submission atomic instead of failing the entire class for one repeated student.
     * The single-student endpoint keeps the stricter reject-on-duplicate rule.
     *
     * @throws NotFoundException   when the course or any listed student does not exist
     * @throws BadRequestException when the date is in the future or a student is listed twice
     */
    @Transactional
    public List<AttendanceResponse> markBulk(BulkAttendanceRequest request) {
        rejectFutureDate(request.classDate());
        Course course = requireCourse(request.courseId());

        List<AttendanceResponse> saved = new ArrayList<>();
        List<Long> seen = new ArrayList<>();
        for (BulkAttendanceRequest.Entry entry : request.entries()) {
            if (seen.contains(entry.studentId())) {
                throw new BadRequestException("Student " + entry.studentId()
                        + " is listed more than once in the same submission");
            }
            seen.add(entry.studentId());
            saved.add(upsert(requireStudent(entry.studentId()), course, request.classDate(), entry.status()));
        }
        return List.copyOf(saved);
    }

    /**
     * Corrects the status of an already recorded attendance entry.
     *
     * @throws NotFoundException when no record carries the given id
     */
    @Transactional
    public AttendanceResponse updateStatus(Long id, AttendanceStatus status) {
        Attendance attendance = attendanceRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Attendance record not found: id=" + id));
        attendance.setStatus(status);
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    /**
     * Returns the attendance of the signed-in student only, resolved from the
     * authenticated principal so one student can never read another's records.
     *
     * @throws NotFoundException when the principal has no student profile
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> findForUsername(String username) {
        Student student = requireStudentByUsername(username);
        return toResponses(attendanceRepository.findByStudentId(student.getId()));
    }

    /**
     * Returns the per-course attendance percentage of the signed-in student.
     * Courses with no classes held report 0 rather than dividing by zero.
     *
     * @throws NotFoundException when the principal has no student profile
     */
    @Transactional(readOnly = true)
    public List<AttendanceSummary> summaryForUsername(String username) {
        Student student = requireStudentByUsername(username);
        return summarise(attendanceRepository.findByStudentId(student.getId()));
    }

    /**
     * Returns every attendance record of one student. Staff-facing.
     *
     * @throws NotFoundException when the student does not exist
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByStudent(Long studentId) {
        Student student = requireStudent(studentId);
        return toResponses(attendanceRepository.findByStudentId(student.getId()));
    }

    /**
     * Returns the attendance sheet of one course, optionally for a single day.
     * Staff-facing.
     *
     * @throws NotFoundException when the course does not exist
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByCourse(Long courseId, LocalDate classDate) {
        Course course = requireCourse(courseId);
        LocalDate day = classDate == null ? LocalDate.now() : classDate;
        return toResponses(attendanceRepository.findByCourseIdAndClassDate(course.getId(), day));
    }

    /**
     * Returns the per-course summary of one student. Staff-facing.
     *
     * @throws NotFoundException when the student does not exist
     */
    @Transactional(readOnly = true)
    public List<AttendanceSummary> summaryForStudent(Long studentId) {
        Student student = requireStudent(studentId);
        return summarise(attendanceRepository.findByStudentId(student.getId()));
    }

    private AttendanceResponse upsert(Student student, Course course, LocalDate classDate,
                                      AttendanceStatus status) {
        Attendance record = attendanceRepository
                .findByStudentIdAndCourseIdAndClassDate(student.getId(), course.getId(), classDate)
                .orElseGet(() -> new Attendance(student, course, classDate, status));
        record.setStatus(status);
        return AttendanceResponse.from(attendanceRepository.save(record));
    }

    private List<AttendanceSummary> summarise(List<Attendance> records) {
        Map<Long, AttendanceSummary> byCourse = new LinkedHashMap<>();
        for (Attendance record : records) {
            Course course = record.getCourse();
            byCourse.computeIfAbsent(course.getId(), courseId -> AttendanceSummary.of(
                    courseId, course.getCode(), course.getTitle(),
                    attendanceRepository.countTotalByStudentAndCourse(record.getStudent().getId(), courseId),
                    attendanceRepository.countPresentByStudentAndCourse(record.getStudent().getId(), courseId)));
        }
        return List.copyOf(byCourse.values());
    }

    private List<AttendanceResponse> toResponses(List<Attendance> records) {
        return records.stream().map(AttendanceResponse::from).toList();
    }

    private void rejectFutureDate(LocalDate classDate) {
        if (classDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Attendance cannot be marked for a future date: " + classDate);
        }
    }

    private Student requireStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: id=" + studentId));
    }

    private Student requireStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("No student profile linked to " + username));
    }

    private Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: id=" + courseId));
    }
}
