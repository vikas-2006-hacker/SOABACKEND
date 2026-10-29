package com.klef.soa.sdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klef.soa.sdp.entity.Result;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentId(Long studentId);

    List<Result> findByStudentIdAndSemester(Long studentId, int semester);

    List<Result> findByCourseCode(String courseCode);

    List<Result> findByResultStatus(String resultStatus);

    Optional<Result> findByStudentIdAndCourseCodeAndSemester(Long studentId, String courseCode, int semester);
}
