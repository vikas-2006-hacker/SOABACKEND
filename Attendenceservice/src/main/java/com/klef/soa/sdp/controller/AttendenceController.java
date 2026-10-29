package com.klef.soa.sdp.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.klef.soa.sdp.dto.AttendanceSummaryDTO;
import com.klef.soa.sdp.dto.AttendanceRequestDTO;
import com.klef.soa.sdp.dto.BulkAttendanceRequestDTO;
import com.klef.soa.sdp.entity.Attendence;
import com.klef.soa.sdp.service.AttendenceService;
import com.klef.soa.sdp.client.NotificationClient;
import com.klef.soa.sdp.configuration.AttendanceThresholdPolicy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
@Tag(
    name = "Attendance Management API",
    description = "Operations for logging daily attendance, calculating percentages, and tracking exam eligibility"
)
@SecurityRequirement(name = "bearerAuth")
public class AttendenceController {

    private static final System.Logger LOGGER = System.getLogger(AttendenceController.class.getName());

    @Autowired
    private AttendenceService attendenceService;

    @Autowired(required = false)
    private NotificationClient notificationClient;

    @Autowired
    private AttendanceThresholdPolicy attendanceThresholdPolicy;


    // =========================================================
    // FACULTY ONLY - MARK SINGLE ATTENDANCE
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @PostMapping
    @Operation(
        summary = "Mark single attendance",
        description = "Faculty only. Records attendance for one student."
    )
    public ResponseEntity<Attendence> markAttendance(
            @Valid @RequestBody AttendanceRequestDTO request) {

        Attendence saved =
                attendenceService.markAttendance(request);
        publishAttendanceEvents(saved.getStudentId(), saved.getCourseCode(), saved.getStatus());

        return new ResponseEntity<>(
                saved,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // FACULTY ONLY - MARK BULK ATTENDANCE
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @PostMapping("/bulk")
    @Operation(
        summary = "Mark bulk attendance",
        description = "Faculty only. Records attendance for multiple students."
    )
    public ResponseEntity<List<Attendence>> markBulkAttendance(
            @Valid @RequestBody BulkAttendanceRequestDTO request) {

        List<Attendence> savedList =
                attendenceService.markBulkAttendance(request);
        savedList.forEach(record -> publishAttendanceEvents(record.getStudentId(), record.getCourseCode(), record.getStatus()));

        return new ResponseEntity<>(
                savedList,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET ALL ATTENDANCE
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(
        summary = "Get all attendance records",
        description = "Authenticated users can view attendance records."
    )
    public ResponseEntity<List<Attendence>> getAllAttendance() {

        return ResponseEntity.ok(
                attendenceService.getAllAttendance()
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET ATTENDANCE BY ID
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Operation(
        summary = "Get attendance record by ID",
        description = "Fetches a single attendance record."
    )
    public ResponseEntity<Attendence> getAttendanceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                attendenceService.getAttendanceById(id)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET STUDENT ATTENDANCE
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/student/{studentId}")
    @Operation(
        summary = "Get attendance logs for a student",
        description = "Retrieves attendance records for a specific student."
    )
    public ResponseEntity<List<Attendence>> getAttendanceByStudentId(
            @PathVariable Long studentId,
            @RequestParam(required = false) String courseCode) {

        if (courseCode != null && !courseCode.isEmpty()) {

            return ResponseEntity.ok(
                attendenceService
                    .getAttendanceByStudentIdAndCourse(
                        studentId,
                        courseCode
                    )
            );
        }

        return ResponseEntity.ok(
                attendenceService
                    .getAttendanceByStudentId(studentId)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - ATTENDANCE SUMMARY
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/student/{studentId}/summary")
    @Operation(
        summary = "Get attendance percentage and exam eligibility",
        description = "Calculates total classes, attendance percentage and exam eligibility."
    )
    public ResponseEntity<AttendanceSummaryDTO> getAttendanceSummary(
            @PathVariable Long studentId,
            @RequestParam(required = false) String courseCode) {

        return ResponseEntity.ok(
                attendenceService.getAttendanceSummary(
                    studentId,
                    courseCode
                )
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET ATTENDANCE BY COURSE
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/course/{courseCode}")
    @Operation(
        summary = "Get attendance by course code",
        description = "Lists attendance entries for a particular course."
    )
    public ResponseEntity<List<Attendence>> getAttendanceByCourse(
            @PathVariable String courseCode) {

        return ResponseEntity.ok(
                attendenceService
                    .getAttendanceByCourse(courseCode)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET ATTENDANCE BY DATE
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/date/{date}")
    @Operation(
        summary = "Get attendance for a specific date",
        description = "Lists attendance records for a given date."
    )
    public ResponseEntity<List<Attendence>> getAttendanceByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                attendenceService
                    .getAttendanceByDate(date)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - SHORTAGE REPORT
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/shortage")
    @Operation(
        summary = "Get students with attendance shortage",
        description = "Lists students whose attendance is below the given threshold."
    )
    public ResponseEntity<List<AttendanceSummaryDTO>>
            getStudentsWithShortage(
                @RequestParam(required = false)
                Double threshold) {

        double effectiveThreshold = threshold == null
                ? attendanceThresholdPolicy.getWarningThreshold() : threshold;

        return ResponseEntity.ok(
                attendenceService
                    .getStudentsWithShortage(effectiveThreshold)
        );
    }


    // =========================================================
    // FACULTY ONLY - UPDATE ATTENDANCE
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @PutMapping("/{id}")
    @Operation(
        summary = "Update attendance record",
        description = "Faculty only. Modifies an existing attendance record."
    )
    public ResponseEntity<Attendence> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequestDTO request) {

        Attendence saved = attendenceService.updateAttendance(id, request);
        publishAttendanceEvents(saved.getStudentId(), saved.getCourseCode(), saved.getStatus());
        return ResponseEntity.ok(saved);
    }


    // =========================================================
    // FACULTY ONLY - DELETE ATTENDANCE
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete attendance record",
        description = "Faculty only. Deletes an attendance record."
    )
    public ResponseEntity<Map<String, String>> deleteAttendance(
            @PathVariable Long id) {

        attendenceService.deleteAttendance(id);

        return ResponseEntity.ok(
            Map.of(
                "message",
                "Attendance record deleted successfully for ID: " + id
            )
        );
    }

    private void publishAttendanceEvents(Long studentId, String courseCode, String status) {
        if (notificationClient == null) return;
        try {
            notificationClient.publishStudentEvent(Map.of(
                    "studentId", studentId,
                    "eventType", "ATTENDANCE_UPDATED",
                    "courseCode", courseCode == null ? "" : courseCode,
                    "detail", "Status: " + (status == null ? "updated" : status)));
            AttendanceSummaryDTO summary = attendenceService.getAttendanceSummary(studentId, null);
            if ("WARNING".equals(summary.getAttendanceStatus()) || "CRITICAL".equals(summary.getAttendanceStatus())) {
                notificationClient.publishStudentEvent(Map.of(
                        "studentId", studentId,
                        "eventType", "ATTENDANCE_WARNING",
                        "detail", String.format(java.util.Locale.ROOT,
                                "Attendance is %.2f%% (%s). The recommended minimum is %.2f%%.",
                                summary.getAttendancePercentage(), summary.getAttendanceStatus(),
                                summary.getRequiredAttendanceThreshold())));
            }
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Attendance saved but notification delivery failed ({0}).", exception.getClass().getSimpleName());
        }
    }
}
