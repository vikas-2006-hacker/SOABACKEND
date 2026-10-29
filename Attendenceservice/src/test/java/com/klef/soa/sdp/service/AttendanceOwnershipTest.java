package com.klef.soa.sdp.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.klef.soa.sdp.client.StudentClient;
import com.klef.soa.sdp.dto.StudentDTO;
import com.klef.soa.sdp.repository.AttendenceRepository;

class AttendanceOwnershipTest {
    private AttendenceRepository repository;
    private StudentClient studentClient;
    private AttendenceServiceImpl service;

    @BeforeEach void setUp() {
        repository = mock(AttendenceRepository.class);
        studentClient = mock(StudentClient.class);
        service = new AttendenceServiceImpl();
        ReflectionTestUtils.setField(service, "attendenceRepository", repository);
        ReflectionTestUtils.setField(service, "studentClient", studentClient);
        lenient().when(studentClient.getStudentByEmail("student.a@example.edu"))
                .thenReturn(new StudentDTO(10L, "Student A", "student.a@example.edu", 18, "CS", null, null, 1, "ACTIVE"));
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }
    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "student.a@example.edu", "token", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void studentCanReadOwnAttendance() {
        authenticate("STUDENT");
        assertDoesNotThrow(() -> service.getAttendanceByStudentId(10L));
        verify(repository).findByStudentId(10L);
    }
    @Test void studentCannotReadAnotherStudentsAttendance() {
        authenticate("STUDENT");
        assertThrows(AccessDeniedException.class, () -> service.getAttendanceByStudentId(20L));
        verifyNoInteractions(repository);
    }
    @Test void adminCanReadAnyStudentsAttendance() {
        authenticate("ADMIN");
        assertDoesNotThrow(() -> service.getAttendanceByStudentId(20L));
        verify(repository).findByStudentId(20L);
    }
    @Test void facultyMustBeMappedBeforeReadingAttendance() {
        authenticate("FACULTY");
        when(studentClient.checkStudentExists(20L, null)).thenReturn(Map.of("exists", true));
        assertDoesNotThrow(() -> service.getAttendanceByStudentId(20L));
        verify(repository).findByStudentId(20L);
    }
    @Test void facultyCannotReadUnmappedAttendance() {
        authenticate("FACULTY");
        when(studentClient.checkStudentExists(20L, null)).thenReturn(Map.of("exists", false));
        assertThrows(AccessDeniedException.class, () -> service.getAttendanceByStudentId(20L));
        verifyNoInteractions(repository);
    }
}
