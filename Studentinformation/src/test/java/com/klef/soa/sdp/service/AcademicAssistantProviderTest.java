package com.klef.soa.sdp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.klef.soa.sdp.dto.AcademicInsightsDTO;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;
import com.klef.soa.sdp.security.StudentAccessPolicy;

@ExtendWith(MockitoExtension.class)
class AcademicAssistantProviderTest {
    @Mock StudentRepository students;
    @Mock AcademicInsightService insights;
    @Mock StudentAccessPolicy accessPolicy;

    @AfterEach
    void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test
    void studentQuestionsAlwaysUseTheirOwnRecordEvenWhenAnotherIdIsRequested() {
        authenticate("student@example.edu", "STUDENT");
        Student ownStudent = student(17L, "Asha Rao", "student@example.edu");
        when(students.findByEmail("student@example.edu")).thenReturn(Optional.of(ownStudent));
        when(insights.forStudentId(17L)).thenReturn(academicRecord(17L, "Asha Rao"));

        var answer = provider().answer("student@example.edu", "STUDENT", "What is attendance for student id 99?");

        assertTrue(answer.answer().contains("Asha Rao's recorded attendance is 80.00%"));
        assertEquals(List.of("attendance summary"), answer.sources());
        verify(accessPolicy).checkStudentAccess(17L);
        verify(insights).forStudentId(17L);
    }

    @Test
    void facultyCannotSelectAStudentOutsideTheirAssignedRoster() {
        authenticate("faculty@example.edu", "FACULTY");
        Student assigned = student(22L, "Bea Learner", "bea@example.edu");
        when(students.findByAssignedFacultyEmailIgnoreCase("faculty@example.edu")).thenReturn(List.of(assigned));

        var answer = provider().answer("faculty@example.edu", "FACULTY", "Show attendance for student id 99");

        assertTrue(answer.answer().contains("assigned student"));
        verifyNoInteractions(accessPolicy, insights);
    }

    @Test
    void facultyCanAskAboutAnAssignedStudentByName() {
        authenticate("faculty@example.edu", "FACULTY");
        Student assigned = student(22L, "Bea Learner", "bea@example.edu");
        when(students.findByAssignedFacultyEmailIgnoreCase("faculty@example.edu")).thenReturn(List.of(assigned));
        when(insights.forStudentId(22L)).thenReturn(academicRecord(22L, "Bea Learner"));

        var answer = provider().answer("faculty@example.edu", "FACULTY", "How is Bea Learner doing in attendance?");

        assertTrue(answer.answer().contains("Bea Learner's recorded attendance is 80.00%"));
        verify(accessPolicy).checkStudentAccess(22L);
        verify(insights).forStudentId(22L);
    }

    private AcademicAssistantProvider provider() {
        return new AcademicAssistantProvider(students, insights, accessPolicy);
    }

    private AcademicInsightsDTO academicRecord(long id, String name) {
        return new AcademicInsightsDTO(id, name, "CS", 3, "ACTIVE",
                new AcademicInsightsDTO.Attendance(10, 8, 2, 0, 80, true, "GOOD", "LOW", 75),
                null, List.of(), List.of());
    }

    private Student student(long id, String name, String email) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setEmail(email);
        student.setRole("STUDENT");
        return student;
    }

    private void authenticate(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, "token", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
