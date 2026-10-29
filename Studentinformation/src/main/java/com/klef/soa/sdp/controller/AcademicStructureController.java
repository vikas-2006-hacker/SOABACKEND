package com.klef.soa.sdp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klef.soa.sdp.dto.CourseRequest;
import com.klef.soa.sdp.dto.CourseOfferingDTO;
import com.klef.soa.sdp.dto.FacultyCourseAssignmentRequest;
import com.klef.soa.sdp.dto.StudentEnrollmentRequest;
import com.klef.soa.sdp.dto.StudentCourseEnrollmentDTO;
import com.klef.soa.sdp.entity.Course;
import com.klef.soa.sdp.entity.FacultyCourseAssignment;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.entity.StudentCourseEnrollment;
import com.klef.soa.sdp.service.AcademicStructureService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class AcademicStructureController {

    private final AcademicStructureService academicStructureService;

    public AcademicStructureController(AcademicStructureService academicStructureService) {
        this.academicStructureService = academicStructureService;
    }

    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    @GetMapping("/courses")
    public List<Course> getCourses(Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return academicStructureService.listAllCourses();
        }
        return academicStructureService.getFacultyCourses(authentication.getName());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/courses/available")
    public List<CourseOfferingDTO> getAvailableCourses(Authentication authentication) {
        return academicStructureService.getAvailableCoursesForStudent(authentication.getName());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/courses/available/{courseId}")
    public CourseOfferingDTO getStudentCourseDetails(@PathVariable Long courseId, Authentication authentication) {
        return academicStructureService.getStudentCourseDetails(courseId, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    @GetMapping("/courses/{courseId}")
    public Course getCourse(@PathVariable Long courseId, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return academicStructureService.getCourse(courseId);
        }
        return academicStructureService.getFacultyCourse(courseId, authentication.getName());
    }

    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    @GetMapping("/courses/{courseId}/faculty")
    public List<FacultyCourseAssignment> getCourseFaculty(@PathVariable Long courseId, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return academicStructureService.getCourseFaculty(courseId);
        }
        return academicStructureService.getCourseFaculty(courseId, authentication.getName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/courses")
    public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicStructureService.createCourse(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/courses/{courseId}")
    public Course updateCourse(@PathVariable Long courseId, @RequestBody CourseRequest request) {
        return academicStructureService.updateCourse(courseId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/faculty-courses")
    public ResponseEntity<FacultyCourseAssignment> assignFacultyToCourse(@Valid @RequestBody FacultyCourseAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicStructureService.assignFacultyToCourse(request));
    }

    @PreAuthorize("hasRole('FACULTY')")
    @GetMapping("/faculty/my-courses")
    public List<Course> getFacultyCourses(Authentication authentication) {
        return academicStructureService.getFacultyCourses(authentication.getName());
    }

    @PreAuthorize("hasRole('FACULTY')")
    @GetMapping("/faculty/my-students")
    public List<Student> getFacultyStudents(@RequestParam(required = false) Long courseId, Authentication authentication) {
        return academicStructureService.getFacultyStudents(authentication.getName(), courseId);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/enrollments")
    public ResponseEntity<StudentCourseEnrollmentDTO> enroll(@Valid @RequestBody StudentEnrollmentRequest request, Authentication authentication) {
        StudentCourseEnrollmentDTO enrollment = academicStructureService.enrollStudent(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/enrollments/my")
    public List<StudentCourseEnrollmentDTO> getMyEnrollments(Authentication authentication) {
        return academicStructureService.getCurrentStudentEnrollments(authentication.getName());
    }

    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','ADMIN')")
    @GetMapping("/enrollments/my/{studentId}")
    public List<StudentCourseEnrollmentDTO> getStudentEnrollments(@PathVariable Long studentId, Authentication authentication) {
        return academicStructureService.getStudentEnrollments(studentId, authentication.getName(), authentication.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("STUDENT"));
    }

    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','ADMIN')")
    @DeleteMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<Map<String, String>> removeEnrollment(@PathVariable Long enrollmentId, Authentication authentication) {
        String role = authentication.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("STUDENT");
        academicStructureService.deleteEnrollment(enrollmentId, authentication.getName(), role);
        return ResponseEntity.ok(Map.of("message", "Enrollment removed successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/enrollments")
    public List<StudentCourseEnrollment> listAllEnrollments() {
        return academicStructureService.listAllEnrollments();
    }
}
