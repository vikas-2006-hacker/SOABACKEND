package com.klef.soa.faculty.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.faculty.dto.FacultyResponse;
import com.klef.soa.faculty.dto.FacultyDirectoryResponse;
import com.klef.soa.faculty.exception.ApiExceptionHandler;
import com.klef.soa.faculty.security.JwtAuthenticationFilter;
import com.klef.soa.faculty.security.SecurityConfig;
import com.klef.soa.faculty.service.FacultyService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@WebMvcTest(FacultyController.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=" + "mySchoolManagementJwtSecret2026DevelopmentKey")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class, FacultyProfileEndpointTest.StubServiceConfiguration.class })
class FacultyProfileEndpointTest {
    private static final String SECRET = "mySchoolManagementJwtSecret2026DevelopmentKey";

    @Autowired
    private MockMvc mvc;

    @Test
    void authenticatedFacultyLoadsTheirOwnProfileAndResponseShape() throws Exception {
        mvc.perform(get("/api/faculty/me").header("Authorization", "Bearer " + token("saketh@gmail.com", "FACULTY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Saketh Anand"))
                .andExpect(jsonPath("$.email").value("saketh@gmail.com"))
                .andExpect(jsonPath("$.employeeId").value("26000001"))
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.designation").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.mfaEnabled").value(true));
    }

    @Test
    void unauthenticatedAndNonFacultyRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/faculty/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/faculty/me").header("Authorization", "Bearer " + token("admin@example.edu", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedIdentityWithoutFacultyRecordReturnsNotFound() throws Exception {
        mvc.perform(get("/api/faculty/me").header("Authorization", "Bearer " + token("missing@example.invalid", "FACULTY")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Faculty not found"));
    }

    @Test
    void studentCanReadOnlyThePublicFacultyDirectoryFields() throws Exception {
        mvc.perform(get("/api/faculty/directory/saketh@gmail.com")
                        .header("Authorization", "Bearer " + token("student@example.edu", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Saketh Anand"))
                .andExpect(jsonPath("$.email").value("saketh@gmail.com"))
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.employeeId").doesNotExist())
                .andExpect(jsonPath("$.mfaEnabled").doesNotExist());
    }

    private String token(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder().subject(email).claim("role", role).issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000)).signWith(key).compact();
    }

    @TestConfiguration
    static class StubServiceConfiguration {
        @Bean
        @Primary
        FacultyService facultyService() {
            return new FacultyService(null, null) {
                @Override
                public FacultyResponse byEmail(String email) {
                    if (!"saketh@gmail.com".equalsIgnoreCase(email)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found");
                    }
                    return new FacultyResponse(1L, "Saketh Anand", email, "26000001", "CSE",
                            null, null, "ACTIVE", true, null, null);
                }

                @Override
                public FacultyDirectoryResponse directory(String email) {
                    if (!"saketh@gmail.com".equalsIgnoreCase(email)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found");
                    }
                    return new FacultyDirectoryResponse(1L, "Saketh Anand", email, "CSE", "Professor", "ACTIVE");
                }
            };
        }
    }
}
