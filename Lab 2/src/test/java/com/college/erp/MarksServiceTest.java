package com.college.erp;

import com.college.erp.dto.MarksRequest;
import com.college.erp.dto.MarksResponse;
import com.college.erp.entity.Course;
import com.college.erp.entity.Role;
import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.MarksRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import com.college.erp.service.MarksService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Iteration 4 - marks entry rules (FR-06, US-04).
 *
 * Every fixture is created with a random roll number / course code because the
 * in-memory H2 database is shared with the other iterations' test classes.
 */
@SpringBootTest
@Transactional
class MarksServiceTest {

    @Autowired
    private MarksService marksService;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("entering marks stores the components and the computed total")
    void enter_validRequest_persistsMarks() {
        Student student = newStudent();
        Course course = newCourse();

        MarksResponse response = marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 35, 50));

        assertThat(response.id()).isNotNull();
        assertThat(response.rollNumber()).isEqualTo(student.getRollNumber());
        assertThat(response.courseCode()).isEqualTo(course.getCode());
        assertThat(response.courseTitle()).isEqualTo(course.getTitle());
        assertThat(response.internalMarks()).isEqualTo(35);
        assertThat(response.externalMarks()).isEqualTo(50);
        assertThat(response.total()).isEqualTo(85);
        assertThat(marksRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())).isTrue();
    }

    @Test
    @DisplayName("internal marks above 40 are rejected")
    void enter_internalAboveMaximum_isRejected() {
        Student student = newStudent();
        Course course = newCourse();

        assertThatThrownBy(() -> marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 41, 50)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Internal marks must be between 0 and 40");
    }

    @Test
    @DisplayName("external marks above 60 are rejected")
    void enter_externalAboveMaximum_isRejected() {
        Student student = newStudent();
        Course course = newCourse();

        assertThatThrownBy(() -> marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 30, 61)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("External marks must be between 0 and 60");
    }

    @Test
    @DisplayName("negative marks are rejected")
    void enter_negativeMarks_isRejected() {
        Student student = newStudent();
        Course course = newCourse();

        assertThatThrownBy(() -> marksService.enter(
                new MarksRequest(student.getId(), course.getId(), -1, 50)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Internal marks must be between 0 and 40");
    }

    @Test
    @DisplayName("the boundary values 40 and 60 are accepted and total 100")
    void enter_boundaryValues_areAccepted() {
        Student student = newStudent();
        Course course = newCourse();

        MarksResponse response = marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 40, 60));

        assertThat(response.total()).isEqualTo(100);
    }

    @Test
    @DisplayName("entering marks twice for the same student and course points the caller at PUT")
    void enter_duplicate_isRejected() {
        Student student = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(student.getId(), course.getId(), 30, 40));

        assertThatThrownBy(() -> marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 32, 45)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exist");
    }

    @Test
    @DisplayName("marks for an unknown student are rejected as not found")
    void enter_unknownStudent_isNotFound() {
        Course course = newCourse();

        assertThatThrownBy(() -> marksService.enter(
                new MarksRequest(999_999L, course.getId(), 30, 40)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updating corrects the recorded marks and the total")
    void update_correctsRecordedMarks() {
        Student student = newStudent();
        Course course = newCourse();
        MarksResponse created = marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 20, 30));

        MarksResponse updated = marksService.update(created.id(),
                new MarksRequest(student.getId(), course.getId(), 38, 55));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.internalMarks()).isEqualTo(38);
        assertThat(updated.externalMarks()).isEqualTo(55);
        assertThat(updated.total()).isEqualTo(93);
    }

    @Test
    @DisplayName("an out-of-range correction is rejected too")
    void update_outOfRange_isRejected() {
        Student student = newStudent();
        Course course = newCourse();
        MarksResponse created = marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 20, 30));

        assertThatThrownBy(() -> marksService.update(created.id(),
                new MarksRequest(student.getId(), course.getId(), 20, 99)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("External marks");
    }

    @Test
    @DisplayName("the me lookup returns only the signed-in student's marks")
    void findForUsername_returnsOwnMarksOnly() {
        Student mine = newStudent();
        Student other = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(mine.getId(), course.getId(), 30, 40));
        marksService.enter(new MarksRequest(other.getId(), course.getId(), 10, 20));

        List<MarksResponse> records = marksService.findForUsername(mine.getUser().getUsername());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).rollNumber()).isEqualTo(mine.getRollNumber());
        assertThat(records.get(0).total()).isEqualTo(70);
    }

    @Test
    @DisplayName("a principal without a student profile is reported as not found")
    void findForUsername_noProfile_isNotFound() {
        assertThatThrownBy(() -> marksService.findForUsername("nobody-" + UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No student profile");
    }

    @Test
    @DisplayName("course listing returns the marks of every student in that course")
    void findByCourse_returnsAllStudentsOfCourse() {
        Student first = newStudent();
        Student second = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(first.getId(), course.getId(), 30, 40));
        marksService.enter(new MarksRequest(second.getId(), course.getId(), 25, 35));

        List<MarksResponse> records = marksService.findByCourse(course.getId());

        assertThat(records).hasSize(2);
        assertThat(records).extracting(MarksResponse::total).containsExactlyInAnyOrder(70, 60);
    }

    private Student newStudent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(new User(
                "mstu-" + suffix, "{noop}secret", "Student " + suffix,
                "mstu-" + suffix + "@college.test", Role.STUDENT));
        return studentRepository.save(new Student(
                "MR" + suffix, "Student " + suffix, "CSE", 3, user));
    }

    private Course newCourse() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return courseRepository.save(new Course(
                "MC-" + suffix, "Course " + suffix, 4, 3, "CSE"));
    }
}
