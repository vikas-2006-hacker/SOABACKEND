package com.klef.soa.sdp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klef.soa.sdp.dto.AcademicInsightsDTO;
import com.klef.soa.sdp.service.AcademicInsightService;

@RestController
@RequestMapping("/api/insights")
public class AcademicInsightController {
    private final AcademicInsightService insights;

    public AcademicInsightController(AcademicInsightService insights) {
        this.insights = insights;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public AcademicInsightsDTO getMyInsights(Authentication authentication) {
        return insights.forStudentEmail(authentication.getName());
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','STUDENT')")
    public AcademicInsightsDTO getStudentInsights(@PathVariable Long studentId) {
        return insights.forStudentId(studentId);
    }
}
