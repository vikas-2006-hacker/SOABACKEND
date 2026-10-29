package com.klef.soa.sdp.dto;

import java.time.LocalDateTime;

import com.klef.soa.sdp.entity.StudentCourseEnrollment.EnrollmentStatus;

public record StudentCourseEnrollmentDTO(
        Long id,
        Long courseId,
        String courseName,
        String courseCode,
        String department,
        Integer credits,
        Integer courseSemester,
        String courseAcademicYear,
        String description,
        String facultyName,
        String facultyEmail,
        String academicYear,
        Integer semester,
        EnrollmentStatus status,
        LocalDateTime enrolledAt
) {
}
