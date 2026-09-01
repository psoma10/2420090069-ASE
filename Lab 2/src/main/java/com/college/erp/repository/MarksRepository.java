package com.college.erp.repository;

import com.college.erp.entity.Marks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarksRepository extends JpaRepository<Marks, Long> {

    List<Marks> findByStudentId(Long studentId);

    List<Marks> findByCourseId(Long courseId);

    Optional<Marks> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Marks> findByStudentRollNumber(String rollNumber);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
