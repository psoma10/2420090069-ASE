package com.college.erp;

import com.college.erp.dto.MarksRequest;
import com.college.erp.dto.MarksResponse;
import com.college.erp.dto.ResultResponse;
import com.college.erp.dto.ResultSummary;
import com.college.erp.entity.Course;
import com.college.erp.entity.Result;
import com.college.erp.entity.Role;
import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import com.college.erp.service.MarksService;
import com.college.erp.service.ResultService;
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
 * Iteration 4 - result publication and grading (FR-07, US-05).
 *
 * Every fixture is created with a random roll number / course code because the
 * in-memory H2 database is shared with the other iterations' test classes.
 */
@SpringBootTest
@Transactional
class ResultServiceTest {

    @Autowired
    private ResultService resultService;

    @Autowired
    private MarksService marksService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("publishing a course publishes one result per student who has marks")
    void publishCourse_publishesEveryStudentWithMarks() {
        Student first = newStudent();
        Student second = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(first.getId(), course.getId(), 30, 40));
        marksService.enter(new MarksRequest(second.getId(), course.getId(), 20, 25));

        int published = resultService.publishCourse(course.getId());

        assertThat(published).isEqualTo(2);
        assertThat(resultService.findByCourse(course.getId())).hasSize(2);
    }

    @Test
    @DisplayName("a student without marks is not published for, and a bare course is rejected")
    void publishCourse_noMarksRecorded_isRejected() {
        Course course = newCourse();

        assertThatThrownBy(() -> resultService.publishCourse(course.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No marks recorded");
    }

    @Test
    @DisplayName("publishing one student without marks is rejected")
    void publishStudent_noMarks_isRejected() {
        Student student = newStudent();
        Course course = newCourse();

        assertThatThrownBy(() -> resultService.publishStudent(student.getId(), course.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No marks recorded");
    }

    @Test
    @DisplayName("publishing an unknown course is reported as not found")
    void publishCourse_unknownCourse_isNotFound() {
        assertThatThrownBy(() -> resultService.publishCourse(999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("a total of 85 is published as grade A and a pass")
    void publishStudent_total85_isGradeA() {
        Student student = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(student.getId(), course.getId(), 35, 50));

        ResultResponse result = resultService.publishStudent(student.getId(), course.getId());

        assertThat(result.totalMarks()).isEqualTo(85);
        assertThat(result.grade()).isEqualTo(Result.gradeFor(85));
        assertThat(result.grade()).isEqualTo("A");
        assertThat(result.passed()).isTrue();
        assertThat(result.rollNumber()).isEqualTo(student.getRollNumber());
        assertThat(result.courseCode()).isEqualTo(course.getCode());
        assertThat(result.publishedOn()).isNotNull();
    }

    @Test
    @DisplayName("a total below the pass mark is published as grade F and a fail")
    void publishStudent_belowPassMark_isGradeF() {
        Student student = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(student.getId(), course.getId(), 10, 15));

        ResultResponse result = resultService.publishStudent(student.getId(), course.getId());

        assertThat(result.totalMarks()).isEqualTo(25);
        assertThat(result.grade()).isEqualTo("F");
        assertThat(result.passed()).isFalse();
    }

    @Test
    @DisplayName("re-publishing after a marks correction updates the same result row")
    void publish_afterMarksCorrection_reappliesNewTotal() {
        Student student = newStudent();
        Course course = newCourse();
        MarksResponse marks = marksService.enter(
                new MarksRequest(student.getId(), course.getId(), 10, 15));
        ResultResponse first = resultService.publishStudent(student.getId(), course.getId());
        assertThat(first.grade()).isEqualTo("F");

        marksService.update(marks.id(), new MarksRequest(student.getId(), course.getId(), 38, 57));
        ResultResponse republished = resultService.publishStudent(student.getId(), course.getId());

        assertThat(republished.id()).isEqualTo(first.id());
        assertThat(republished.totalMarks()).isEqualTo(95);
        assertThat(republished.grade()).isEqualTo("A+");
        assertThat(republished.passed()).isTrue();
        assertThat(resultService.findByStudent(student.getId())).hasSize(1);
    }

    @Test
    @DisplayName("re-publishing a whole course refreshes rather than duplicates results")
    void publishCourse_twice_doesNotDuplicateResults() {
        Student student = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(student.getId(), course.getId(), 30, 40));
        resultService.publishCourse(course.getId());

        int republished = resultService.publishCourse(course.getId());

        assertThat(republished).isEqualTo(1);
        assertThat(resultService.findByCourse(course.getId())).hasSize(1);
    }

    @Test
    @DisplayName("the me lookup returns only the signed-in student's results")
    void findForUsername_returnsOwnResultsOnly() {
        Student mine = newStudent();
        Student other = newStudent();
        Course course = newCourse();
        marksService.enter(new MarksRequest(mine.getId(), course.getId(), 30, 40));
        marksService.enter(new MarksRequest(other.getId(), course.getId(), 10, 20));
        resultService.publishCourse(course.getId());

        List<ResultResponse> results = resultService.findForUsername(mine.getUser().getUsername());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).rollNumber()).isEqualTo(mine.getRollNumber());
    }

    @Test
    @DisplayName("the summary counts courses, passes and the aggregate percentage")
    void summaryForUsername_aggregatesPublishedResults() {
        Student student = newStudent();
        Course passed = newCourse();
        Course failed = newCourse();
        marksService.enter(new MarksRequest(student.getId(), passed.getId(), 35, 45));
        marksService.enter(new MarksRequest(student.getId(), failed.getId(), 10, 20));
        resultService.publishCourse(passed.getId());
        resultService.publishCourse(failed.getId());

        ResultSummary summary = resultService.summaryForUsername(student.getUser().getUsername());

        assertThat(summary.totalCourses()).isEqualTo(2);
        assertThat(summary.coursesPassed()).isEqualTo(1);
        assertThat(summary.totalMarks()).isEqualTo(110);
        assertThat(summary.percentage()).isEqualTo(55.00);
    }

    @Test
    @DisplayName("a student with no published results gets zeroes, not a divide-by-zero")
    void summaryForUsername_noResults_returnsZeroes() {
        Student student = newStudent();

        ResultSummary summary = resultService.summaryForUsername(student.getUser().getUsername());

        assertThat(summary.totalCourses()).isZero();
        assertThat(summary.coursesPassed()).isZero();
        assertThat(summary.totalMarks()).isZero();
        assertThat(summary.percentage()).isZero();
    }

    @Test
    @DisplayName("an empty summary rounds to two decimals without dividing by zero")
    void summaryOf_emptyCollection_returnsZeroPercent() {
        ResultSummary summary = ResultSummary.of(List.<ResultResponse>of());

        assertThat(summary.percentage()).isZero();
    }

    @Test
    @DisplayName("a principal without a student profile is reported as not found")
    void findForUsername_noProfile_isNotFound() {
        assertThatThrownBy(() -> resultService.findForUsername("nobody-" + UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No student profile");
    }

    private Student newStudent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(new User(
                "rstu-" + suffix, "{noop}secret", "Student " + suffix,
                "rstu-" + suffix + "@college.test", Role.STUDENT));
        return studentRepository.save(new Student(
                "RR" + suffix, "Student " + suffix, "CSE", 3, user));
    }

    private Course newCourse() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return courseRepository.save(new Course(
                "RC-" + suffix, "Course " + suffix, 4, 3, "CSE"));
    }
}
