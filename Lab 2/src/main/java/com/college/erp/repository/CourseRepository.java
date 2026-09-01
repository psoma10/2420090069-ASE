package com.college.erp.repository;

import com.college.erp.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    List<Course> findByDepartment(String department);

    List<Course> findBySemester(Integer semester);

    List<Course> findByFacultyId(Long facultyId);

    boolean existsByCode(String code);
}
