package com.college.erp.service;

import com.college.erp.dto.MarksRequest;
import com.college.erp.dto.MarksResponse;
import com.college.erp.entity.Course;
import com.college.erp.entity.Marks;
import com.college.erp.entity.Student;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.MarksRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Iteration 4 - faculty marks entry and correction (FR-06, US-04).
 *
 * Ranges are checked here as well as on {@link MarksRequest} so that the rule holds
 * for any caller, not only for requests that arrive through bean validation. Entities
 * are mapped to DTOs inside the transaction because open-in-view is disabled.
 */
@Service
@Transactional
public class MarksService {

    private final MarksRepository marksRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public MarksService(MarksRepository marksRepository, StudentRepository studentRepository,
                        CourseRepository courseRepository) {
        this.marksRepository = marksRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Records the internal and external marks of one student in one course.
     *
     * @throws NotFoundException   when the student or the course does not exist
     * @throws BadRequestException when a mark is out of range, or marks already exist
     */
    public MarksResponse enter(MarksRequest request) {
        validateRange(request.internalMarks(), request.externalMarks());
        Student student = requireStudent(request.studentId());
        Course course = requireCourse(request.courseId());

        if (marksRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new BadRequestException("Marks for student " + student.getRollNumber()
                    + " in course " + course.getCode() + " already exist; use PUT /api/marks/{id} to correct them");
        }

        Marks marks = new Marks(student, course, request.internalMarks(), request.externalMarks());
        return toResponse(marksRepository.save(marks));
    }

    /**
     * Corrects an already recorded marks entry. Only the two mark components change,
     * because a correction never moves marks to a different student or course.
     *
     * @throws NotFoundException   when no marks entry has that id
     * @throws BadRequestException when a mark is out of range
     */
    public MarksResponse update(Long id, MarksRequest request) {
        validateRange(request.internalMarks(), request.externalMarks());
        Marks marks = require(id);
        marks.setInternalMarks(request.internalMarks());
        marks.setExternalMarks(request.externalMarks());
        return toResponse(marksRepository.save(marks));
    }

    /** Finds one marks entry by id. */
    @Transactional(readOnly = true)
    public MarksResponse findById(Long id) {
        return toResponse(require(id));
    }

    /** Lists every marks entry recorded for one student. */
    @Transactional(readOnly = true)
    public List<MarksResponse> findByStudent(Long studentId) {
        requireStudent(studentId);
        return toResponses(marksRepository.findByStudentId(studentId));
    }

    /** Lists every marks entry recorded for one course. */
    @Transactional(readOnly = true)
    public List<MarksResponse> findByCourse(Long courseId) {
        requireCourse(courseId);
        return toResponses(marksRepository.findByCourseId(courseId));
    }

    /**
     * Returns the marks of the signed-in student, resolved from the principal so that
     * a student can never read another student's marks.
     *
     * @throws NotFoundException when the principal has no student profile
     */
    @Transactional(readOnly = true)
    public List<MarksResponse> findForUsername(String username) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("No student profile linked to " + username));
        return toResponses(marksRepository.findByStudentId(student.getId()));
    }

    private void validateRange(Integer internalMarks, Integer externalMarks) {
        if (internalMarks == null || externalMarks == null) {
            throw new BadRequestException("Internal and external marks are both required");
        }
        if (internalMarks < 0 || internalMarks > Marks.MAX_INTERNAL) {
            throw new BadRequestException("Internal marks must be between 0 and " + Marks.MAX_INTERNAL);
        }
        if (externalMarks < 0 || externalMarks > Marks.MAX_EXTERNAL) {
            throw new BadRequestException("External marks must be between 0 and " + Marks.MAX_EXTERNAL);
        }
    }

    private Marks require(Long id) {
        return marksRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No marks entry with id " + id));
    }

    private Student requireStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No student with id " + studentId));
    }

    private Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("No course with id " + courseId));
    }

    private List<MarksResponse> toResponses(List<Marks> marks) {
        return marks.stream().map(this::toResponse).toList();
    }

    private MarksResponse toResponse(Marks marks) {
        Student student = marks.getStudent();
        Course course = marks.getCourse();
        return new MarksResponse(
                marks.getId(),
                student.getId(),
                student.getRollNumber(),
                course.getId(),
                course.getCode(),
                course.getTitle(),
                marks.getInternalMarks(),
                marks.getExternalMarks(),
                marks.getTotal());
    }
}
