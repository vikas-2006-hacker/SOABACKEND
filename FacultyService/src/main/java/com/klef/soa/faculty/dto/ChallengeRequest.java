package com.klef.soa.faculty.dto;
import jakarta.validation.constraints.NotBlank;
public record ChallengeRequest(@NotBlank String challengeId){}
