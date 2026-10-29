package com.klef.soa.sdp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.klef.soa.sdp.dto.StudentProfileDTO;
import com.klef.soa.sdp.dto.CurrentStudentDTO;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.service.StudentService;
import com.klef.soa.sdp.client.FacultyDirectoryClient;
import com.klef.soa.sdp.service.NotificationService;
import com.klef.soa.sdp.service.AcademicStructureService;
import com.klef.soa.sdp.dto.NotificationEventRequest;
import feign.FeignException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
@Tag(
    name = "Student Information API",
    description = "Operations for managing student profiles, demographics, and statuses"
)
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private com.klef.soa.sdp.security.StudentAccessPolicy studentAccessPolicy;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private FacultyDirectoryClient facultyDirectoryClient;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AcademicStructureService academicStructureService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's student profile")
    public ResponseEntity<CurrentStudentDTO> getCurrentStudent(
            org.springframework.security.core.Authentication authentication) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        return ResponseEntity.ok(CurrentStudentDTO.from(student));
    }


    // =========================================================
    // CREATE STUDENT
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @PostMapping
    @Operation(
        summary = "Register a new student",
        description = "Creates a new student profile. The system auto-generates the student ID."
    )
    public ResponseEntity<Student> createStudent(
            @Valid @RequestBody Student student,
            org.springframework.security.core.Authentication authentication) {

        if (hasRole(authentication, "FACULTY")) student.setAssignedFacultyEmail(authentication.getName());
        else {
            normalizeFacultyEmail(student);
            validateFacultyAssignment(student);
        }

        Student created = studentService.createStudent(student);
        if (hasRole(authentication, "ADMIN")) {
            notificationService.publishAdminEvent(new NotificationEventRequest(null, "STUDENT_CREATED", null, created.getName()));
        }

        return new ResponseEntity<>(
                created,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // CREATE MULTIPLE STUDENTS
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @PostMapping("/batch")
    @Operation(
        summary = "Register multiple students",
        description = "Creates multiple student profiles in a single request."
    )
    public ResponseEntity<List<Student>> createStudentsBatch(
            @Valid @RequestBody List<Student> students,
            org.springframework.security.core.Authentication authentication) {

        students.forEach(student -> {
            if (hasRole(authentication, "FACULTY")) student.setAssignedFacultyEmail(authentication.getName());
            else {
                normalizeFacultyEmail(student);
                validateFacultyAssignment(student);
            }
        });

        List<Student> createdList =
                studentService.createStudentsBatch(students);
        if (hasRole(authentication, "ADMIN")) {
            createdList.forEach(created -> notificationService.publishAdminEvent(
                    new NotificationEventRequest(null, "STUDENT_CREATED", null, created.getName())));
        }

        return new ResponseEntity<>(
                createdList,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // GET ALL STUDENTS
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @GetMapping
    @Operation(
        summary = "Get all students",
        description = "Retrieves all students with optional department and status filters."
    )
    public ResponseEntity<List<Student>> getAllStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            org.springframework.security.core.Authentication authentication) {

        if (hasRole(authentication, "FACULTY")) {
            List<Student> mapped = academicStructureService.getFacultyStudents(authentication.getName(), null).stream()
                    .filter(student -> "STUDENT".equalsIgnoreCase(student.getRole()))
                    .filter(student -> department == null || department.isBlank() || student.getDepartment().equalsIgnoreCase(department))
                    .filter(student -> status == null || status.isBlank() || student.getStatus().equalsIgnoreCase(status))
                    .toList();
            return ResponseEntity.ok(mapped);
        }

        if (department != null && status != null) {

            return ResponseEntity.ok(
                studentService.getStudentsByDepartmentAndStatus(
                    department,
                    status
                )
            );

        } else if (department != null) {

            return ResponseEntity.ok(
                studentService.getStudentsByDepartment(
                    department
                )
            );

        } else if (status != null) {

            return ResponseEntity.ok(
                studentService.getStudentsByStatus(
                    status
                )
            );
        }

        return ResponseEntity.ok(
            studentService.getAllStudents()
        );
    }


    // =========================================================
    // GET STUDENT BY ID
    // AUTHENTICATED USERS
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Operation(
        summary = "Get student by ID",
        description = "Fetches a single student profile by student ID."
    )
    public ResponseEntity<Student> getStudentById(
            @PathVariable Long id) {

        studentAccessPolicy.checkStudentAccess(id);
        return ResponseEntity.ok(
            studentService.getStudentById(id)
        );
    }


    // =========================================================
    // GET FULL STUDENT PROFILE
    // STUDENT + ATTENDANCE + RESULT
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/profile")
    @Operation(
        summary = "Get full student profile",
        description =
            "Returns the student's personal details together with "
            + "attendance records and academic results aggregated "
            + "from Attendance and Result microservices."
    )
    public ResponseEntity<StudentProfileDTO> getStudentProfile(
            @PathVariable Long id) {

        studentAccessPolicy.checkStudentAccess(id);
        return ResponseEntity.ok(
            studentService.getStudentProfile(id)
        );
    }


    // =========================================================
    // GET STUDENT BY EMAIL
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/email/{email}")
    @Operation(
        summary = "Get student by email",
        description = "Fetches a student profile using institutional email."
    )
    public ResponseEntity<Student> getStudentByEmail(
            @PathVariable String email) {

        studentAccessPolicy.checkEmailAccess(email);
        return ResponseEntity.ok(
            studentService.getStudentByEmail(email)
        );
    }


    // =========================================================
    // GET STUDENTS BY DEPARTMENT
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @GetMapping("/department/{department}")
    @Operation(
        summary = "Get students by department",
        description = "Lists all students enrolled in a department."
    )
    public ResponseEntity<List<Student>> getStudentsByDepartment(
            @PathVariable String department,
            org.springframework.security.core.Authentication authentication) {

        if (hasRole(authentication, "FACULTY")) {
            return ResponseEntity.ok(studentService.getStudentsByFacultyEmail(authentication.getName()).stream()
                    .filter(student -> "STUDENT".equalsIgnoreCase(student.getRole()))
                    .filter(student -> student.getDepartment().equalsIgnoreCase(department)).toList());
        }

        return ResponseEntity.ok(
            studentService.getStudentsByDepartment(department)
        );
    }


    // =========================================================
    // UPDATE STUDENT
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @PutMapping("/{id}")
    @Operation(
        summary = "Update student profile",
        description = "Updates the details of an existing student."
    )
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student,
            org.springframework.security.core.Authentication authentication) {

        studentAccessPolicy.checkStudentAccess(id);
        if (hasRole(authentication, "ADMIN")) {
            normalizeFacultyEmail(student);
            validateFacultyAssignment(student);
        }

        return ResponseEntity.ok(
            studentService.updateStudent(id, student)
        );
    }


    // =========================================================
    // UPDATE STUDENT STATUS
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update student status",
        description =
            "Changes student status such as ACTIVE, INACTIVE, or GRADUATED."
    )
    public ResponseEntity<Student> updateStudentStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        studentAccessPolicy.checkStudentAccess(id);

        return ResponseEntity.ok(
            studentService.updateStudentStatus(
                id,
                status
            )
        );
    }


    // =========================================================
    // DELETE STUDENT
    // ADMIN ONLY
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete student",
        description = "Removes a student profile from the system."
    )
    public ResponseEntity<Map<String, String>> deleteStudent(
            @PathVariable Long id) {

        studentService.deleteStudent(id);

        return ResponseEntity.ok(
            Map.of(
                "message",
                "Student profile deleted successfully for ID: " + id
            )
        );
    }


    // =========================================================
    // CHECK STUDENT EXISTS
    // USED BY OTHER MICROSERVICES
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/check/{id}")
    @Operation(
        summary = "Check if student exists",
        description =
            "Used by Attendance and Result microservices "
            + "for inter-service student validation."
    )
    public ResponseEntity<Map<String, Object>> checkStudentExists(
            @PathVariable Long id,
            @RequestParam(required = false) String courseCode) {

        studentAccessPolicy.checkStudentAccess(id, courseCode);
        boolean exists =
                studentService.checkStudentExists(id);

        return ResponseEntity.ok(
            Map.of(
                "studentId", id,
                "exists", exists
            )
        );
    }


    // =========================================================
    // GET TOTAL STUDENT COUNT
    // ADMIN / FACULTY ONLY
    // =========================================================

    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    @GetMapping("/count")
    @Operation(
        summary = "Get total student count",
        description = "Returns the total number of registered students."
    )
    public ResponseEntity<Map<String, Long>> getTotalCount() {

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        long count = hasRole(authentication, "FACULTY")
                ? studentService.getStudentsByFacultyEmail(authentication.getName()).stream()
                    .filter(student -> "STUDENT".equalsIgnoreCase(student.getRole())).count()
                : studentService.getTotalStudentCount();

        return ResponseEntity.ok(
            Map.of(
                "totalStudents",
                count
            )
        );
    }

    private boolean hasRole(org.springframework.security.core.Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private void normalizeFacultyEmail(Student student) {
        if (student.getAssignedFacultyEmail() != null) {
            String email = student.getAssignedFacultyEmail().trim();
            student.setAssignedFacultyEmail(email.isEmpty() ? null : email.toLowerCase(java.util.Locale.ROOT));
        }
    }

    private void validateFacultyAssignment(Student student) {
        String email = student.getAssignedFacultyEmail();
        if (email == null || email.isBlank()) return;
        if (facultyDirectoryClient == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Faculty Service is unavailable to validate the assignment.");
        }
        try {
            Map<String, Object> faculty = facultyDirectoryClient.getFacultyByEmail(email);
            if (!"ACTIVE".equalsIgnoreCase(String.valueOf(faculty.get("status")))) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned faculty account is not active.");
            }
        } catch (FeignException.NotFound exception) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "No faculty account exists for the assigned email.");
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            throw exception;
        } catch (FeignException exception) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Faculty Service could not validate the assignment.");
        }
    }
}
