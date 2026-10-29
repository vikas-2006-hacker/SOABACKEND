package com.klef.soa.sdp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import com.klef.soa.sdp.dto.CourseRequest;
import com.klef.soa.sdp.dto.FacultyCourseAssignmentRequest;
import com.klef.soa.sdp.dto.StudentEnrollmentRequest;
import com.klef.soa.sdp.dto.FacultyDirectoryDTO;
import com.klef.soa.sdp.client.FacultyDirectoryClient;
import com.klef.soa.sdp.entity.Course;
import com.klef.soa.sdp.entity.FacultyCourseAssignment;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.CourseRepository;
import com.klef.soa.sdp.repository.FacultyCourseAssignmentRepository;
import com.klef.soa.sdp.repository.StudentCourseEnrollmentRepository;
import com.klef.soa.sdp.repository.StudentRepository;

class AcademicStructureServiceTest {
    private CourseRepository courseRepository;
    private FacultyCourseAssignmentRepository facultyAssignmentRepository;
    private StudentCourseEnrollmentRepository enrollmentRepository;
    private StudentRepository studentRepository;
    private NotificationService notificationService;
    private FacultyDirectoryClient facultyDirectoryClient;
    private AcademicStructureService service;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        facultyAssignmentRepository = mock(FacultyCourseAssignmentRepository.class);
        enrollmentRepository = mock(StudentCourseEnrollmentRepository.class);
        studentRepository = mock(StudentRepository.class);
        notificationService = mock(NotificationService.class);
        facultyDirectoryClient = mock(FacultyDirectoryClient.class);
        service = new AcademicStructureService(courseRepository, facultyAssignmentRepository, enrollmentRepository, studentRepository, notificationService, facultyDirectoryClient);
    }

    @Test
    void createCoursePersistsCourse() {
        when(courseRepository.findByCourseCodeIgnoreCase("DS1010")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course saved = service.createCourse(new CourseRequest("DS1010", "Data Science", "CSE", 2, "2026-2027", 4, "data science", true));

        assertEquals("DS1010", saved.getCourseCode());
        assertEquals("Data Science", saved.getCourseName());
        assertEquals("CSE", saved.getDepartment());
        assertEquals(4, saved.getCredits());
        assertEquals(2, saved.getSemester());
        assertEquals("2026-2027", saved.getAcademicYear());
        assertEquals("data science", saved.getDescription());
        verify(courseRepository).save(any(Course.class));
        verifyNoInteractions(facultyAssignmentRepository, enrollmentRepository, studentRepository, facultyDirectoryClient);
    }

    @Test
    void duplicateCourseCodeIsRejectedBeforeSave() {
        when(courseRepository.findByCourseCodeIgnoreCase("DS1010")).thenReturn(Optional.of(new Course()));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () ->
                service.createCourse(new CourseRequest("DS1010", "Data Science", "CSE", 2, "2026-2027", 4, "data science", true)));

        assertEquals(400, thrown.getStatusCode().value());
        assertEquals("Course code already exists", thrown.getReason());
        org.mockito.Mockito.verify(courseRepository, org.mockito.Mockito.never()).save(any(Course.class));
        verifyNoInteractions(facultyAssignmentRepository, enrollmentRepository, studentRepository, facultyDirectoryClient);
    }

    @Test
    void inactiveFacultyCannotBeAssignedToCourse() {
        Course course = new Course();
        course.setId(4L);
        course.setAcademicYear("2026-2027");
        course.setSemester(2);
        when(facultyDirectoryClient.getFacultyByEmail("faculty@example.edu")).thenReturn(Map.of("status", "INACTIVE"));
        when(courseRepository.findById(4L)).thenReturn(Optional.of(course));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () ->
                service.assignFacultyToCourse(new FacultyCourseAssignmentRequest("faculty@example.edu", 4L, null, null)));

        assertEquals(400, thrown.getStatusCode().value());
        assertEquals("Faculty account is inactive", thrown.getReason());
        org.mockito.Mockito.verifyNoInteractions(facultyAssignmentRepository);
    }

    @Test
    void activeFacultyCanBeAssignedToCourse() {
        Course course = new Course();
        course.setId(4L);
        course.setAcademicYear("2026-2027");
        course.setSemester(2);
        when(facultyDirectoryClient.getFacultyByEmail("faculty@example.edu")).thenReturn(Map.of("status", "ACTIVE"));
        when(courseRepository.findById(4L)).thenReturn(Optional.of(course));
        when(facultyAssignmentRepository.findByFacultyEmailIgnoreCaseAndCourseIdAndAcademicYearAndSemester(
                "faculty@example.edu", 4L, "2026-2027", 2)).thenReturn(Optional.empty());
        when(facultyAssignmentRepository.save(any(FacultyCourseAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FacultyCourseAssignment assignment = service.assignFacultyToCourse(
                new FacultyCourseAssignmentRequest("faculty@example.edu", 4L, null, null));

        assertEquals("faculty@example.edu", assignment.getFacultyEmail());
        assertEquals(4L, assignment.getCourseId());
        assertEquals("2026-2027", assignment.getAcademicYear());
        assertEquals(2, assignment.getSemester());
        verify(facultyAssignmentRepository).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void duplicateEnrollmentIsRejected() {
        Student student = new Student();
        student.setId(10L);
        student.setEmail("student@example.edu");
        Course course = new Course();
        course.setId(3L);
        course.setCourseName("Data Structures");
        course.setActive(true);
        course.setAcademicYear("2025-2026");
        course.setSemester(1);

        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(student));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 3L)).thenReturn(true);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () ->
                service.enrollStudent(new StudentEnrollmentRequest(3L, 7L), "student@example.edu"));

        assertEquals(409, thrown.getStatusCode().value());
        verifyNoInteractions(notificationService, facultyDirectoryClient, facultyAssignmentRepository);
    }

    @Test
    void enrollmentUsesAuthenticatedStudentAndActiveAssignmentAndNotifiesBothParties() {
        Student student = new Student();
        student.setId(10L);
        student.setName("Alex Student");
        student.setEmail("student@example.edu");
        student.setStatus("ACTIVE");
        Course course = new Course();
        course.setId(3L);
        course.setCourseCode("DS101");
        course.setCourseName("Data Structures");
        course.setDepartment("CSE");
        course.setCredits(4);
        course.setActive(true);
        course.setAcademicYear("2026-2027");
        course.setSemester(1);
        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setId(7L);
        assignment.setCourseId(3L);
        assignment.setFacultyEmail("faculty@example.edu");
        assignment.setAcademicYear("2026-2027");
        assignment.setSemester(1);
        assignment.setActive(true);
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(student));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 3L)).thenReturn(false);
        when(facultyAssignmentRepository.findByIdAndCourseIdAndActiveTrue(7L, 3L)).thenReturn(Optional.of(assignment));
        when(facultyDirectoryClient.getFacultyDirectoryByEmail("faculty@example.edu"))
                .thenReturn(new FacultyDirectoryDTO(8L, "Faculty Name", "faculty@example.edu", "CSE", "Professor", "ACTIVE"));
        when(enrollmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.enrollStudent(new StudentEnrollmentRequest(3L, 7L), "student@example.edu");

        assertEquals("Data Structures", result.courseName());
        assertEquals("Faculty Name", result.facultyName());
        assertEquals("faculty@example.edu", result.facultyEmail());
        assertEquals("2026-2027", result.academicYear());
        assertEquals(1, result.semester());
        verify(enrollmentRepository).saveAndFlush(any());
        verify(notificationService).notifyUser("student@example.edu", "STUDENT", "ACADEMICS", "Enrollment successful", "You are now enrolled in Data Structures with Faculty Name.");
        verify(notificationService).notifyUser("faculty@example.edu", "FACULTY", "ACADEMICS", "New student enrollment", "Alex Student enrolled in Data Structures.");
        org.mockito.Mockito.verify(studentRepository, org.mockito.Mockito.never()).save(any(Student.class));
    }

    @Test
    void inactiveCourseCannotBeEnrolledIn() {
        Student student = new Student(); student.setId(10L); student.setEmail("student@example.edu");
        Course course = new Course(); course.setId(3L); course.setActive(false);
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(student));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () ->
                service.enrollStudent(new StudentEnrollmentRequest(3L, 7L), "student@example.edu"));

        assertEquals(400, thrown.getStatusCode().value());
        assertEquals("This course is no longer available for enrollment", thrown.getReason());
        verifyNoInteractions(notificationService);
    }

    @Test
    void unrelatedFacultyAssignmentIsRejected() {
        Student student = new Student(); student.setId(10L); student.setEmail("student@example.edu");
        Course course = new Course(); course.setId(3L); course.setActive(true);
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(student));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 3L)).thenReturn(false);
        when(facultyAssignmentRepository.findByIdAndCourseIdAndActiveTrue(7L, 3L)).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () ->
                service.enrollStudent(new StudentEnrollmentRequest(3L, 7L), "student@example.edu"));

        assertEquals(400, thrown.getStatusCode().value());
        assertEquals("Selected faculty is not actively assigned to this course", thrown.getReason());
        verify(enrollmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(facultyDirectoryClient, notificationService);
    }

    @Test
    void studentCannotReadAnotherStudentsEnrollments() {
        Student other = new Student(); other.setId(10L); other.setEmail("other@example.edu");
        when(studentRepository.findById(10L)).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class, () -> service.getStudentEnrollments(10L, "student@example.edu", "STUDENT"));
        verifyNoInteractions(enrollmentRepository, facultyDirectoryClient);
    }

    @Test
    void facultyCannotReadStudentsOutsideTheirEnrollmentRoster() {
        Student student = new Student(); student.setId(10L); student.setEmail("student@example.edu");
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(10L)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> service.getStudentEnrollments(10L, "other-faculty@example.edu", "FACULTY"));
        verifyNoInteractions(facultyDirectoryClient);
    }

    @Test
    void availableCoursesAreFetchedThroughTheStudentAndCourseScopedBackendQuery() {
        Student student = new Student(); student.setId(10L); student.setEmail("student@example.edu");
        when(studentRepository.findByEmailIgnoreCase("student@example.edu")).thenReturn(Optional.of(student));
        when(courseRepository.findAvailableForStudent(10L)).thenReturn(List.of());

        assertEquals(List.of(), service.getAvailableCoursesForStudent("student@example.edu"));
        verify(courseRepository).findAvailableForStudent(10L);
    }

    @Test
    void facultyRosterOnlyIncludesActiveEnrollmentsOnCurrentlyAssignedCourses() {
        FacultyCourseAssignment activeAssignment = new FacultyCourseAssignment();
        activeAssignment.setCourseId(3L); activeAssignment.setActive(true);
        FacultyCourseAssignment inactiveAssignment = new FacultyCourseAssignment();
        inactiveAssignment.setCourseId(4L); inactiveAssignment.setActive(false);
        when(facultyAssignmentRepository.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc("faculty@example.edu"))
                .thenReturn(List.of(activeAssignment, inactiveAssignment));
        var assignedEnrollment = enrollment(10L, 3L);
        var unrelatedEnrollment = enrollment(20L, 4L);
        when(enrollmentRepository.findByFacultyEmailIgnoreCaseOrderByEnrolledAtDesc("faculty@example.edu"))
                .thenReturn(List.of(assignedEnrollment, unrelatedEnrollment));
        Student student = new Student(); student.setId(10L); student.setName("Assigned Student");
        when(studentRepository.findAllById(any())).thenReturn(List.of(student));

        List<Student> roster = service.getFacultyStudents("faculty@example.edu", null);

        assertEquals(List.of("Assigned Student"), roster.stream().map(Student::getName).toList());
        verify(studentRepository).findAllById(java.util.Set.of(10L));
    }

    private com.klef.soa.sdp.entity.StudentCourseEnrollment enrollment(Long studentId, Long courseId) {
        var enrollment = new com.klef.soa.sdp.entity.StudentCourseEnrollment();
        enrollment.setStudentId(studentId); enrollment.setCourseId(courseId);
        enrollment.setFacultyEmail("faculty@example.edu");
        enrollment.setStatus(com.klef.soa.sdp.entity.StudentCourseEnrollment.EnrollmentStatus.ACTIVE);
        return enrollment;
    }
}
