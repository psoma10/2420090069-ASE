package com.college.erp.service;

import com.college.erp.dto.EnrollmentResponse;
import com.college.erp.entity.Course;
import com.college.erp.entity.Enrollment;
import com.college.erp.entity.Student;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.EnrollmentRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Iteration 2 - student course registration (FR-04, US-02).
 *
 * A student is always resolved from the authenticated username, never from a
 * client supplied identifier, so nobody can register or drop on behalf of
 * another student. Responses are mapped inside the transaction because
 * spring.jpa.open-in-view is false.
 */
@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Registers the authenticated student for a course.
     *
     * @param username authenticated principal name of the registering student
     * @param courseId course the student wants to register for
     * @return the created registration
     * @throws NotFoundException   when the principal has no student profile or the course is missing
     * @throws BadRequestException when the student is already registered for that course
     */
    @Transactional
    public EnrollmentResponse register(String username, Long courseId) {
        Student student = requireStudent(username);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found: id=" + courseId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new BadRequestException("Already registered for course: " + course.getCode());
        }
        return toResponse(enrollmentRepository.save(new Enrollment(student, course)));
    }

    /**
     * Lists the courses the authenticated student is registered for.
     *
     * @param username authenticated principal name of the student
     * @return that student's registrations, never null
     * @throws NotFoundException when the principal has no student profile
     */
    public List<EnrollmentResponse> findMyEnrollments(String username) {
        Student student = requireStudent(username);
        return enrollmentRepository.findByStudentId(student.getId()).stream()
                .map(EnrollmentService::toResponse)
                .toList();
    }

    /**
     * Lists the roster of a course, for faculty and administrators.
     *
     * @param courseId course identifier
     * @return registrations for that course, never null
     * @throws NotFoundException when the course does not exist
     */
    public List<EnrollmentResponse> findCourseRoster(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new NotFoundException("Course not found: id=" + courseId);
        }
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentService::toResponse)
                .toList();
    }

    /**
     * Counts how many students are registered for a course.
     *
     * @param courseId course identifier
     * @return number of registrations
     */
    public long countForCourse(Long courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    /**
     * Drops a registration. An administrator may drop any registration; a student
     * may only drop their own.
     *
     * @param enrollmentId registration identifier
     * @param username     authenticated principal name
     * @param admin        true when the caller holds the ADMIN role
     * @throws NotFoundException     when the registration does not exist
     * @throws AccessDeniedException when a student tries to drop somebody else's registration
     */
    @Transactional
    public void drop(Long enrollmentId, String username, boolean admin) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: id=" + enrollmentId));

        if (!admin && !isOwnedBy(enrollment, username)) {
            throw new AccessDeniedException("You may only drop your own registrations");
        }
        enrollmentRepository.delete(enrollment);
    }

    private boolean isOwnedBy(Enrollment enrollment, String username) {
        return studentRepository.findByUserUsername(username)
                .map(student -> student.getId().equals(enrollment.getStudent().getId()))
                .orElse(false);
    }

    private Student requireStudent(String username) {
        if (username == null || username.isBlank()) {
            throw new NotFoundException("No authenticated student");
        }
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("No student profile for user: " + username));
    }

    /** Flattens a registration and its lazy associations into a detached record. */
    private static EnrollmentResponse toResponse(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        return new EnrollmentResponse(
                enrollment.getId(),
                student.getId(),
                student.getRollNumber(),
                student.getName(),
                CourseService.toResponse(enrollment.getCourse()),
                enrollment.getRegisteredOn().toString());
    }
}
