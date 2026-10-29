package com.klef.soa.sdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.soa.sdp.entity.FacultyCourseAssignment;

public interface FacultyCourseAssignmentRepository extends JpaRepository<FacultyCourseAssignment, Long> {
    List<FacultyCourseAssignment> findByFacultyEmailIgnoreCaseOrderByCourseIdAsc(String facultyEmail);
    List<FacultyCourseAssignment> findByCourseIdAndActiveTrueOrderBySemesterAsc(Long courseId);
    Optional<FacultyCourseAssignment> findByIdAndCourseIdAndActiveTrue(Long id, Long courseId);
    List<FacultyCourseAssignment> findByCourseIdOrderBySemesterAsc(Long courseId);
    Optional<FacultyCourseAssignment> findByFacultyEmailIgnoreCaseAndCourseIdAndAcademicYearAndSemester(String facultyEmail, Long courseId, String academicYear, Integer semester);
    Optional<FacultyCourseAssignment> findByFacultyEmailIgnoreCaseAndCourseId(String facultyEmail, Long courseId);
}
