package com.klef.soa.sdp.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.klef.soa.sdp.repository.StudentRepository;
import com.klef.soa.sdp.repository.StudentCourseEnrollmentRepository;
import com.klef.soa.sdp.repository.CourseRepository;
import com.klef.soa.sdp.repository.FacultyCourseAssignmentRepository;
import com.klef.soa.sdp.entity.StudentCourseEnrollment;

@Component("studentAccessPolicy")
public class StudentAccessPolicy {
    private final StudentRepository studentRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final FacultyCourseAssignmentRepository facultyAssignments;

    public StudentAccessPolicy(StudentRepository studentRepository,
            StudentCourseEnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            FacultyCourseAssignmentRepository facultyAssignments) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.facultyAssignments = facultyAssignments;
    }

    public void checkStudentAccess(Long studentId) {
        checkStudentAccess(studentId, null);
    }

    public void checkStudentAccess(Long studentId, String courseCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"))) {
            boolean assigned = courseCode == null || courseCode.isBlank()
                    ? enrollmentRepository.findByStudentIdAndFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(studentId, auth.getName())
                            .stream()
                            .filter(enrollment -> enrollment.getStatus() == StudentCourseEnrollment.EnrollmentStatus.ACTIVE)
                            .anyMatch(enrollment -> facultyAssignments.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc(auth.getName())
                                    .stream().anyMatch(assignment -> assignment.isActive()
                                            && assignment.getCourseId().equals(enrollment.getCourseId())))
                    : courseRepository.findByCourseCodeIgnoreCase(courseCode.trim())
                            .filter(course -> course.isActive())
                            .map(course -> facultyAssignments.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc(auth.getName())
                                    .stream().anyMatch(assignment -> assignment.isActive()
                                            && assignment.getCourseId().equals(course.getId()))
                                    && enrollmentRepository.existsByStudentIdAndCourseIdAndFacultyEmailIgnoreCaseAndStatus(
                                            studentId, course.getId(), auth.getName(), StudentCourseEnrollment.EnrollmentStatus.ACTIVE))
                            .orElse(false);
            if (assigned) return;
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))
                && studentRepository.findByEmailIgnoreCase(auth.getName()).map(s -> s.getId().equals(studentId)).orElse(false)) return;
        throw new AccessDeniedException("Access denied for this student");
    }

    public void checkEmailAccess(String email) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return;
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"))
                && studentRepository.findByEmailIgnoreCase(email).map(s -> enrollmentRepository
                    .existsByStudentIdAndFacultyEmailIgnoreCaseAndStatus(s.getId(), auth.getName(), StudentCourseEnrollment.EnrollmentStatus.ACTIVE)).orElse(false)) return;
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))
                && auth.getName().equalsIgnoreCase(email)) return;
        throw new AccessDeniedException("Access denied for this student");
    }
}
