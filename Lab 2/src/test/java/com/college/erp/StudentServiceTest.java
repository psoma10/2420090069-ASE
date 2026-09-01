package com.college.erp;

import com.college.erp.dto.StudentRequest;
import com.college.erp.dto.StudentResponse;
import com.college.erp.exception.BadRequestException;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.UserRepository;
import com.college.erp.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Iteration 1 - verifies student information management (FR-02).
 */
@SpringBootTest
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserRepository userRepository;

    private StudentRequest sampleRequest() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return new StudentRequest(
                "CS-" + unique,
                "Test Student",
                "Computer Science",
                3,
                "9990001111",
                "student-" + unique,
                "secret123",
                unique + "@college.edu");
    }

    @Test
    void createStoresProfileAndHashesPassword() {
        StudentRequest request = sampleRequest();

        StudentResponse created = studentService.create(request);

        assertNotNull(created.id());
        assertEquals(request.rollNumber(), created.rollNumber());
        assertEquals("Computer Science", created.department());
        assertEquals(request.username(), created.username());

        String stored = userRepository.findByUsername(request.username()).orElseThrow().getPassword();
        assertNotEquals(request.password(), stored, "password must never be stored in plain text");
        assertTrue(stored.startsWith("$2"), "password should be BCrypt hashed");
    }

    @Test
    void createRejectsDuplicateRollNumber() {
        StudentRequest first = sampleRequest();
        studentService.create(first);

        StudentRequest duplicate = new StudentRequest(
                first.rollNumber(), "Another Student", "Computer Science", 2, null,
                first.username() + "-2", "secret123", "other-" + first.email());

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> studentService.create(duplicate));
        assertTrue(error.getMessage().contains(first.rollNumber()));
    }

    @Test
    void findByRollNumberReturnsTheStudent() {
        StudentRequest request = sampleRequest();
        studentService.create(request);

        StudentResponse found = studentService.findByRollNumber(request.rollNumber());

        assertEquals(request.name(), found.name());
        assertEquals(request.semester(), found.semester());
    }

    @Test
    void findByRollNumberFailsForUnknownStudent() {
        assertThrows(NotFoundException.class, () -> studentService.findByRollNumber("NO-SUCH-ROLL"));
    }

    @Test
    void updateChangesEditableFields() {
        StudentResponse created = studentService.create(sampleRequest());

        StudentRequest update = new StudentRequest(
                created.rollNumber(), "Renamed Student", "Information Technology", 5, "8887776666",
                created.username(), "secret123", created.email());

        StudentResponse updated = studentService.update(created.id(), update);

        assertEquals("Renamed Student", updated.name());
        assertEquals("Information Technology", updated.department());
        assertEquals(5, updated.semester());
        assertEquals("8887776666", updated.phone());
    }
}
