package com.college.erp.config;

import com.college.erp.entity.*;
import com.college.erp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Iteration 5 - seeds the demo accounts and a small course catalogue so the ERP
 * can be demonstrated immediately after startup.
 *
 * Skipped under the {@code test} profile so that automated tests control their own data.
 * The demo passwords below are lab credentials only and are BCrypt-hashed before storage.
 */
@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                      StudentRepository studentRepository,
                                      FacultyRepository facultyRepository,
                                      CourseRepository courseRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            userRepository.save(new User("admin", passwordEncoder.encode("admin123"),
                    "System Administrator", "admin@college.edu", Role.ADMIN));

            User facultyUser = userRepository.save(new User("faculty1", passwordEncoder.encode("faculty123"),
                    "Dr. Anitha Rao", "anitha.rao@college.edu", Role.FACULTY));
            Faculty faculty = facultyRepository.save(new Faculty("FAC001", "Dr. Anitha Rao",
                    "Computer Science", "Associate Professor", facultyUser));

            User studentUser = userRepository.save(new User("student1", passwordEncoder.encode("student123"),
                    "Ravi Kumar", "ravi.kumar@college.edu", Role.STUDENT));
            Student student = new Student("CS2024001", "Ravi Kumar", "Computer Science", 3, studentUser);
            student.setPhone("9876543210");
            studentRepository.save(student);

            Course dbms = new Course("CS301", "Database Management Systems", 4, 3, "Computer Science");
            dbms.setFaculty(faculty);
            courseRepository.save(dbms);

            Course se = new Course("CS302", "Software Engineering", 3, 3, "Computer Science");
            se.setFaculty(faculty);
            courseRepository.save(se);

            courseRepository.save(new Course("CS303", "Operating Systems", 4, 3, "Computer Science"));
        };
    }
}
