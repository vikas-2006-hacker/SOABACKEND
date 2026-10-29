package com.klef.soa.sdp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.klef.soa.sdp.dto.AcademicTranscriptDTO;
import com.klef.soa.sdp.dto.ResultRequestDTO;
import com.klef.soa.sdp.dto.SemesterResultDTO;
import com.klef.soa.sdp.entity.Result;
import com.klef.soa.sdp.service.ResultService;
import com.klef.soa.sdp.client.NotificationClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/results")
@Tag(
    name = "Result Management API",
    description = "Operations for managing student academic results"
)
@SecurityRequirement(name = "bearerAuth")
public class ResultController {

    private static final System.Logger LOGGER = System.getLogger(ResultController.class.getName());

    @Autowired
    private ResultService resultService;

    @Autowired(required = false)
    private NotificationClient notificationClient;


    // =========================================================
    // FACULTY ONLY - RECORD SINGLE RESULT
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @PostMapping
    @Operation(
        summary = "Record student result",
        description = "Faculty only. Records academic result for one student."
    )
    public ResponseEntity<Result> recordResult(
            @Valid @RequestBody ResultRequestDTO request) {

        Result saved = resultService.recordResult(request);
        publishResultEvent(saved);

        return new ResponseEntity<>(
                saved,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // FACULTY ONLY - RECORD BATCH RESULTS
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @PostMapping("/batch")
    @Operation(
        summary = "Record multiple student results",
        description = "Faculty only. Records academic results for multiple students."
    )
    public ResponseEntity<List<Result>> recordBatchResults(
            @Valid @RequestBody List<ResultRequestDTO> requests) {

        List<Result> saved =
                resultService.recordBatchResults(requests);
        saved.forEach(this::publishResultEvent);

        return new ResponseEntity<>(
                saved,
                HttpStatus.CREATED
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET ALL RESULTS
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(
        summary = "Get all results",
        description = "Returns all academic results."
    )
    public ResponseEntity<List<Result>> getAllResults() {

        return ResponseEntity.ok(
                resultService.getAllResults()
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET RESULT BY ID
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Operation(
        summary = "Get result by ID",
        description = "Fetches one academic result by result ID."
    )
    public ResponseEntity<Result> getResultById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                resultService.getResultById(id)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - GET STUDENT RESULTS
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/student/{studentId}")
    @Operation(
        summary = "Get results for a student",
        description = "Retrieves all results for a specific student."
    )
    public ResponseEntity<List<Result>> getResultsByStudentId(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                resultService.getResultsByStudentId(studentId)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - SEMESTER RESULT
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/student/{studentId}/semester/{semester}")
    @Operation(
        summary = "Get semester result",
        description = "Returns semester GPA, credits and course results."
    )
    public ResponseEntity<SemesterResultDTO> getSemesterResult(
            @PathVariable Long studentId,
            @PathVariable int semester) {

        return ResponseEntity.ok(
                resultService.getSemesterResult(
                        studentId,
                        semester
                )
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - CGPA
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/student/{studentId}/cgpa")
    @Operation(
        summary = "Calculate cumulative GPA",
        description = "Calculates the student's cumulative GPA and academic standing."
    )
    public ResponseEntity<Map<String, Object>> calculateCumulativeGPA(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                resultService.calculateCumulativeGPA(studentId)
        );
    }


    // =========================================================
    // AUTHENTICATED USERS - ACADEMIC TRANSCRIPT
    // =========================================================

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/student/{studentId}/transcript")
    @Operation(
        summary = "Generate academic transcript",
        description = "Generates complete academic transcript including results and attendance."
    )
    public ResponseEntity<AcademicTranscriptDTO> generateAcademicTranscript(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                resultService.generateAcademicTranscript(studentId)
        );
    }


    // =========================================================
    // FACULTY ONLY - UPDATE RESULT
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @PutMapping("/{id}")
    @Operation(
        summary = "Update student result",
        description = "Faculty only. Updates an existing academic result."
    )
    public ResponseEntity<Result> updateResult(
            @PathVariable Long id,
            @Valid @RequestBody ResultRequestDTO request) {

        Result saved = resultService.updateResult(id, request);
        publishResultEvent(saved);
        return ResponseEntity.ok(saved);
    }


    // =========================================================
    // FACULTY ONLY - DELETE RESULT
    // =========================================================

    @PreAuthorize("hasRole('FACULTY')")
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete student result",
        description = "Faculty only. Deletes an academic result."
    )
    public ResponseEntity<Map<String, String>> deleteResult(
            @PathVariable Long id) {

        resultService.deleteResult(id);

        return ResponseEntity.ok(
                Map.of(
                    "message",
                    "Result deleted successfully for ID: " + id
                )
        );
    }

    private void publishResultEvent(Result result) {
        if (notificationClient == null || result == null
                || !("PASS".equalsIgnoreCase(result.getResultStatus())
                        || "FAIL".equalsIgnoreCase(result.getResultStatus()))) return;
        try {
            notificationClient.publishStudentEvent(Map.of(
                    "studentId", result.getStudentId(),
                    "eventType", "RESULT_PUBLISHED",
                    "courseCode", result.getCourseCode() == null ? "" : result.getCourseCode()));
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Result saved but notification delivery failed ({0}).", exception.getClass().getSimpleName());
        }
    }
}
