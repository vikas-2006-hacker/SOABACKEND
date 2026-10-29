package com.klef.soa.sdp.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;
import com.klef.soa.sdp.client.FacultyAuthClient;
import feign.FeignException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FacultyAuthClient facultyAuthClient;

    @Autowired
    public AuthController(StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          FacultyAuthClient facultyAuthClient) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.facultyAuthClient = facultyAuthClient;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Student user = studentRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || "FACULTY".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(callFaculty(() -> facultyAuthClient.login(request)));
        }

        if (user == null || user.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        boolean validPassword = passwordEncoder.matches(request.getPassword(), user.getPassword());

        // One-time migration support for students created before BCrypt was enabled.
        if (!validPassword && !user.getPassword().startsWith("$2a$")
                && !user.getPassword().startsWith("$2b$")
                && !user.getPassword().startsWith("$2y$")
                && request.getPassword().equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            studentRepository.save(user);
            validPassword = true;
        }

        if (!validPassword) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        String role = user.getRole() == null || user.getRole().isBlank()
                ? "STUDENT" : user.getRole().toUpperCase();

        String token = jwtUtil.generateToken(user.getEmail(), role);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", user.getEmail(), role));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@Valid @RequestBody MfaChallengeRequest request) {
        return ResponseEntity.ok(callFaculty(() -> facultyAuthClient.setup(request)));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@Valid @RequestBody MfaVerificationRequest request) {
        FacultyMfaVerifiedResponse result = callFaculty(() -> facultyAuthClient.verify(request));
        if (!"SUCCESS".equals(result.status())) {
            HttpStatus status = "RATE_LIMITED".equals(result.status()) ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body("EXPIRED".equals(result.status()) ? "MFA challenge expired. Sign in again." : "Invalid MFA code or challenge.");
        }
        String token = jwtUtil.generateToken(result.email(), "FACULTY");
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", result.email(), "FACULTY"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (studentRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("An account with this email already exists");
        }
        Student user = new Student();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setAge(18);
        user.setDepartment("GENERAL");
        user.setContact("");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        user.setStatus("ACTIVE");
        studentRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("Account created. You can now sign in.");
    }

    private <T> T callFaculty(java.util.function.Supplier<T> action) {
        try { return action.get(); }
        catch (FeignException exception) {
            if (exception.status() == 401) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password / MFA challenge");
            if (exception.status() == 403) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty account is inactive. Contact an administrator.");
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Faculty authentication service is unavailable.");
        }
    }
}
