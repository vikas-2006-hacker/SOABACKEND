package com.klef.soa.faculty.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.klef.soa.faculty.entity.Faculty;
import com.klef.soa.faculty.repository.FacultyRepository;
@Component @Profile("dev")
public class FacultyDevInitializer implements CommandLineRunner {
 private final FacultyRepository repository;private final PasswordEncoder encoder;private final String password;
 public FacultyDevInitializer(FacultyRepository repository,PasswordEncoder encoder,@Value("${FACULTY_DEV_PASSWORD:}") String password){this.repository=repository;this.encoder=encoder;this.password=password;}
 @Override public void run(String... args){if(password.isBlank()||repository.existsByEmailIgnoreCase("faculty@gmail.com"))return;Faculty f=new Faculty();f.setName("Development Faculty");f.setEmail("faculty@gmail.com");f.setEmployeeId("DEV-FAC001");f.setDepartment("General");f.setStatus("ACTIVE");f.setPasswordHash(encoder.encode(password));f.setMfaEnabled(false);repository.save(f);}
}
