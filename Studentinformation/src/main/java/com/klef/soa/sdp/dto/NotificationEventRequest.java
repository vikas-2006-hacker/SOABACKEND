package com.klef.soa.sdp.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationEventRequest(
        Long studentId,
        @NotBlank String eventType,
        String courseCode,
        String detail) { }
