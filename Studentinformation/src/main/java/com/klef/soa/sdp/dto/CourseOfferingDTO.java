package com.klef.soa.sdp.dto;

import java.util.List;

import com.klef.soa.sdp.entity.StudentCourseEnrollment.EnrollmentStatus;

public record CourseOfferingDTO(
        Long id,
        String courseCode,
        String courseName,
        String department,
        Integer credits,
        Integer semester,
        String academicYear,
        String description,
        boolean active,
        List<CourseFacultyDTO> faculty,
        EnrollmentStatus enrollmentStatus
) {
}
