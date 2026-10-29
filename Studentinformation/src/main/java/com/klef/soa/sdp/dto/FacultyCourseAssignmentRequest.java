package com.klef.soa.sdp.dto;

public record FacultyCourseAssignmentRequest(
        String facultyEmail,
        Long courseId,
        String academicYear,
        Integer semester
) {
}
