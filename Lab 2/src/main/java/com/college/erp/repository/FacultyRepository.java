package com.college.erp.repository;

import com.college.erp.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByEmployeeCode(String employeeCode);

    Optional<Faculty> findByUserUsername(String username);

    List<Faculty> findByDepartment(String department);

    boolean existsByEmployeeCode(String employeeCode);
}
