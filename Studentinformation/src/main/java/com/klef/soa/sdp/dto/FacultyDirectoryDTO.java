package com.klef.soa.sdp.dto;

public record FacultyDirectoryDTO(
        Long id,
        String name,
        String email,
        String department,
        String designation,
        String status
) {
}
