package com.college.erp;

import com.college.erp.dto.CourseRequest;
import com.college.erp.dto.CourseResponse;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.service.CourseService;
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
 * Iteration 2 - course management tests (FR-03).
 *
 * Every fixture uses a random course code and a random department so the tests
 * stay independent of seed data and of rows created by other iterations that
 * share the same in-memory H2 database.
 */
@SpringBootTest
@Transactional
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    private static String uniqueCode() {
        return "CS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String uniqueDepartment() {
        return "DEPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static CourseRequest request(String code, String department, Integer semester) {
        return new CourseRequest(code, "Software Engineering", 4, semester, department);
    }

    @Test
    @DisplayName("create persists a course and returns it without a faculty")
    void create_validRequest_persistsCourse() {
        String code = uniqueCode();
        String department = uniqueDepartment();

        CourseResponse created = courseService.create(request(code, department, 3));

        assertThat(created.id()).isNotNull();
        assertThat(created.code()).isEqualTo(code);
        assertThat(created.title()).isEqualTo("Software Engineering");
        assertThat(created.credits()).isEqualTo(4);
        assertThat(created.semester()).isEqualTo(3);
        assertThat(created.department()).isEqualTo(department);
        assertThat(created.facultyId()).isNull();
        assertThat(created.facultyName()).isNull();

        assertThat(courseService.findById(created.id()).code()).isEqualTo(code);
    }

    @Test
    @DisplayName("create rejects a duplicate course code")
    void create_duplicateCode_throwsBadRequest() {
        String code = uniqueCode();
        courseService.create(request(code, uniqueDepartment(), 3));

        assertThatThrownBy(() -> courseService.create(request(code, uniqueDepartment(), 5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(code);
    }

    @Test
    @DisplayName("findAll filters by department and by semester")
    void findAll_departmentFilter_returnsOnlyThatDepartment() {
        String department = uniqueDepartment();
        String otherDepartment = uniqueDepartment();
        CourseResponse mine = courseService.create(request(uniqueCode(), department, 3));
        CourseResponse alsoMine = courseService.create(request(uniqueCode(), department, 6));
        courseService.create(request(uniqueCode(), otherDepartment, 3));

        List<CourseResponse> byDepartment = courseService.findAll(department, null);

        assertThat(byDepartment).extracting(CourseResponse::id)
                .containsExactlyInAnyOrder(mine.id(), alsoMine.id());
        assertThat(byDepartment).allSatisfy(course ->
                assertThat(course.department()).isEqualTo(department));

        List<CourseResponse> byDepartmentAndSemester = courseService.findAll(department, 6);
        assertThat(byDepartmentAndSemester).extracting(CourseResponse::id)
                .containsExactly(alsoMine.id());
    }

    @Test
    @DisplayName("findAll without filters includes every created course")
    void findAll_noFilters_includesCreatedCourse() {
        CourseResponse created = courseService.create(request(uniqueCode(), uniqueDepartment(), 2));

        assertThat(courseService.findAll(null, null))
                .extracting(CourseResponse::id)
                .contains(created.id());
    }

    @Test
    @DisplayName("findById throws when the course does not exist")
    void findById_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> courseService.findById(-1L))
                .isInstanceOf(NotFoundException.class);
    }
}
