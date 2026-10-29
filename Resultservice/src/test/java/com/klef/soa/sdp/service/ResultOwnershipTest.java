package com.klef.soa.sdp.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.klef.soa.sdp.entity.Result;
import com.klef.soa.sdp.exception.ResourceNotFoundException;
import com.klef.soa.sdp.repository.ResultRepository;

class ResultOwnershipTest {
    private ResultRepository repository;
    private StudentClient studentClient;
    private ResultServiceImpl service;

    @BeforeEach void setUp() {
        repository = mock(ResultRepository.class);
        studentClient = mock(StudentClient.class);
        service = new ResultServiceImpl();
        ReflectionTestUtils.setField(service, "resultRepository", repository);
        ReflectionTestUtils.setField(service, "studentClient", studentClient);
        lenient().when(studentClient.getStudentByEmail("student.a@example.edu"))
                .thenReturn(new StudentDTO(10L, "Student A", "student.a@example.edu", 18, "CS", null, null, 1, "2025-2026", "ACTIVE"));
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }
    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "student.a@example.edu", "token", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void studentCanReadOwnResults() {
        authenticate("STUDENT");
        assertDoesNotThrow(() -> service.getResultsByStudentId(10L));
        verify(repository).findByStudentId(10L);
    }
    @Test void studentCannotReadAnotherStudentsResults() {
        authenticate("STUDENT");
        assertThrows(AccessDeniedException.class, () -> service.getResultsByStudentId(20L));
        verifyNoInteractions(repository);
    }
    @Test void adminCanReadAnyStudentsResults() {
        authenticate("ADMIN");
        assertDoesNotThrow(() -> service.getResultsByStudentId(20L));
        verify(repository).findByStudentId(20L);
    }
    @Test void facultyMustBeMappedBeforeReadingResults() {
        authenticate("FACULTY");
        when(studentClient.checkStudentExists(20L, null)).thenReturn(Map.of("exists", true));
        assertDoesNotThrow(() -> service.getResultsByStudentId(20L));
        verify(repository).findByStudentId(20L);
    }
    @Test void facultyCannotReadUnmappedResults() {
        authenticate("FACULTY");
        when(studentClient.checkStudentExists(20L, null)).thenReturn(Map.of("exists", false));
        assertThrows(AccessDeniedException.class, () -> service.getResultsByStudentId(20L));
        verifyNoInteractions(repository);
    }

    @Test void studentsOnlyReceivePublishedResults() {
        authenticate("STUDENT");
        Result published = new Result(1L, 10L, 1, "2025-2026", "CS101", "Algorithms", 3, 35, 50, 85, "A", 9, "PASS");
        Result withheld = new Result(2L, 10L, 1, "2025-2026", "CS102", "Networks", 3, 35, 50, 85, "A", 9, "WITHHELD");
        when(repository.findByStudentId(10L)).thenReturn(List.of(published, withheld));

        assertEquals(List.of(published), service.getResultsByStudentId(10L));
    }

    @Test void studentCannotFetchAnIndividualWithheldResult() {
        authenticate("STUDENT");
        Result withheld = new Result(2L, 10L, 1, "2025-2026", "CS102", "Networks", 3, 35, 50, 85, "A", 9, "WITHHELD");
        when(repository.findById(2L)).thenReturn(Optional.of(withheld));

        assertThrows(ResourceNotFoundException.class, () -> service.getResultById(2L));
    }
}
