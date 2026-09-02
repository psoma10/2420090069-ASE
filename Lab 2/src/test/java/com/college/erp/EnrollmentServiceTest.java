package com.college.erp;

import com.college.erp.dto.CourseRequest;
import com.college.erp.dto.CourseResponse;
import com.college.erp.dto.EnrollmentResponse;
import com.college.erp.entity.Role;
import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import com.college.erp.service.CourseService;
import com.college.erp.service.EnrollmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Iteration 2 - course registration tests (FR-04, US-02).
 *
 * Users, students and courses are created per test with random identifiers so
 * the suite never depends on seed data and never collides with rows written by
 * other iterations sharing the same in-memory H2 database.
 */
@SpringBootTest
@Transactional
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Student newStudent() {
        String suffix = unique();
        User user = userRepository.save(new User(
                "stu" + suffix,
                "{noop}not-used-in-service-tests",
                "Student " + suffix,
                "stu" + suffix + "@college.test",
                Role.STUDENT));
        return studentRepository.save(new Student("R" + suffix, "Student " + suffix, "CSE", 3, user));
    }

    private CourseResponse newCourse() {
        String suffix = unique();
        return courseService.create(new CourseRequest(
                "CS" + suffix, "Course " + suffix, 4, 3, "DEPT-" + suffix));
    }

    @Test
    @DisplayName("register enrolls the authenticated student in a course")
    void register_validStudentAndCourse_createsEnrollment() {
        Student student = newStudent();
        CourseResponse course = newCourse();
        String username = student.getUser().getUsername();

        EnrollmentResponse enrollment = enrollmentService.register(username, course.id());

        assertThat(enrollment.id()).isNotNull();
        assertThat(enrollment.studentId()).isEqualTo(student.getId());
        assertThat(enrollment.rollNumber()).isEqualTo(student.getRollNumber());
        assertThat(enrollment.course().id()).isEqualTo(course.id());
        assertThat(enrollment.registeredOn()).isNotBlank();

        List<EnrollmentResponse> mine = enrollmentService.findMyEnrollments(username);
        assertThat(mine).extracting(EnrollmentResponse::id).containsExactly(enrollment.id());
        assertThat(enrollmentService.countForCourse(course.id())).isEqualTo(1L);
    }

    @Test
    @DisplayName("register rejects registering twice for the same course")
    void register_duplicateRegistration_throwsBadRequest() {
        Student student = newStudent();
        CourseResponse course = newCourse();
        String username = student.getUser().getUsername();
        enrollmentService.register(username, course.id());

        assertThatThrownBy(() -> enrollmentService.register(username, course.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(course.code());

        assertThat(enrollmentService.countForCourse(course.id())).isEqualTo(1L);
    }

    @Test
    @DisplayName("register rejects a principal without a student profile")
    void register_unknownPrincipal_throwsNotFound() {
        CourseResponse course = newCourse();

        assertThatThrownBy(() -> enrollmentService.register("ghost-" + unique(), course.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("course roster lists the registered students")
    void findCourseRoster_returnsRegisteredStudents() {
        Student first = newStudent();
        Student second = newStudent();
        CourseResponse course = newCourse();
        enrollmentService.register(first.getUser().getUsername(), course.id());
        enrollmentService.register(second.getUser().getUsername(), course.id());

        List<EnrollmentResponse> roster = enrollmentService.findCourseRoster(course.id());

        assertThat(roster).extracting(EnrollmentResponse::studentId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    @DisplayName("a student may not drop another student's registration")
    void drop_otherStudentsEnrollment_isDenied() {
        Student owner = newStudent();
        Student intruder = newStudent();
        CourseResponse course = newCourse();
        EnrollmentResponse enrollment = enrollmentService.register(owner.getUser().getUsername(), course.id());

        String intruderName = intruder.getUser().getUsername();
        assertThatThrownBy(() -> enrollmentService.drop(enrollment.id(), intruderName, false))
                .isInstanceOf(AccessDeniedException.class);

        enrollmentService.drop(enrollment.id(), owner.getUser().getUsername(), false);
        assertThat(enrollmentService.findMyEnrollments(owner.getUser().getUsername())).isEmpty();
    }
}
