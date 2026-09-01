package com.college.erp.service;

import com.college.erp.dto.ResultResponse;
import com.college.erp.dto.ResultSummary;
import com.college.erp.entity.Course;
import com.college.erp.entity.Marks;
import com.college.erp.entity.Result;
import com.college.erp.entity.Student;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.MarksRepository;
import com.college.erp.repository.ResultRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Iteration 4 - result publication, grading and student result view (FR-07, US-05).
 *
 * Grades are never computed here: {@link Result#apply(int)} owns the grading ladder,
 * so re-publishing after a marks correction always yields a consistent grade.
 */
@Service
@Transactional
public class ResultService {

    private final ResultRepository resultRepository;
    private final MarksRepository marksRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public ResultService(ResultRepository resultRepository, MarksRepository marksRepository,
                         StudentRepository studentRepository, CourseRepository courseRepository) {
        this.resultRepository = resultRepository;
        this.marksRepository = marksRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Publishes results for every student who has marks recorded in the course.
     * Students already published are re-published with their current total.
     *
     * @return the number of results created or refreshed
     * @throws NotFoundException when the course does not exist
     */
    public int publishCourse(Long courseId) {
        Course course = requireCourse(courseId);
        List<Marks> marksList = marksRepository.findByCourseId(course.getId());
        if (marksList.isEmpty()) {
            throw new BadRequestException("No marks recorded for course " + course.getCode()
                    + "; enter marks before publishing results");
        }
        marksList.forEach(this::publish);
        return marksList.size();
    }

    /**
     * Publishes or re-publishes the result of one student in one course.
     *
     * @throws NotFoundException   when the student or the course does not exist
     * @throws BadRequestException when the student has no marks in that course
     */
    public ResultResponse publishStudent(Long studentId, Long courseId) {
        Student student = requireStudent(studentId);
        Course course = requireCourse(courseId);
        Marks marks = marksRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow(() -> new BadRequestException("No marks recorded for student "
                        + student.getRollNumber() + " in course " + course.getCode()
                        + "; enter marks before publishing results"));
        return toResponse(publish(marks));
    }

    /** Lists the published results of one student. */
    @Transactional(readOnly = true)
    public List<ResultResponse> findByStudent(Long studentId) {
        requireStudent(studentId);
        return toResponses(resultRepository.findByStudentId(studentId));
    }

    /** Lists the published results of one course. */
    @Transactional(readOnly = true)
    public List<ResultResponse> findByCourse(Long courseId) {
        requireCourse(courseId);
        return toResponses(resultRepository.findByCourseId(courseId));
    }

    /**
     * Returns the published results of the signed-in student, resolved from the
     * principal so that a student can never read another student's results.
     *
     * @throws NotFoundException when the principal has no student profile
     */
    @Transactional(readOnly = true)
    public List<ResultResponse> findForUsername(String username) {
        Student student = requireStudentByUsername(username);
        return toResponses(resultRepository.findByStudentId(student.getId()));
    }

    /**
     * Aggregates the signed-in student's published results into course counts and an
     * overall percentage, reporting zeroes rather than dividing by zero when nothing
     * has been published yet.
     *
     * @throws NotFoundException when the principal has no student profile
     */
    @Transactional(readOnly = true)
    public ResultSummary summaryForUsername(String username) {
        return ResultSummary.of(findForUsername(username));
    }

    /** Aggregates one student's published results, for faculty and admin views. */
    @Transactional(readOnly = true)
    public ResultSummary summaryForStudent(Long studentId) {
        return ResultSummary.of(findByStudent(studentId));
    }

    private Result publish(Marks marks) {
        Long studentId = marks.getStudent().getId();
        Long courseId = marks.getCourse().getId();
        Result result = resultRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseGet(() -> new Result(marks.getStudent(), marks.getCourse(), marks.getTotal()));
        result.apply(marks.getTotal());
        return resultRepository.save(result);
    }

    private Student requireStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No student with id " + studentId));
    }

    private Student requireStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("No student profile linked to " + username));
    }

    private Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("No course with id " + courseId));
    }

    private List<ResultResponse> toResponses(List<Result> results) {
        return results.stream().map(this::toResponse).toList();
    }

    private ResultResponse toResponse(Result result) {
        Student student = result.getStudent();
        Course course = result.getCourse();
        return new ResultResponse(
                result.getId(),
                student.getId(),
                student.getRollNumber(),
                course.getId(),
                course.getCode(),
                course.getTitle(),
                result.getTotalMarks(),
                result.getGrade(),
                result.isPassed(),
                result.getPublishedOn());
    }
}
