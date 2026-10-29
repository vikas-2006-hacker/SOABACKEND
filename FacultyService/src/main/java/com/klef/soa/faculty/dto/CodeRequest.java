package com.klef.soa.faculty.dto;
import jakarta.validation.constraints.*;
public record CodeRequest(@NotBlank String challengeId,@NotBlank @Pattern(regexp="[0-9]{6}") String code){}
