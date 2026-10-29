package com.klef.soa.sdp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseRequest(
        @NotBlank(message = "Course code is required")
        String courseCode,
        @NotBlank(message = "Course name is required")
        String courseName,
        @NotBlank(message = "Department is required")
        String department,
        @NotNull(message = "Semester is required")
        @Min(value = 1, message = "Semester must be at least 1")
        Integer semester,
        @NotBlank(message = "Academic year is required")
        String academicYear,
        @NotNull(message = "Credits are required")
        @Min(value = 1, message = "Credits must be at least 1")
        Integer credits,
        String description,
        Boolean active
) {
}
