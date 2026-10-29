package com.klef.soa.sdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.soa.sdp.entity.StudentCourseEnrollment;

public interface StudentCourseEnrollmentRepository extends JpaRepository<StudentCourseEnrollment, Long> {
    List<StudentCourseEnrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId);
    List<StudentCourseEnrollment> findByFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(String facultyEmail);
    List<StudentCourseEnrollment> findByFacultyEmailIgnoreCaseAndCourseIdOrderByEnrolledAtDesc(String facultyEmail, Long courseId);
    List<StudentCourseEnrollment> findByCourseIdOrderByEnrolledAtDesc(Long courseId);
    List<StudentCourseEnrollment> findByStudentIdAndStatusOrderByEnrolledAtDesc(Long studentId, StudentCourseEnrollment.EnrollmentStatus status);
    List<StudentCourseEnrollment> findByStudentIdAndFacultyEmailIgnoreCaseOrderByEnrolledAtDesc(Long studentId, String facultyEmail);
    List<StudentCourseEnrollment> findByStudentIdAndCourseIdOrderByEnrolledAtDesc(Long studentId, Long courseId);
    Optional<StudentCourseEnrollment> findByStudentIdAndCourseIdAndAcademicYearAndSemester(Long studentId, Long courseId, String academicYear, Integer semester);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndFacultyEmailIgnoreCaseAndStatus(Long studentId, String facultyEmail, StudentCourseEnrollment.EnrollmentStatus status);
    boolean existsByStudentIdAndCourseIdAndFacultyEmailIgnoreCaseAndStatus(Long studentId, Long courseId, String facultyEmail, StudentCourseEnrollment.EnrollmentStatus status);
    boolean existsByStudentIdAndCourseIdAndAcademicYearAndSemesterAndStatus(Long studentId, Long courseId, String academicYear, Integer semester, StudentCourseEnrollment.EnrollmentStatus status);
    boolean existsByStudentIdAndCourseIdAndAcademicYearAndSemester(Long studentId, Long courseId, String academicYear, Integer semester);
}
