package com.klef.soa.faculty.dto;
import jakarta.validation.constraints.*;
public record FacultyLoginRequest(@NotBlank @Email String email,@NotBlank String password){}
