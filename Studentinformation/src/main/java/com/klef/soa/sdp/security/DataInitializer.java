package com.klef.soa.sdp.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;

/** Creates only the demo admin account. Faculty records belong to Faculty Service. */
@Configuration
@Profile("dev")
public class DataInitializer {

    @Value("${EDUPULSE_DEV_ADMIN_EMAIL:}") private String adminEmail;
    @Value("${EDUPULSE_DEV_ADMIN_NAME:}") private String adminName;
    @Value("${EDUPULSE_DEV_ADMIN_PASSWORD:}") private String adminPassword;

    @Bean
    CommandLineRunner seedSecurityUsers(StudentRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (adminEmail.isBlank() || adminName.isBlank() || adminPassword.isBlank()) return;
            createUserIfMissing(repository, encoder, adminName, adminEmail, adminPassword, "ADMIN");
        };
    }

    private void createUserIfMissing(StudentRepository repository,
                                     PasswordEncoder encoder,
                                     String name,
                                     String email,
                                     String password,
                                     String role) {
        if (repository.existsByEmail(email)) {
            return;
        }

        Student user = new Student();
        user.setName(name);
        user.setEmail(email);
        user.setAge(30);
        user.setDepartment("ADMINISTRATION");
        user.setContact("0000000000");
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        user.setSemester(1);
        user.setAcademicYear("2025-2026");
        user.setStatus("ACTIVE");
        repository.save(user);
    }
}
