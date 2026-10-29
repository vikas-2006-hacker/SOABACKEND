package com.klef.soa.sdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.soa.sdp.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCodeIgnoreCase(String courseCode);
    List<Course> findByActiveTrueOrderByCourseNameAsc();
    List<Course> findAllByOrderByCourseNameAsc();

    @Query("select distinct c from Course c join FacultyCourseAssignment a on a.courseId = c.id "
            + "where c.active = true and a.active = true "
            + "and not exists (select e.id from StudentCourseEnrollment e where e.studentId = :studentId and e.courseId = c.id) "
            + "order by c.courseName asc")
    List<Course> findAvailableForStudent(@Param("studentId") Long studentId);
}
