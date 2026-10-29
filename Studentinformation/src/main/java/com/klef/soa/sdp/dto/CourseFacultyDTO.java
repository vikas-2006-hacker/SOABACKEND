package com.klef.soa.sdp.dto;

public record CourseFacultyDTO(
        Long assignmentId,
        String name,
        String email,
        String department,
        String designation,
        Integer semester,
        String academicYear
) {
}
