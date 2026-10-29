package com.klef.soa.sdp.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.entity.Course;
import com.klef.soa.sdp.entity.FacultyCourseAssignment;
import com.klef.soa.sdp.entity.StudentCourseEnrollment;
import com.klef.soa.sdp.repository.CourseRepository;
import com.klef.soa.sdp.repository.FacultyCourseAssignmentRepository;
import com.klef.soa.sdp.repository.StudentCourseEnrollmentRepository;
import com.klef.soa.sdp.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentAccessPolicyTest {
    @Mock StudentRepository repository;
    @Mock StudentCourseEnrollmentRepository enrollments;
    @Mock CourseRepository courses;
    @Mock FacultyCourseAssignmentRepository facultyAssignments;
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    private StudentAccessPolicy policy() { return new StudentAccessPolicy(repository, enrollments, courses, facultyAssignments); }
    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                role.equals("FACULTY") ? "faculty@example.edu" : "student.a@example.edu", "token", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    private Student student(Long id) { Student s = new Student(); s.setId(id); s.setEmail("student.a@example.edu"); s.setAssignedFacultyEmail("faculty@example.edu"); return s; }

    @Test void studentCanReadOwnProfile() {
        authenticate("STUDENT"); when(repository.findByEmailIgnoreCase("student.a@example.edu")).thenReturn(Optional.of(student(10L)));
        assertDoesNotThrow(() -> policy().checkStudentAccess(10L));
    }
    @Test void studentCannotReadAnotherProfile() {
        authenticate("STUDENT"); when(repository.findByEmailIgnoreCase("student.a@example.edu")).thenReturn(Optional.of(student(10L)));
        assertThrows(AccessDeniedException.class, () -> policy().checkStudentAccess(20L));
    }
    @Test void adminCanReadAnyProfile() {
        authenticate("ADMIN");
        assertDoesNotThrow(() -> policy().checkStudentAccess(20L));
    }
    @Test void facultyCanReadOnlyStudentsWithAnActiveEnrollmentInTheirCourse() {
        authenticate("FACULTY");
        StudentCourseEnrollment enrollment = new StudentCourseEnrollment();
        enrollment.setCourseId(7L);
        enrollment.setStatus(StudentCourseEnrollment.EnrollmentStatus.ACTIVE);
        when(enrollments.findByStudentIdAndFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(20L, "faculty@example.edu"))
                .thenReturn(List.of(enrollment));
        FacultyCourseAssignment activeAssignment = new FacultyCourseAssignment();
        activeAssignment.setActive(true);
        activeAssignment.setCourseId(7L);
        when(facultyAssignments.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc("faculty@example.edu"))
                .thenReturn(List.of(activeAssignment));
        assertDoesNotThrow(() -> policy().checkStudentAccess(20L));
        when(enrollments.findByStudentIdAndFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(21L, "faculty@example.edu"))
                .thenReturn(List.of());
        assertThrows(AccessDeniedException.class, () -> policy().checkStudentAccess(21L));
    }

    @Test void facultyCourseChecksAreScopedToTheCourseOffering() {
        authenticate("FACULTY");
        Course course = new Course(); course.setId(7L); course.setActive(true);
        when(courses.findByCourseCodeIgnoreCase("DS101")).thenReturn(Optional.of(course));
        FacultyCourseAssignment activeAssignment = new FacultyCourseAssignment(); activeAssignment.setActive(true); activeAssignment.setCourseId(7L);
        when(facultyAssignments.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc("faculty@example.edu"))
                .thenReturn(List.of(activeAssignment));
        when(enrollments.existsByStudentIdAndCourseIdAndFacultyEmailIgnoreCaseAndStatus(
                20L, 7L, "faculty@example.edu", StudentCourseEnrollment.EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertDoesNotThrow(() -> policy().checkStudentAccess(20L, "DS101"));
        assertThrows(AccessDeniedException.class, () -> policy().checkStudentAccess(20L, "OTHER"));
    }
}
