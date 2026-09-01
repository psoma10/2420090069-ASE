package com.college.erp.service;

import com.college.erp.dto.StudentRequest;
import com.college.erp.dto.StudentResponse;
import com.college.erp.entity.Role;
import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Iteration 1 - student profile and academic information (FR-02).
 */
@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lists every student, optionally narrowed to one department. */
    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(String department) {
        List<Student> students = (department == null || department.isBlank())
                ? studentRepository.findAll()
                : studentRepository.findByDepartment(department);
        return students.stream().map(this::toResponse).toList();
    }

    /** Finds one student by id. */
    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return toResponse(require(id));
    }

    /** Returns the profile of the currently logged-in student. */
    @Transactional(readOnly = true)
    public StudentResponse findByUsername(String username) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("No student profile linked to " + username));
        return toResponse(student);
    }

    /** Finds one student by roll number. */
    @Transactional(readOnly = true)
    public StudentResponse findByRollNumber(String rollNumber) {
        Student student = studentRepository.findByRollNumber(rollNumber)
                .orElseThrow(() -> new NotFoundException("No student with roll number " + rollNumber));
        return toResponse(student);
    }

    /** Creates a student together with their login account. */
    public StudentResponse create(StudentRequest request) {
        if (studentRepository.existsByRollNumber(request.rollNumber())) {
            throw new BadRequestException("Roll number " + request.rollNumber() + " is already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username " + request.username() + " is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email " + request.email() + " is already registered");
        }

        User user = new User(request.username(), passwordEncoder.encode(request.password()),
                request.name(), request.email(), Role.STUDENT);
        userRepository.save(user);

        Student student = new Student(request.rollNumber(), request.name(), request.department(),
                request.semester(), user);
        student.setPhone(request.phone());
        return toResponse(studentRepository.save(student));
    }

    /** Updates the editable fields of a student profile. */
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = require(id);
        student.setName(request.name());
        student.setDepartment(request.department());
        student.setSemester(request.semester());
        student.setPhone(request.phone());
        return toResponse(studentRepository.save(student));
    }

    /** Removes a student and their login account. */
    public void delete(Long id) {
        Student student = require(id);
        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private Student require(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No student with id " + id));
    }

    private StudentResponse toResponse(Student student) {
        User user = student.getUser();
        return new StudentResponse(
                student.getId(),
                student.getRollNumber(),
                student.getName(),
                student.getDepartment(),
                student.getSemester(),
                student.getPhone(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getEmail());
    }
}
