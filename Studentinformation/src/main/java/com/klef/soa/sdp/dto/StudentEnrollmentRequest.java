package com.klef.soa.sdp.dto;

import jakarta.validation.constraints.NotNull;

public record StudentEnrollmentRequest(
        @NotNull(message = "Course is required") Long courseId,
        @NotNull(message = "Select an assigned faculty member") Long facultyAssignmentId
) {
}
