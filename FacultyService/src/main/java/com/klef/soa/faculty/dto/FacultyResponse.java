package com.klef.soa.faculty.dto;
import java.time.LocalDateTime;
import com.klef.soa.faculty.entity.Faculty;
public record FacultyResponse(Long id,String name,String email,String employeeId,String department,String designation,String phone,String status,boolean mfaEnabled,LocalDateTime createdAt,LocalDateTime updatedAt) {
 public static FacultyResponse from(Faculty f){return new FacultyResponse(f.getId(),f.getName(),f.getEmail(),f.getEmployeeId(),f.getDepartment(),f.getDesignation(),f.getPhone(),f.getStatus(),f.isMfaEnabled(),f.getCreatedAt(),f.getUpdatedAt());}
}
