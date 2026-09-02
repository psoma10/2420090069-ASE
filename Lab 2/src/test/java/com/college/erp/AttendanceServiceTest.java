package com.college.erp;

import com.college.erp.dto.AttendanceRequest;
import com.college.erp.dto.AttendanceResponse;
import com.college.erp.dto.AttendanceSummary;
import com.college.erp.dto.BulkAttendanceRequest;
import com.college.erp.entity.AttendanceStatus;
import com.college.erp.entity.Course;
import com.college.erp.entity.Role;
import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import com.college.erp.exception.BadRequestException;
import com.college.erp.repository.AttendanceRepository;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import com.college.erp.service.AttendanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Iteration 3 - attendance marking and percentage rules (FR-05, US-03).
 *
 * Every fixture is created with a random roll number / course code because the
 * in-memory H2 database is shared with the other iterations' test classes.
 */
@SpringBootTest
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("marking attendance stores a PRESENT record for the student and course")
    void mark_validRequest_persistsRecord() {
        Student student = newStudent();
        Course course = newCourse();
        LocalDate today = LocalDate.now();

        AttendanceResponse response = attendanceService.mark(
                new AttendanceRequest(student.getId(), course.getId(), today, AttendanceStatus.PRESENT));

        assertThat(response.id()).isNotNull();
        assertThat(response.rollNumber()).isEqualTo(student.getRollNumber());
        assertThat(response.courseCode()).isEqualTo(course.getCode());
        assertThat(response.status()).isEqualTo("PRESENT");
        assertThat(attendanceRepository.existsByStudentIdAndCourseIdAndClassDate(
                student.getId(), course.getId(), today)).isTrue();
    }

    @Test
    @DisplayName("marking the same student, course and date twice is rejected")
    void mark_duplicate_isRejected() {
        Student student = newStudent();
        Course course = newCourse();
        LocalDate today = LocalDate.now();
        attendanceService.mark(
                new AttendanceRequest(student.getId(), course.getId(), today, AttendanceStatus.PRESENT));

        assertThatThrownBy(() -> attendanceService.mark(
                new AttendanceRequest(student.getId(), course.getId(), today, AttendanceStatus.ABSENT)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already recorded");
    }

    @Test
    @DisplayName("a future class date is rejected")
    void mark_futureDate_isRejected() {
        Student student = newStudent();
        Course course = newCourse();

        assertThatThrownBy(() -> attendanceService.mark(new AttendanceRequest(
                student.getId(), course.getId(), LocalDate.now().plusDays(1), AttendanceStatus.PRESENT)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future date");
    }

    @Test
    @DisplayName("summary reports the present percentage rounded to two decimals")
    void summary_threeOfFourPresent_reports75Percent() {
        Student student = newStudent();
        Course course = newCourse();
        markDays(student, course, AttendanceStatus.PRESENT, 1, 2, 3);
        markDays(student, course, AttendanceStatus.ABSENT, 4);

        List<AttendanceSummary> summaries = attendanceService.summaryForStudent(student.getId());

        assertThat(summaries).hasSize(1);
        AttendanceSummary summary = summaries.get(0);
        assertThat(summary.courseCode()).isEqualTo(course.getCode());
        assertThat(summary.courseTitle()).isEqualTo(course.getTitle());
        assertThat(summary.totalClasses()).isEqualTo(4);
        assertThat(summary.presentCount()).isEqualTo(3);
        assertThat(summary.percentage()).isEqualTo(75.00);
    }

    @Test
    @DisplayName("a recurring third rounds to two decimal places")
    void summary_twoOfThreePresent_roundsToTwoDecimals() {
        Student student = newStudent();
        Course course = newCourse();
        markDays(student, course, AttendanceStatus.PRESENT, 1, 2);
        markDays(student, course, AttendanceStatus.ABSENT, 3);

        AttendanceSummary summary = attendanceService.summaryForStudent(student.getId()).get(0);

        assertThat(summary.percentage()).isEqualTo(66.67);
    }

    @Test
    @DisplayName("a student with no classes held gets an empty summary, not a divide-by-zero")
    void summary_noClassesHeld_returnsEmptyList() {
        Student student = newStudent();

        List<AttendanceSummary> summaries = attendanceService.summaryForStudent(student.getId());

        assertThat(summaries).isEmpty();
    }

    @Test
    @DisplayName("a course with zero classes reports 0 percent instead of dividing by zero")
    void summaryOf_zeroClasses_returnsZeroPercent() {
        AttendanceSummary summary = AttendanceSummary.of(1L, "CS000", "Empty Course", 0, 0);

        assertThat(summary.percentage()).isZero();
        assertThat(summary.totalClasses()).isZero();
    }

    @Test
    @DisplayName("bulk marking records every listed student and overwrites repeats")
    void markBulk_marksClassAndOverwritesExisting() {
        Student first = newStudent();
        Student second = newStudent();
        Course course = newCourse();
        LocalDate today = LocalDate.now();

        List<AttendanceResponse> created = attendanceService.markBulk(new BulkAttendanceRequest(
                course.getId(), today,
                List.of(new BulkAttendanceRequest.Entry(first.getId(), AttendanceStatus.PRESENT),
                        new BulkAttendanceRequest.Entry(second.getId(), AttendanceStatus.ABSENT))));

        assertThat(created).hasSize(2);

        List<AttendanceResponse> corrected = attendanceService.markBulk(new BulkAttendanceRequest(
                course.getId(), today,
                List.of(new BulkAttendanceRequest.Entry(second.getId(), AttendanceStatus.PRESENT))));

        assertThat(corrected).hasSize(1);
        assertThat(corrected.get(0).status()).isEqualTo("PRESENT");
        assertThat(attendanceRepository.findByCourseIdAndClassDate(course.getId(), today)).hasSize(2);
    }

    @Test
    @DisplayName("the /me lookup returns only the signed-in student's records")
    void findForUsername_returnsOwnRecordsOnly() {
        Student mine = newStudent();
        Student other = newStudent();
        Course course = newCourse();
        markDays(mine, course, AttendanceStatus.PRESENT, 1);
        markDays(other, course, AttendanceStatus.PRESENT, 1);

        List<AttendanceResponse> records =
                attendanceService.findForUsername(mine.getUser().getUsername());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).rollNumber()).isEqualTo(mine.getRollNumber());
    }

    @Test
    @DisplayName("updating a record corrects its status")
    void updateStatus_correctsRecordedStatus() {
        Student student = newStudent();
        Course course = newCourse();
        AttendanceResponse created = attendanceService.mark(new AttendanceRequest(
                student.getId(), course.getId(), LocalDate.now(), AttendanceStatus.ABSENT));

        AttendanceResponse updated = attendanceService.updateStatus(created.id(), AttendanceStatus.PRESENT);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.status()).isEqualTo("PRESENT");
    }

    private void markDays(Student student, Course course, AttendanceStatus status, int... daysAgo) {
        for (int offset : daysAgo) {
            attendanceService.mark(new AttendanceRequest(
                    student.getId(), course.getId(), LocalDate.now().minusDays(offset), status));
        }
    }

    private Student newStudent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(new User(
                "stu-" + suffix, "{noop}secret", "Student " + suffix,
                "stu-" + suffix + "@college.test", Role.STUDENT));
        return studentRepository.save(new Student(
                "R" + suffix, "Student " + suffix, "CSE", 3, user));
    }

    private Course newCourse() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return courseRepository.save(new Course(
                "C-" + suffix, "Course " + suffix, 4, 3, "CSE"));
    }
}
