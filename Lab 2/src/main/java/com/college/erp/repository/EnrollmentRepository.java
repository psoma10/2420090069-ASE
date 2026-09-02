package com.college.erp.repository;

import com.college.erp.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Iteration 2 - persistence for student course registrations (FR-04).
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Enrollments of one student, with course and faculty eagerly joined so the
     * service can map them after the transaction closes (open-in-view is off).
     */
    @Query("select e from Enrollment e "
            + "join fetch e.course c left join fetch c.faculty "
            + "join fetch e.student where e.student.id = :studentId")
    List<Enrollment> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Roster of one course, with student and course graph eagerly joined.
     */
    @Query("select e from Enrollment e "
            + "join fetch e.course c left join fetch c.faculty "
            + "join fetch e.student where e.course.id = :courseId")
    List<Enrollment> findByCourseId(@Param("courseId") Long courseId);

    @Query("select e from Enrollment e "
            + "join fetch e.course c left join fetch c.faculty "
            + "join fetch e.student s where s.rollNumber = :rollNumber")
    List<Enrollment> findByStudentRollNumber(@Param("rollNumber") String rollNumber);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByCourseId(Long courseId);
}
