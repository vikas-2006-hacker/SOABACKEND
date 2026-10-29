package com.klef.soa.faculty.dto;

import com.klef.soa.faculty.entity.Faculty;

/** Public faculty details used when presenting assigned course offerings. */
public record FacultyDirectoryResponse(
        Long id,
        String name,
        String email,
        String department,
        String designation,
        String status
) {
    public static FacultyDirectoryResponse from(Faculty faculty) {
        return new FacultyDirectoryResponse(
                faculty.getId(),
                faculty.getName(),
                faculty.getEmail(),
                faculty.getDepartment(),
                faculty.getDesignation(),
                faculty.getStatus()
        );
    }
}
