package com.klef.soa.sdp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.klef.soa.sdp.client.AttendanceSummaryClient;
import com.klef.soa.sdp.client.ResultClient;
import com.klef.soa.sdp.dto.AcademicInsightsDTO;
import com.klef.soa.sdp.dto.AttendanceSummaryDTO;
import com.klef.soa.sdp.dto.ResultRecordDTO;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;
import com.klef.soa.sdp.security.StudentAccessPolicy;

@ExtendWith(MockitoExtension.class)
class AcademicInsightServiceTest {
    @Mock StudentRepository students;
    @Mock AttendanceSummaryClient attendance;
    @Mock ResultClient results;
    @Mock StudentAccessPolicy accessPolicy;
    private AcademicInsightService service;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new AcademicInsightService(students, attendance, results, accessPolicy);
        student = new Student();
        student.setId(17L);
        student.setName("Asha Rao");
        student.setEmail("asha@example.edu");
        student.setRole("STUDENT");
        student.setDepartment("CS");
        student.setSemester(3);
        student.setStatus("ACTIVE");
        when(students.findByEmail("asha@example.edu")).thenReturn(Optional.of(student));
    }

    @Test
    void derivesExplainableAttendanceAndPerformanceFromPublishedRecords() {
        AttendanceSummaryDTO attendanceSummary = new AttendanceSummaryDTO();
        attendanceSummary.setTotalClasses(10);
        attendanceSummary.setClassesAttended(7);
        attendanceSummary.setClassesAbsent(3);
        attendanceSummary.setAttendancePercentage(70.0);
        attendanceSummary.setAttendanceStatus("CRITICAL");
        attendanceSummary.setAttendanceRisk("HIGH");
        attendanceSummary.setRequiredAttendanceThreshold(75.0);
        attendanceSummary.setExamEligible(false);
        when(attendance.getSummary(17L, null)).thenReturn(attendanceSummary);
        when(results.getResultsByStudentId(17L)).thenReturn(List.of(result(1, "Algorithms", 9, 85, "PASS"),
                result(2, "Networks", 0, 42, "FAIL"), result(3, "Withheld", 10, 99, "WITHHELD")));

        AcademicInsightsDTO insight = service.forStudentEmail("asha@example.edu");

        assertEquals(70.0, insight.attendance().percentage());
        assertEquals("CRITICAL", insight.attendance().status());
        assertEquals("HIGH", insight.attendance().risk());
        assertEquals(2, insight.performance().resultsCount());
        assertEquals(6, insight.performance().creditsRegistered());
        assertEquals(3, insight.performance().creditsEarned());
        assertEquals(4.5, insight.performance().cumulativeGpa());
        assertEquals("Algorithms", insight.performance().strongestSubject().courseName());
        assertEquals("Networks", insight.performance().focusSubject().courseName());
        assertEquals("HIGH", insight.insights().get(0).severity());
        assertEquals(List.of(), insight.unavailableSources());
    }

    @Test
    void returnsPartialFactsWhenAnAcademicSourceIsUnavailable() {
        when(attendance.getSummary(17L, null)).thenThrow(new IllegalStateException("offline"));
        when(results.getResultsByStudentId(17L)).thenReturn(List.of());

        AcademicInsightsDTO insight = service.forStudentEmail("asha@example.edu");

        assertNull(insight.attendance());
        assertNull(insight.performance());
        assertEquals(List.of("attendance"), insight.unavailableSources());
        assertEquals(List.of(), insight.insights());
    }

    private ResultRecordDTO result(long id, String name, double gradePoints, double marks, String status) {
        ResultRecordDTO result = new ResultRecordDTO();
        result.setId(id);
        result.setStudentId(17L);
        result.setCourseName(name);
        result.setCourseCode("CS" + id);
        result.setSemester(3);
        result.setAcademicYear("2025-2026");
        result.setCredits(3);
        result.setGradePoints(gradePoints);
        result.setTotalMarks(marks);
        result.setGrade("A");
        result.setResultStatus(status);
        return result;
    }
}
