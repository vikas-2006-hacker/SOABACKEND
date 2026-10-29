package com.klef.soa.sdp.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.klef.soa.sdp.client.FacultyAuthClient;

import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AuthControllerMfaTest {
    @Mock StudentRepository studentRepository;
    @Mock PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil = new JwtUtil();
    @Mock FacultyAuthClient facultyAuthClient;

    @BeforeEach void configureJwt() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "TestSecretKeyForFacultyServiceJwtSigning2026");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60000L);
    }

    private AuthController controller() {
        return new AuthController(studentRepository, passwordEncoder, jwtUtil, facultyAuthClient);
    }
    private LoginRequest login(String email) {
        LoginRequest request = new LoginRequest(); request.setEmail(email); request.setPassword("correct-password"); return request;
    }
    private Student account(String role) {
        Student student = new Student(); student.setId(9L); student.setEmail("person@example.edu");
        student.setPassword("encoded"); student.setRole(role); return student;
    }

    @Test void studentGetsJwtWithoutMfa() {
        Student student = account("STUDENT");
        when(studentRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("correct-password", "encoded")).thenReturn(true);
        ResponseEntity<?> response = controller().login(login(student.getEmail()));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(LoginResponse.class, response.getBody());
        assertEquals("STUDENT", jwtUtil.extractRole(((LoginResponse) response.getBody()).getToken()));
    }

    @Test void adminGetsJwtWithoutMfa() {
        Student admin = account("ADMIN");
        when(studentRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("correct-password", "encoded")).thenReturn(true);
        ResponseEntity<?> response = controller().login(login(admin.getEmail()));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(LoginResponse.class, response.getBody());
        assertEquals("ADMIN", jwtUtil.extractRole(((LoginResponse) response.getBody()).getToken()));
    }

    @Test void facultyGetsChallengeInsteadOfJwtUntilMfa() {
        Student faculty = account("FACULTY");
        when(studentRepository.findByEmail(faculty.getEmail())).thenReturn(Optional.of(faculty));
        MfaChallengeResponse challenge = new MfaChallengeResponse(true, true, "challenge-id");
        when(facultyAuthClient.login(any())).thenReturn(challenge);
        ResponseEntity<?> response = controller().login(login(faculty.getEmail()));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(challenge, response.getBody());
        assertInstanceOf(MfaChallengeResponse.class, response.getBody());
    }

    @Test void validFacultyMfaIsTheOnlyPathToFacultyJwt() {
        Student faculty = account("FACULTY");
        when(facultyAuthClient.verify(any())).thenReturn(new FacultyMfaVerifiedResponse("SUCCESS", faculty.getEmail()));
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setChallengeId("challenge-id"); request.setCode("123456");
        ResponseEntity<?> response = controller().verifyMfa(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(LoginResponse.class, response.getBody());
        assertEquals("FACULTY", jwtUtil.extractRole(((LoginResponse) response.getBody()).getToken()));
    }

    @Test void invalidFacultyMfaDoesNotIssueJwt() {
        when(facultyAuthClient.verify(any())).thenReturn(new FacultyMfaVerifiedResponse("INVALID", null));
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setChallengeId("challenge-id"); request.setCode("111111");
        ResponseEntity<?> response = controller().verifyMfa(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody() instanceof LoginResponse);
    }

    @Test void registrationAlwaysCreatesStudentRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User"); request.setEmail("new@example.edu"); request.setPassword("password123");
        when(studentRepository.existsByEmail("new@example.edu")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(studentRepository.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));
        ResponseEntity<?> response = controller().register(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var saved = org.mockito.ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(saved.capture());
        assertEquals("STUDENT", saved.getValue().getRole());
    }
}
