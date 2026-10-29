package com.klef.soa.sdp.dto;

import java.util.List;

/** Whitelisted academic facts and deterministic observations; contains no credentials or private auth data. */
public record AcademicInsightsDTO(
        Long studentId,
        String studentName,
        String department,
        int semester,
        String studentStatus,
        Attendance attendance,
        Performance performance,
        List<Insight> insights,
        List<String> unavailableSources) {

    public record Attendance(long totalClasses, long classesAttended, long classesAbsent,
            long classesExcused, double percentage, boolean examEligible,
            String status, String risk, double requiredThreshold) { }

    public record Performance(Double cumulativeGpa, String standing, Integer resultsCount,
            Double averageMarks, Integer creditsRegistered, Integer creditsEarned,
            ResultSnapshot strongestSubject, ResultSnapshot focusSubject, ResultSnapshot latestResult) { }

    public record ResultSnapshot(String courseName, String courseCode, int semester,
            String academicYear, double marks, String grade, double gradePoints,
            int credits, String resultStatus) { }

    public record Insight(String category, String title, String detail, String severity) { }
}
