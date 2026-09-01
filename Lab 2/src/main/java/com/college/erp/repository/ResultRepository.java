package com.college.erp.repository;

import com.college.erp.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentId(Long studentId);

    Optional<Result> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Result> findByCourseId(Long courseId);

    List<Result> findByStudentRollNumber(String rollNumber);
}
