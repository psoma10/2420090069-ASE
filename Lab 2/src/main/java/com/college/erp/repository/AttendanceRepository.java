package com.college.erp.repository;

import com.college.erp.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Iteration 3 - attendance persistence (FR-05).
 *
 * The finder methods join-fetch student and course because
 * {@code spring.jpa.open-in-view} is false: the service maps entities to DTOs
 * inside the transaction and no lazy proxy may escape it.
 */
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("select a from Attendance a join fetch a.student join fetch a.course "
            + "where a.student.id = :studentId order by a.classDate desc")
    List<Attendance> findByStudentId(@Param("studentId") Long studentId);

    @Query("select a from Attendance a join fetch a.student join fetch a.course "
            + "where a.student.id = :studentId and a.course.id = :courseId order by a.classDate desc")
    List<Attendance> findByStudentIdAndCourseId(@Param("studentId") Long studentId,
                                                @Param("courseId") Long courseId);

    @Query("select a from Attendance a join fetch a.student join fetch a.course "
            + "where a.course.id = :courseId and a.classDate = :classDate order by a.student.rollNumber")
    List<Attendance> findByCourseIdAndClassDate(@Param("courseId") Long courseId,
                                                @Param("classDate") LocalDate classDate);

    @Query("select a from Attendance a join fetch a.student join fetch a.course "
            + "where a.student.rollNumber = :rollNumber order by a.classDate desc")
    List<Attendance> findByStudentRollNumber(@Param("rollNumber") String rollNumber);

    /** Loads a single record with student and course already initialised. */
    @Query("select a from Attendance a join fetch a.student join fetch a.course where a.id = :id")
    Optional<Attendance> findDetailedById(@Param("id") Long id);

    boolean existsByStudentIdAndCourseIdAndClassDate(Long studentId, Long courseId, LocalDate classDate);

    Optional<Attendance> findByStudentIdAndCourseIdAndClassDate(Long studentId, Long courseId, LocalDate classDate);

    /** Number of PRESENT records held for one student in one course. */
    @Query("select count(a) from Attendance a where a.student.id = :studentId and a.course.id = :courseId "
            + "and a.status = com.college.erp.entity.AttendanceStatus.PRESENT")
    long countPresentByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    /** Total number of classes recorded for one student in one course. */
    @Query("select count(a) from Attendance a where a.student.id = :studentId and a.course.id = :courseId")
    long countTotalByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
