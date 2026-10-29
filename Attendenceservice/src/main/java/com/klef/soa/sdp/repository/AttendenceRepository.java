package com.klef.soa.sdp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klef.soa.sdp.entity.Attendence;

@Repository
public interface AttendenceRepository extends JpaRepository<Attendence, Long> {

    List<Attendence> findByStudentId(Long studentId);

    List<Attendence> findByStudentIdAndCourseCode(Long studentId, String courseCode);

    List<Attendence> findByCourseCode(String courseCode);

    List<Attendence> findByDate(LocalDate date);

    List<Attendence> findByStudentIdAndDate(Long studentId, LocalDate date);

    List<Attendence> findByCourseCodeAndDate(String courseCode, LocalDate date);

    Optional<Attendence> findByStudentIdAndCourseCodeAndDate(Long studentId, String courseCode, LocalDate date);

    long countByStudentId(Long studentId);

    long countByStudentIdAndStatusIgnoreCase(Long studentId, String status);

    long countByStudentIdAndCourseCode(Long studentId, String courseCode);

    long countByStudentIdAndCourseCodeAndStatusIgnoreCase(Long studentId, String courseCode, String status);
}
