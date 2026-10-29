package com.klef.soa.sdp.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Single source of truth for attendance status, risk, and exam eligibility. */
@Component
public class AttendanceThresholdPolicy {
    private final double excellentThreshold;
    private final double goodThreshold;
    private final double warningThreshold;

    public AttendanceThresholdPolicy(
            @Value("${attendance.excellent-threshold:90}") double excellentThreshold,
            @Value("${attendance.good-threshold:80}") double goodThreshold,
            @Value("${attendance.warning-threshold:75}") double warningThreshold) {
        this.excellentThreshold = excellentThreshold;
        this.goodThreshold = goodThreshold;
        this.warningThreshold = warningThreshold;
    }

    @PostConstruct
    void validateThresholds() {
        if (!Double.isFinite(excellentThreshold) || !Double.isFinite(goodThreshold)
                || !Double.isFinite(warningThreshold) || warningThreshold < 0
                || goodThreshold <= warningThreshold || excellentThreshold <= goodThreshold
                || excellentThreshold > 100) {
            throw new IllegalStateException("Attendance thresholds must satisfy 0 <= warning < good < excellent <= 100.");
        }
    }

    public String status(long totalClasses, double percentage) {
        if (totalClasses == 0) return "INSUFFICIENT_DATA";
        if (percentage >= excellentThreshold) return "EXCELLENT";
        if (percentage >= goodThreshold) return "GOOD";
        if (percentage >= warningThreshold) return "WARNING";
        return "CRITICAL";
    }

    public String risk(long totalClasses, double percentage) {
        return switch (status(totalClasses, percentage)) {
            case "EXCELLENT", "GOOD" -> "LOW";
            case "WARNING" -> "MODERATE";
            case "CRITICAL" -> "HIGH";
            default -> "UNKNOWN";
        };
    }

    public boolean examEligible(long totalClasses, double percentage) {
        return totalClasses > 0 && percentage >= warningThreshold;
    }

    public double getExcellentThreshold() { return excellentThreshold; }
    public double getGoodThreshold() { return goodThreshold; }
    public double getWarningThreshold() { return warningThreshold; }
}
