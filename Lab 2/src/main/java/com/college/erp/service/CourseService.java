package com.college.erp.service;

import com.college.erp.dto.CourseRequest;
import com.college.erp.dto.CourseResponse;
import com.college.erp.entity.Course;
import com.college.erp.entity.Faculty;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.FacultyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Iteration 2 - course catalogue management (FR-03).
 *
 * Every method maps entities to {@link CourseResponse} inside its transaction,
 * because spring.jpa.open-in-view is false and the lazy faculty association
 * cannot be resolved once the transaction has closed.
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;

    public CourseService(CourseRepository courseRepository, FacultyRepository facultyRepository) {
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
    }

    /**
     * Lists courses, optionally narrowed by department and/or semester.
     *
     * @param department department to filter on, or null/blank for all departments
     * @param semester   semester to filter on, or null for all semesters
     * @return matching courses, never null
     */
    public List<CourseResponse> findAll(String department, Integer semester) {
        boolean byDepartment = department != null && !department.isBlank();
        String wanted = byDepartment ? department.trim() : null;

        return courseRepository.findAll().stream()
                .filter(course -> !byDepartment || course.getDepartment().equalsIgnoreCase(wanted))
                .filter(course -> semester == null || semester.equals(course.getSemester()))
                .map(CourseService::toResponse)
                .toList();
    }

    /**
     * Fetches one course by its identifier.
     *
     * @param id course identifier
     * @return the course view
     * @throws NotFoundException when no course has that id
     */
    public CourseResponse findById(Long id) {
        return toResponse(requireCourse(id));
    }

    /**
     * Creates a new course.
     *
     * @param request validated course details
     * @return the persisted course view
     * @throws BadRequestException when the course code is already taken
     */
    @Transactional
    public CourseResponse create(CourseRequest request) {
        String code = request.code().trim();
        if (courseRepository.existsByCode(code)) {
            throw new BadRequestException("Course code already exists: " + code);
        }
        Course course = new Course(code, request.title().trim(), request.credits(),
                request.semester(), request.department().trim());
        return toResponse(courseRepository.save(course));
    }

    /**
     * Updates the mutable details of an existing course. The course code is immutable.
     *
     * @param id      course identifier
     * @param request validated course details
     * @return the updated course view
     * @throws NotFoundException when no course has that id
     */
    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = requireCourse(id);
        course.setTitle(request.title().trim());
        course.setCredits(request.credits());
        course.setSemester(request.semester());
        course.setDepartment(request.department().trim());
        return toResponse(courseRepository.save(course));
    }

    /**
     * Deletes a course.
     *
     * @param id course identifier
     * @throws NotFoundException when no course has that id
     */
    @Transactional
    public void delete(Long id) {
        courseRepository.delete(requireCourse(id));
    }

    /**
     * Assigns a faculty member as the owner of a course.
     *
     * @param courseId  course identifier
     * @param facultyId faculty identifier
     * @return the updated course view including the faculty name
     * @throws NotFoundException when either the course or the faculty is missing
     */
    @Transactional
    public CourseResponse assignFaculty(Long courseId, Long facultyId) {
        Course course = requireCourse(courseId);
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new NotFoundException("Faculty not found: id=" + facultyId));
        course.setFaculty(faculty);
        return toResponse(courseRepository.save(course));
    }

    private Course requireCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course not found: id=" + id));
    }

    /** Flattens a course (and its lazy faculty) into a detached response record. */
    static CourseResponse toResponse(Course course) {
        Faculty faculty = course.getFaculty();
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getCredits(),
                course.getSemester(),
                course.getDepartment(),
                faculty == null ? null : faculty.getId(),
                faculty == null ? null : faculty.getName());
    }
}
