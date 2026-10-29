package com.klef.soa.sdp.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.sdp.client.AttendanceSummaryClient;
import com.klef.soa.sdp.client.ResultClient;
import com.klef.soa.sdp.dto.AcademicInsightsDTO;
import com.klef.soa.sdp.dto.AttendanceSummaryDTO;
import com.klef.soa.sdp.dto.ResultRecordDTO;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;
import com.klef.soa.sdp.security.StudentAccessPolicy;

@Service
@Transactional(readOnly = true)
public class AcademicInsightService {
    private final StudentRepository students;
    private final AttendanceSummaryClient attendance;
    private final ResultClient results;
    private final StudentAccessPolicy accessPolicy;

    public AcademicInsightService(StudentRepository students, AttendanceSummaryClient attendance, ResultClient results,
            StudentAccessPolicy accessPolicy) {
        this.students = students;
        this.attendance = attendance;
        this.results = results;
        this.accessPolicy = accessPolicy;
    }

    public AcademicInsightsDTO forStudentEmail(String email) {
        Student student = students.findByEmail(email)
                .filter(candidate -> "STUDENT".equalsIgnoreCase(candidate.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        return forStudent(student);
    }

    public AcademicInsightsDTO forStudentId(Long studentId) {
        accessPolicy.checkStudentAccess(studentId);
        Student student = students.findByIdAndRoleIgnoreCase(studentId, "STUDENT")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        return forStudent(student);
    }

    private AcademicInsightsDTO forStudent(Student student) {
        List<String> unavailable = new ArrayList<>();
        AttendanceSummaryDTO attendanceSummary = null;
        List<ResultRecordDTO> resultRecords = null;

        try {
            attendanceSummary = attendance.getSummary(student.getId(), null);
        } catch (RuntimeException exception) {
            unavailable.add("attendance");
        }
        try {
            resultRecords = results.getResultsByStudentId(student.getId());
            if (resultRecords == null) resultRecords = List.of();
        } catch (RuntimeException exception) {
            unavailable.add("results");
        }
        List<ResultRecordDTO> published = resultRecords == null ? List.of()
                : resultRecords.stream().filter(Objects::nonNull)
                        .filter(result -> "PASS".equalsIgnoreCase(result.getResultStatus())
                                || "FAIL".equalsIgnoreCase(result.getResultStatus())).toList();
        List<AcademicInsightsDTO.Insight> insights = new ArrayList<>();
        AcademicInsightsDTO.Attendance attendanceInsight = toAttendance(attendanceSummary, insights);
        AcademicInsightsDTO.Performance performance = toPerformance(published, insights);
        return new AcademicInsightsDTO(student.getId(), student.getName(), student.getDepartment(),
                student.getSemester(), student.getStatus(), attendanceInsight, performance,
                List.copyOf(insights), List.copyOf(unavailable));
    }

    private AcademicInsightsDTO.Attendance toAttendance(AttendanceSummaryDTO summary,
            List<AcademicInsightsDTO.Insight> insights) {
        if (summary == null) return null;
        String status = summary.getAttendanceStatus() == null
                ? (summary.getTotalClasses() == 0 ? "INSUFFICIENT_DATA" : "UNAVAILABLE")
                : summary.getAttendanceStatus();
        String risk = summary.getAttendanceRisk() == null ? "UNKNOWN" : summary.getAttendanceRisk();
        if (summary.getTotalClasses() > 0) {
            String detail = String.format(java.util.Locale.ROOT,
                    "You attended %d of %d recorded classes (%.2f%%).",
                    summary.getClassesAttended(), summary.getTotalClasses(), summary.getAttendancePercentage());
            insights.add(new AcademicInsightsDTO.Insight("ATTENDANCE", attendanceTitle(status), detail,
                    switch (status) {
                        case "CRITICAL" -> "HIGH";
                        case "WARNING" -> "MODERATE";
                        case "EXCELLENT", "GOOD" -> "POSITIVE";
                        default -> "NEUTRAL";
                    }));
        }
        return new AcademicInsightsDTO.Attendance(summary.getTotalClasses(), summary.getClassesAttended(),
                summary.getClassesAbsent(), summary.getClassesExcused(), summary.getAttendancePercentage(),
                summary.isExamEligible(), status, risk, summary.getRequiredAttendanceThreshold());
    }

    private AcademicInsightsDTO.Performance toPerformance(List<ResultRecordDTO> published,
            List<AcademicInsightsDTO.Insight> insights) {
        Comparator<ResultRecordDTO> strongestOrder = Comparator
                .comparingDouble(ResultRecordDTO::getGradePoints)
                .thenComparingDouble(ResultRecordDTO::getTotalMarks)
                .thenComparing(ResultRecordDTO::getCourseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        Comparator<ResultRecordDTO> latestOrder = Comparator
                .comparingInt(ResultRecordDTO::getSemester)
                .thenComparing(ResultRecordDTO::getAcademicYear, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ResultRecordDTO::getId, Comparator.nullsLast(Long::compareTo));
        ResultRecordDTO strongest = published.stream().max(strongestOrder).orElse(null);
        ResultRecordDTO focus = published.stream().min(strongestOrder).orElse(null);
        ResultRecordDTO latest = published.stream().max(latestOrder).orElse(null);
        Double average = published.isEmpty() ? null
                : published.stream().mapToDouble(ResultRecordDTO::getTotalMarks).average().orElse(Double.NaN);
        if (strongest != null) {
            insights.add(new AcademicInsightsDTO.Insight("RESULTS", "Strongest recorded subject",
                    String.format(java.util.Locale.ROOT, "%s has the highest recorded grade points (%.1f) across %d published results.",
                            safe(strongest.getCourseName()), strongest.getGradePoints(), published.size()), "POSITIVE"));
        }
        if (published.isEmpty()) return null;
        int creditsRegistered = published.stream().mapToInt(ResultRecordDTO::getCredits).sum();
        int creditsEarned = published.stream()
                .filter(result -> !"FAIL".equalsIgnoreCase(result.getResultStatus()))
                .mapToInt(ResultRecordDTO::getCredits).sum();
        double weightedGradePoints = published.stream()
                .mapToDouble(result -> result.getGradePoints() * result.getCredits()).sum();
        double cumulativeGpa = creditsRegistered == 0 ? 0
                : java.math.BigDecimal.valueOf(weightedGradePoints / creditsRegistered)
                        .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
        boolean hasFailure = published.stream().anyMatch(result -> "FAIL".equalsIgnoreCase(result.getResultStatus()));
        String standing = hasFailure ? "ACADEMIC PROBATION"
                : cumulativeGpa >= 9.0 ? "FIRST CLASS WITH DISTINCTION"
                : cumulativeGpa >= 7.5 ? "FIRST CLASS"
                : cumulativeGpa >= 6.0 ? "SECOND CLASS" : "PASS";
        return new AcademicInsightsDTO.Performance(
                cumulativeGpa,
                standing,
                published.size(),
                average,
                creditsRegistered,
                creditsEarned,
                snapshot(strongest), snapshot(focus), snapshot(latest));
    }

    private AcademicInsightsDTO.ResultSnapshot snapshot(ResultRecordDTO result) {
        if (result == null) return null;
        return new AcademicInsightsDTO.ResultSnapshot(result.getCourseName(), result.getCourseCode(),
                result.getSemester(), result.getAcademicYear(), result.getTotalMarks(), result.getGrade(),
                result.getGradePoints(), result.getCredits(), result.getResultStatus());
    }

    private String attendanceTitle(String status) {
        return switch (status) {
            case "EXCELLENT" -> "Excellent attendance";
            case "GOOD" -> "Attendance is on track";
            case "WARNING" -> "Attendance needs attention";
            case "CRITICAL" -> "Attendance is critically low";
            default -> "Attendance status unavailable";
        };
    }

    private String safe(String value) { return value == null || value.isBlank() ? "This subject" : value; }
}
