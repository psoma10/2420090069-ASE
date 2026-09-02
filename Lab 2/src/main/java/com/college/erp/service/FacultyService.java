package com.college.erp.service;

import com.college.erp.dto.FacultyRequest;
import com.college.erp.dto.FacultyResponse;
import com.college.erp.entity.Faculty;
import com.college.erp.entity.Role;
import com.college.erp.entity.User;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.FacultyRepository;
import com.college.erp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Iteration 5 - faculty management (FR-08).
 */
@Service
@Transactional
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public FacultyService(FacultyRepository facultyRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lists every faculty member, optionally narrowed to one department. */
    @Transactional(readOnly = true)
    public List<FacultyResponse> findAll(String department) {
        List<Faculty> faculty = (department == null || department.isBlank())
                ? facultyRepository.findAll()
                : facultyRepository.findByDepartment(department);
        return faculty.stream().map(this::toResponse).toList();
    }

    /** Finds one faculty member by id. */
    @Transactional(readOnly = true)
    public FacultyResponse findById(Long id) {
        return toResponse(require(id));
    }

    /** Returns the profile of the currently logged-in faculty member. */
    @Transactional(readOnly = true)
    public FacultyResponse findByUsername(String username) {
        Faculty faculty = facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("No faculty profile linked to " + username));
        return toResponse(faculty);
    }

    /** Creates a faculty member together with their login account. */
    public FacultyResponse create(FacultyRequest request) {
        if (facultyRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new BadRequestException("Employee code " + request.employeeCode() + " is already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username " + request.username() + " is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email " + request.email() + " is already registered");
        }

        User user = new User(request.username(), passwordEncoder.encode(request.password()),
                request.name(), request.email(), Role.FACULTY);
        userRepository.save(user);

        Faculty faculty = new Faculty(request.employeeCode(), request.name(), request.department(),
                request.designation(), user);
        return toResponse(facultyRepository.save(faculty));
    }

    /** Updates the editable fields of a faculty member. */
    public FacultyResponse update(Long id, FacultyRequest request) {
        Faculty faculty = require(id);
        faculty.setName(request.name());
        faculty.setDepartment(request.department());
        faculty.setDesignation(request.designation());
        return toResponse(facultyRepository.save(faculty));
    }

    /** Removes a faculty member and their login account. */
    public void delete(Long id) {
        Faculty faculty = require(id);
        User user = faculty.getUser();
        facultyRepository.delete(faculty);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private Faculty require(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No faculty member with id " + id));
    }

    private FacultyResponse toResponse(Faculty faculty) {
        User user = faculty.getUser();
        return new FacultyResponse(
                faculty.getId(),
                faculty.getEmployeeCode(),
                faculty.getName(),
                faculty.getDepartment(),
                faculty.getDesignation(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getEmail());
    }
}
