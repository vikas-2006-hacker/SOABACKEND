package com.klef.soa.faculty.dto;
import jakarta.validation.constraints.*;
public record UpdateFacultyRequest(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=254) String email,
 @NotBlank @Size(max=32) String employeeId, @NotBlank @Size(max=100) String department,
 @Size(max=100) String designation, @Size(max=32) String phone) {}
