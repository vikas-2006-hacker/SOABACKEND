package com.klef.soa.sdp.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;

import com.klef.soa.sdp.dto.CourseRequest;
import com.klef.soa.sdp.dto.CourseFacultyDTO;
import com.klef.soa.sdp.dto.CourseOfferingDTO;
import com.klef.soa.sdp.dto.FacultyDirectoryDTO;
import com.klef.soa.sdp.dto.FacultyCourseAssignmentRequest;
import com.klef.soa.sdp.dto.StudentCourseEnrollmentDTO;
import com.klef.soa.sdp.dto.StudentEnrollmentRequest;
import com.klef.soa.sdp.client.FacultyDirectoryClient;
import com.klef.soa.sdp.entity.Course;
import com.klef.soa.sdp.entity.FacultyCourseAssignment;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.entity.StudentCourseEnrollment;
import com.klef.soa.sdp.repository.CourseRepository;
import com.klef.soa.sdp.repository.FacultyCourseAssignmentRepository;
import com.klef.soa.sdp.repository.StudentCourseEnrollmentRepository;
import com.klef.soa.sdp.repository.StudentRepository;

@Service
@Transactional
public class AcademicStructureService {

    private final CourseRepository courseRepository;
    private final FacultyCourseAssignmentRepository facultyCourseAssignmentRepository;
    private final StudentCourseEnrollmentRepository studentCourseEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final FacultyDirectoryClient facultyDirectoryClient;

    public AcademicStructureService(
            CourseRepository courseRepository,
            FacultyCourseAssignmentRepository facultyCourseAssignmentRepository,
            StudentCourseEnrollmentRepository studentCourseEnrollmentRepository,
            StudentRepository studentRepository,
            NotificationService notificationService,
            FacultyDirectoryClient facultyDirectoryClient
    ) {
        this.courseRepository = courseRepository;
        this.facultyCourseAssignmentRepository = facultyCourseAssignmentRepository;
        this.studentCourseEnrollmentRepository = studentCourseEnrollmentRepository;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
        this.facultyDirectoryClient = facultyDirectoryClient;
    }

    @Transactional(readOnly = true)
    public Student getCurrentStudent(String email) {
        return studentRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student record not found for the authenticated user"));
    }

    @Transactional(readOnly = true)
    public List<Course> listCourses() {
        return courseRepository.findByActiveTrueOrderByCourseNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Course> listAllCourses() {
        return courseRepository.findAllByOrderByCourseNameAsc();
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingDTO> getAvailableCoursesForStudent(String studentEmail) {
        Student student = getCurrentStudent(studentEmail);
        Map<String, FacultyDirectoryDTO> facultyCache = new HashMap<>();
        List<CourseOfferingDTO> offerings = new ArrayList<>();
        for (Course course : courseRepository.findAvailableForStudent(student.getId())) {
            List<CourseFacultyDTO> faculty = getAssignedFaculty(course, facultyCache);
            if (!faculty.isEmpty()) offerings.add(toOffering(course, faculty, null));
        }
        return offerings;
    }

    @Transactional(readOnly = true)
    public CourseOfferingDTO getStudentCourseDetails(Long courseId, String studentEmail) {
        Student student = getCurrentStudent(studentEmail);
        Course course = getCourse(courseId);
        if (!course.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This course is not available for enrollment");
        }
        List<CourseFacultyDTO> faculty = getAssignedFaculty(course, new HashMap<>());
        if (faculty.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This course does not have an active faculty assignment");
        }
        StudentCourseEnrollment.EnrollmentStatus status = studentCourseEnrollmentRepository
                .findByStudentIdAndCourseIdOrderByEnrolledAtDesc(student.getId(), courseId)
                .stream()
                .map(StudentCourseEnrollment::getStatus)
                .findFirst()
                .orElse(null);
        return toOffering(course, faculty, status);
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    public Course createCourse(CourseRequest request) {
        String courseCode = normalizeText(request.courseCode());
        if (courseRepository.findByCourseCodeIgnoreCase(courseCode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course code already exists");
        }
        Course course = new Course();
        course.setCourseCode(courseCode);
        course.setCourseName(normalizeText(request.courseName()));
        course.setDepartment(normalizeText(request.department()));
        course.setSemester(request.semester() == null ? 1 : request.semester());
        course.setAcademicYear(request.academicYear() == null || request.academicYear().isBlank() ? "2025-2026" : request.academicYear().trim());
        course.setCredits(request.credits() == null ? 3 : request.credits());
        course.setDescription(request.description() == null ? "" : request.description().trim());
        course.setActive(request.active() == null || request.active());
        return courseRepository.save(course);
    }

    public Course updateCourse(Long courseId, CourseRequest request) {
        Course course = getCourse(courseId);
        if (request.courseCode() != null && !request.courseCode().isBlank()) {
            String newCode = request.courseCode().trim();
            Optional<Course> existing = courseRepository.findByCourseCodeIgnoreCase(newCode);
            if (existing.isPresent() && !existing.get().getId().equals(courseId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course code already exists");
            }
            course.setCourseCode(newCode);
        }
        if (request.courseName() != null && !request.courseName().isBlank()) {
            course.setCourseName(request.courseName().trim());
        }
        if (request.department() != null && !request.department().isBlank()) {
            course.setDepartment(request.department().trim());
        }
        if (request.semester() != null) {
            course.setSemester(request.semester());
        }
        if (request.academicYear() != null && !request.academicYear().isBlank()) {
            course.setAcademicYear(request.academicYear().trim());
        }
        if (request.credits() != null) {
            course.setCredits(request.credits());
        }
        if (request.description() != null) {
            course.setDescription(request.description().trim());
        }
        if (request.active() != null) {
            course.setActive(request.active());
        }
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<FacultyCourseAssignment> getCourseFaculty(Long courseId) {
        return facultyCourseAssignmentRepository.findByCourseIdAndActiveTrueOrderBySemesterAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Course getFacultyCourse(Long courseId, String facultyEmail) {
        boolean assigned = facultyCourseAssignmentRepository.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc(facultyEmail)
                .stream().anyMatch(assignment -> assignment.isActive() && assignment.getCourseId().equals(courseId));
        if (!assigned) throw new AccessDeniedException("Faculty is not assigned to this course");
        return getCourse(courseId);
    }

    @Transactional(readOnly = true)
    public List<FacultyCourseAssignment> getCourseFaculty(Long courseId, String facultyEmail) {
        getFacultyCourse(courseId, facultyEmail);
        return getCourseFaculty(courseId);
    }

    public FacultyCourseAssignment assignFacultyToCourse(FacultyCourseAssignmentRequest request) {
        String facultyEmail = normalizeEmail(request.facultyEmail());
        Long courseId = request.courseId();
        Course course = getCourse(courseId);
        validateFacultyIsActive(facultyEmail);
        String academicYear = request.academicYear() == null || request.academicYear().isBlank()
                ? course.getAcademicYear() == null ? "2025-2026" : course.getAcademicYear()
                : request.academicYear().trim();
        Integer semester = request.semester() == null ? course.getSemester() : request.semester();

        Optional<FacultyCourseAssignment> existing = facultyCourseAssignmentRepository
                .findByFacultyEmailIgnoreCaseAndCourseIdAndAcademicYearAndSemester(facultyEmail, courseId, academicYear, semester);
        if (existing.isPresent()) {
            FacultyCourseAssignment assignment = existing.get();
            assignment.setActive(true);
            return facultyCourseAssignmentRepository.save(assignment);
        }

        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setFacultyEmail(facultyEmail);
        assignment.setCourseId(courseId);
        assignment.setAcademicYear(academicYear);
        assignment.setSemester(semester);
        assignment.setActive(true);
        return facultyCourseAssignmentRepository.save(assignment);
    }

    private void validateFacultyIsActive(String facultyEmail) {
        Map<String, Object> faculty;
        try {
            faculty = facultyDirectoryClient.getFacultyByEmail(facultyEmail);
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty account does not exist");
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Faculty Service is unavailable to validate the assignment");
        }
        if (faculty == null || !"ACTIVE".equalsIgnoreCase(String.valueOf(faculty.get("status")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty account is inactive");
        }
    }

    @Transactional(readOnly = true)
    public List<Course> getFacultyCourses(String facultyEmail) {
        List<Long> courseIds = facultyCourseAssignmentRepository.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc(facultyEmail)
                .stream()
                .filter(FacultyCourseAssignment::isActive)
                .map(FacultyCourseAssignment::getCourseId)
                .distinct()
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<Course> courses = new ArrayList<>(courseRepository.findAllById(courseIds));
        courses.removeIf(course -> !course.isActive());
        courses.sort(Comparator.comparing(Course::getCourseName, String.CASE_INSENSITIVE_ORDER));
        return courses;
    }

    @Transactional(readOnly = true)
    public List<Student> getFacultyStudents(String facultyEmail, Long courseId) {
        Set<Long> activeCourseIds = facultyCourseAssignmentRepository.findByFacultyEmailIgnoreCaseOrderByCourseIdAsc(facultyEmail)
                .stream()
                .filter(FacultyCourseAssignment::isActive)
                .map(FacultyCourseAssignment::getCourseId)
                .filter(id -> courseId == null || courseId.equals(id))
                .collect(java.util.stream.Collectors.toSet());
        if (activeCourseIds.isEmpty()) return List.of();
        List<StudentCourseEnrollment> enrollments =
                courseId == null
                        ? studentCourseEnrollmentRepository.findByFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(facultyEmail)
                        : studentCourseEnrollmentRepository.findByFacultyEmailIgnoreCaseAndCourseIdOrderByEnrolledAtDesc(facultyEmail, courseId);
        Set<Long> studentIds = new LinkedHashSet<>();
        for (StudentCourseEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() == StudentCourseEnrollment.EnrollmentStatus.ACTIVE
                    && activeCourseIds.contains(enrollment.getCourseId())) {
                studentIds.add(enrollment.getStudentId());
            }
        }
        if (studentIds.isEmpty()) {
            return List.of();
        }
        List<Student> students = new ArrayList<>(studentRepository.findAllById(studentIds));
        students.sort(Comparator.comparing(Student::getName, String.CASE_INSENSITIVE_ORDER));
        return students;
    }

    public StudentCourseEnrollmentDTO enrollStudent(StudentEnrollmentRequest request, String actorEmail) {
        if (request == null || request.courseId() == null || request.facultyAssignmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a course and an assigned faculty member");
        }
        Student student = getCurrentStudent(actorEmail);
        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only active student accounts can enroll in courses");
        }
        Course course = getCourse(request.courseId());
        if (!course.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This course is no longer available for enrollment");
        }
        if (studentCourseEnrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You're already enrolled in this course.");
        }
        FacultyCourseAssignment assignment = facultyCourseAssignmentRepository
                .findByIdAndCourseIdAndActiveTrue(request.facultyAssignmentId(), course.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected faculty is not actively assigned to this course"));
        String facultyEmail = normalizeEmail(assignment.getFacultyEmail());
        FacultyDirectoryDTO faculty = getFacultyDirectory(facultyEmail, new HashMap<>());
        if (faculty == null || !"ACTIVE".equalsIgnoreCase(faculty.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected faculty account is not active");
        }
        String academicYear = assignment.getAcademicYear() == null || assignment.getAcademicYear().isBlank()
                ? course.getAcademicYear() : assignment.getAcademicYear();
        Integer semester = assignment.getSemester() == null ? course.getSemester() : assignment.getSemester();

        StudentCourseEnrollment enrollment = new StudentCourseEnrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setCourseId(course.getId());
        enrollment.setFacultyEmail(facultyEmail);
        enrollment.setAcademicYear(academicYear);
        enrollment.setSemester(semester);
        enrollment.setStatus(StudentCourseEnrollment.EnrollmentStatus.ACTIVE);
        StudentCourseEnrollment saved;
        try {
            saved = studentCourseEnrollmentRepository.saveAndFlush(enrollment);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You're already enrolled in this course.");
        }

        notificationService.notifyUser(
                student.getEmail(),
                "STUDENT",
                "ACADEMICS",
                "Enrollment successful",
                "You are now enrolled in " + course.getCourseName() + " with " + faculty.name() + "."
        );
        notificationService.notifyUser(
                facultyEmail,
                "FACULTY",
                "ACADEMICS",
                "New student enrollment",
                student.getName() + " enrolled in " + course.getCourseName() + "."
        );
        return toEnrollmentView(saved, course, faculty);
    }

    @Transactional(readOnly = true)
    public List<StudentCourseEnrollmentDTO> getStudentEnrollments(Long studentId, String requesterEmail, String requesterRole) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if ("STUDENT".equalsIgnoreCase(requesterRole) && !student.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("Access denied for this student");
        }
        List<StudentCourseEnrollment> enrollments = studentCourseEnrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(studentId);
        if ("FACULTY".equalsIgnoreCase(requesterRole)) {
            enrollments = enrollments.stream()
                    .filter(enrollment -> enrollment.getStatus() == StudentCourseEnrollment.EnrollmentStatus.ACTIVE)
                    .filter(enrollment -> enrollment.getFacultyEmail().equalsIgnoreCase(requesterEmail))
                    .toList();
            if (enrollments.isEmpty()) {
                throw new AccessDeniedException("Faculty is not assigned to this student");
            }
        }
        Map<String, FacultyDirectoryDTO> facultyCache = new HashMap<>();
        Map<Long, Course> courseCache = new HashMap<>();
        return enrollments.stream()
                .sorted(Comparator.comparing(StudentCourseEnrollment::getEnrolledAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(enrollment -> {
                    Course course = courseCache.computeIfAbsent(enrollment.getCourseId(), this::getCourse);
                    FacultyDirectoryDTO faculty = getFacultyDirectory(enrollment.getFacultyEmail(), facultyCache);
                    return toEnrollmentView(enrollment, course, faculty);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentCourseEnrollmentDTO> getCurrentStudentEnrollments(String studentEmail) {
        Student student = getCurrentStudent(studentEmail);
        return getStudentEnrollments(student.getId(), studentEmail, "STUDENT");
    }

    @Transactional(readOnly = true)
    public boolean facultyHasActiveEnrollment(Long studentId, String facultyEmail) {
        return studentCourseEnrollmentRepository.existsByStudentIdAndFacultyEmailIgnoreCaseAndStatus(
                studentId, facultyEmail, StudentCourseEnrollment.EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean facultyHasActiveEnrollmentInCourse(Long studentId, String facultyEmail, String courseCode) {
        if (courseCode == null || courseCode.isBlank()) return false;
        return courseRepository.findByCourseCodeIgnoreCase(courseCode.trim())
                .map(course -> studentCourseEnrollmentRepository.existsByStudentIdAndCourseIdAndFacultyEmailIgnoreCaseAndStatus(
                        studentId, course.getId(), facultyEmail, StudentCourseEnrollment.EnrollmentStatus.ACTIVE))
                .orElse(false);
    }

    private List<CourseFacultyDTO> getAssignedFaculty(Course course, Map<String, FacultyDirectoryDTO> facultyCache) {
        return facultyCourseAssignmentRepository.findByCourseIdAndActiveTrueOrderBySemesterAsc(course.getId()).stream()
                .map(assignment -> {
                    FacultyDirectoryDTO faculty = getFacultyDirectory(assignment.getFacultyEmail(), facultyCache);
                    if (faculty == null || !"ACTIVE".equalsIgnoreCase(faculty.status())) return null;
                    return new CourseFacultyDTO(
                            assignment.getId(),
                            faculty.name(),
                            faculty.email(),
                            faculty.department(),
                            faculty.designation(),
                            assignment.getSemester(),
                            assignment.getAcademicYear());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private FacultyDirectoryDTO getFacultyDirectory(String email, Map<String, FacultyDirectoryDTO> facultyCache) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank()) return null;
        FacultyDirectoryDTO cached = facultyCache.get(normalized);
        if (cached != null) return cached;
        try {
            FacultyDirectoryDTO faculty = facultyDirectoryClient.getFacultyDirectoryByEmail(normalized);
            facultyCache.put(normalized, faculty);
            return faculty;
        } catch (FeignException.NotFound exception) {
            FacultyDirectoryDTO missing = new FacultyDirectoryDTO(null, normalized, normalized, null, null, "INACTIVE");
            facultyCache.put(normalized, missing);
            return missing;
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Faculty Service is unavailable to load course faculty");
        }
    }

    private CourseOfferingDTO toOffering(Course course, List<CourseFacultyDTO> faculty,
            StudentCourseEnrollment.EnrollmentStatus enrollmentStatus) {
        return new CourseOfferingDTO(course.getId(), course.getCourseCode(), course.getCourseName(),
                course.getDepartment(), course.getCredits(), course.getSemester(), course.getAcademicYear(),
                course.getDescription(), course.isActive(), faculty, enrollmentStatus);
    }

    private StudentCourseEnrollmentDTO toEnrollmentView(StudentCourseEnrollment enrollment, Course course,
            FacultyDirectoryDTO faculty) {
        String facultyName = faculty == null || faculty.name() == null || faculty.name().isBlank()
                ? enrollment.getFacultyEmail() : faculty.name();
        return new StudentCourseEnrollmentDTO(
                enrollment.getId(),
                course.getId(),
                course.getCourseName(),
                course.getCourseCode(),
                course.getDepartment(),
                course.getCredits(),
                course.getSemester(),
                course.getAcademicYear(),
                course.getDescription(),
                facultyName,
                enrollment.getFacultyEmail(),
                enrollment.getAcademicYear(),
                enrollment.getSemester(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt());
    }

    public List<StudentCourseEnrollment> getFacultyEnrollments(String facultyEmail) {
        return studentCourseEnrollmentRepository.findByFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(facultyEmail)
                .stream()
                .filter(enrollment -> enrollment.getStatus() == StudentCourseEnrollment.EnrollmentStatus.ACTIVE)
                .toList();
    }

    public List<StudentCourseEnrollment> listAllEnrollments() {
        return studentCourseEnrollmentRepository.findAll();
    }

    public void deleteEnrollment(Long enrollmentId, String actorEmail, String actorRole) {
        StudentCourseEnrollment enrollment = studentCourseEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

        if ("STUDENT".equalsIgnoreCase(actorRole)) {
            Student student = studentRepository.findById(enrollment.getStudentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
            if (!student.getEmail().equalsIgnoreCase(actorEmail)) {
                throw new AccessDeniedException("Access denied for this enrollment");
            }
        }
        if ("FACULTY".equalsIgnoreCase(actorRole)
                && !enrollment.getFacultyEmail().equalsIgnoreCase(actorEmail)) {
            throw new AccessDeniedException("Faculty cannot manage unrelated enrollments");
        }

        enrollment.setStatus(StudentCourseEnrollment.EnrollmentStatus.DROPPED);
        studentCourseEnrollmentRepository.save(enrollment);

        Student student = studentRepository.findById(enrollment.getStudentId()).orElse(null);
        if (student != null) {
            notificationService.notifyUser(
                    student.getEmail(),
                    "STUDENT",
                    "ACADEMICS",
                    "Enrollment updated",
                    "Your enrollment was removed."
            );
        }
        notificationService.notifyUser(
                enrollment.getFacultyEmail(),
                "FACULTY",
                "ACADEMICS",
                "Enrollment removed",
                "A student's enrollment was removed from your course."
        );
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
