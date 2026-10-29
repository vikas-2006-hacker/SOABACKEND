package com.klef.soa.sdp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import feign.FeignException;

import com.klef.soa.sdp.client.AttendanceClient;
import com.klef.soa.sdp.client.StudentClient;
import com.klef.soa.sdp.dto.*;
import com.klef.soa.sdp.entity.Result;
import com.klef.soa.sdp.exception.ResourceNotFoundException;
import com.klef.soa.sdp.repository.ResultRepository;

@Service
@Transactional
public class ResultServiceImpl implements ResultService {

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private StudentClient studentClient;

    @Autowired(required = false)
    private AttendanceClient attendanceClient;

    private boolean isStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
    }

    private boolean isFaculty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
    }

    private List<Result> getResultsForMappedStudents() {
        try {
            Set<Long> ids = studentClient.getMappedStudents().stream().map(StudentDTO::getId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            return resultRepository.findAll().stream().filter(result -> ids.contains(result.getStudentId())).toList();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to load faculty student assignments.");
        }
    }

    private List<Result> visibleResults(List<Result> results) {
        if (!isStudent()) return results;
        return results.stream().filter(this::isPublished).toList();
    }

    private boolean isPublished(Result result) {
        return "PASS".equalsIgnoreCase(result.getResultStatus())
                || "FAIL".equalsIgnoreCase(result.getResultStatus());
    }

    private Long currentStudentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        try {
            StudentDTO student = studentClient.getStudentByEmail(auth.getName());
            if (student == null || student.getId() == null) throw new AccessDeniedException("Authenticated student mapping not found");
            return student.getId();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessDeniedException("Unable to resolve authenticated student", e);
        }
    }

    private void checkStudentAccess(Long studentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) return;
        boolean faculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        if (faculty) {
            try {
                Map<String, Object> mapping = studentClient.checkStudentExists(studentId, null);
                if (Boolean.TRUE.equals(mapping.get("exists"))) return;
            } catch (Exception ex) {
                throw new AccessDeniedException("Faculty is not assigned to this student", ex);
            }
            throw new AccessDeniedException("Faculty is not assigned to this student");
        }
        if (isStudent() && currentStudentId().equals(studentId)) return;
        throw new AccessDeniedException("Access denied for this student");
    }

    // -----------------------------------------------------------------------
    // Service-to-Service: validate student exists in Studentinformation DB
    // Throws ResourceNotFoundException if student not registered
    // -----------------------------------------------------------------------
    private void verifyStudentExists(Long studentId, String courseCode) {
        try {
            Map<String, Object> response = studentClient.checkStudentExists(studentId, courseCode);
            Boolean exists = (Boolean) response.get("exists");
            if (exists == null || !exists) {
                throw new ResourceNotFoundException(
                        "Student with ID " + studentId + " is not registered in the Student Information Service. "
                        + "Please add the student first before recording results.");
            }
        } catch (ResourceNotFoundException ex) {
            // Re-throw our own exception
            throw ex;
        } catch (FeignException ex) {
            if (ex.status() == 403) throw new AccessDeniedException("Faculty is not assigned to this student", ex);
            if (ex.status() == 404) throw new ResourceNotFoundException("Student ID " + studentId + " is not registered.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify student access with Student Service.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify student access with Student Service.");
        }
    }

    @Override
    public Result recordResult(ResultRequestDTO request) {
        // Inter-service check: student must exist in studentdb before recording result
        verifyStudentExists(request.getStudentId(), request.getCourseCode());

        Result result = resultRepository.findByStudentIdAndCourseCodeAndSemester(
                request.getStudentId(),
                request.getCourseCode(),
                request.getSemester()
        ).orElse(new Result());

        result.setStudentId(request.getStudentId());
        result.setSemester(request.getSemester());
        result.setAcademicYear(request.getAcademicYear());
        result.setCourseCode(request.getCourseCode());
        result.setCourseName(request.getCourseName());
        result.setCredits(request.getCredits());
        result.setInternalMarks(request.getInternalMarks());
        result.setExternalMarks(request.getExternalMarks());

        // Automated Grade Computation Engine
        computeGradeAndPoints(result);

        return resultRepository.save(result);
    }

    @Override
    public List<Result> recordBatchResults(List<ResultRequestDTO> requests) {
        List<Result> results = new ArrayList<>();
        for (ResultRequestDTO req : requests) {
            results.add(recordResult(req));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getAllResults() {
        if (isStudent()) return visibleResults(resultRepository.findByStudentId(currentStudentId()));
        if (isFaculty()) return getResultsForMappedStudents();
        return resultRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultById(Long id) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result record not found with ID: " + id));
        checkStudentAccess(result.getStudentId());
        if (isStudent() && !isPublished(result)) {
            throw new ResourceNotFoundException("Result record not found with ID: " + id);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsByStudentId(Long studentId) {
        checkStudentAccess(studentId);
        return visibleResults(resultRepository.findByStudentId(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public SemesterResultDTO getSemesterResult(Long studentId, int semester) {
        checkStudentAccess(studentId);
        List<Result> courseResults = visibleResults(resultRepository.findByStudentIdAndSemester(studentId, semester));
        if (courseResults.isEmpty()) {
            throw new ResourceNotFoundException("No results found for student ID " + studentId + " in semester " + semester);
        }

        int totalCredits = 0;
        int creditsEarned = 0;
        double weightedPoints = 0.0;
        boolean hasFailedSubject = false;

        String academicYear = courseResults.get(0).getAcademicYear();

        for (Result r : courseResults) {
            totalCredits += r.getCredits();
            if (!"FAIL".equalsIgnoreCase(r.getResultStatus())) {
                creditsEarned += r.getCredits();
            } else {
                hasFailedSubject = true;
            }
            weightedPoints += (r.getGradePoints() * r.getCredits());
        }

        double gpa = totalCredits > 0
                ? BigDecimal.valueOf(weightedPoints / totalCredits).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        String standing = hasFailedSubject ? "PROBATION" : "PASS";

        return SemesterResultDTO.builder()
                .studentId(studentId)
                .semester(semester)
                .academicYear(academicYear)
                .totalCredits(totalCredits)
                .creditsEarned(creditsEarned)
                .semesterGPA(gpa)
                .semesterStanding(standing)
                .courseResults(courseResults)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> calculateCumulativeGPA(Long studentId) {
        checkStudentAccess(studentId);
        List<Result> allResults = visibleResults(resultRepository.findByStudentId(studentId));
        if (allResults.isEmpty()) {
            return Map.of(
                    "studentId", studentId,
                    "cgpa", 0.0,
                    "totalCreditsEarned", 0,
                    "academicStanding", "NO_RECORDS"
            );
        }

        int totalCredits = 0;
        int creditsEarned = 0;
        double weightedPoints = 0.0;
        boolean hasFailures = false;

        for (Result r : allResults) {
            totalCredits += r.getCredits();
            if (!"FAIL".equalsIgnoreCase(r.getResultStatus())) {
                creditsEarned += r.getCredits();
            } else {
                hasFailures = true;
            }
            weightedPoints += (r.getGradePoints() * r.getCredits());
        }

        double cgpa = totalCredits > 0
                ? BigDecimal.valueOf(weightedPoints / totalCredits).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        String standing;
        if (hasFailures) {
            standing = "ACADEMIC PROBATION";
        } else if (cgpa >= 9.0) {
            standing = "FIRST CLASS WITH DISTINCTION";
        } else if (cgpa >= 7.5) {
            standing = "FIRST CLASS";
        } else if (cgpa >= 6.0) {
            standing = "SECOND CLASS";
        } else {
            standing = "PASS";
        }

        return Map.of(
                "studentId", studentId,
                "cgpa", cgpa,
                "totalCreditsRegistered", totalCredits,
                "totalCreditsEarned", creditsEarned,
                "academicStanding", standing
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicTranscriptDTO generateAcademicTranscript(Long studentId) {
        checkStudentAccess(studentId);
        // Fetch Student Details via OpenFeign (strict - student must exist)
        StudentDTO student = null;
        try {
            student = studentClient.getStudentById(studentId);
        } catch (Exception e) {
            System.out.println("Inter-service call to Student Service failed: " + e.getMessage());
        }

        // Fetch Attendance Summary via OpenFeign (optional)
        AttendanceSummaryDTO attendance = null;
        if (attendanceClient != null) {
            try {
                attendance = attendanceClient.getAttendanceSummary(studentId, null);
            } catch (Exception e) {
                System.out.println("Inter-service call to Attendance Service failed: " + e.getMessage());
            }
        }

        // Fetch all results grouped by semester
        List<Result> allResults = visibleResults(resultRepository.findByStudentId(studentId));
        Map<Integer, List<Result>> resultsBySemester = allResults.stream()
                .collect(Collectors.groupingBy(Result::getSemester));

        List<SemesterResultDTO> semesterBreakdowns = new ArrayList<>();
        int totalCreditsRegistered = 0;
        int totalCreditsEarned = 0;
        double overallWeightedPoints = 0.0;
        boolean hasFailures = false;

        for (Map.Entry<Integer, List<Result>> entry : resultsBySemester.entrySet()) {
            int sem = entry.getKey();
            List<Result> list = entry.getValue();

            int semCredits = 0;
            int semEarned = 0;
            double semPoints = 0.0;
            boolean semFail = false;

            for (Result r : list) {
                semCredits += r.getCredits();
                totalCreditsRegistered += r.getCredits();
                if (!"FAIL".equalsIgnoreCase(r.getResultStatus())) {
                    semEarned += r.getCredits();
                    totalCreditsEarned += r.getCredits();
                } else {
                    semFail = true;
                    hasFailures = true;
                }
                double pts = r.getGradePoints() * r.getCredits();
                semPoints += pts;
                overallWeightedPoints += pts;
            }

            double semGPA = semCredits > 0
                    ? BigDecimal.valueOf(semPoints / semCredits).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            semesterBreakdowns.add(SemesterResultDTO.builder()
                    .studentId(studentId)
                    .semester(sem)
                    .academicYear(list.isEmpty() ? "2025-2026" : list.get(0).getAcademicYear())
                    .totalCredits(semCredits)
                    .creditsEarned(semEarned)
                    .semesterGPA(semGPA)
                    .semesterStanding(semFail ? "PROBATION" : "PASS")
                    .courseResults(list)
                    .build());
        }

        // Sort semester breakdowns chronologically
        semesterBreakdowns.sort(Comparator.comparingInt(SemesterResultDTO::getSemester));

        double cgpa = totalCreditsRegistered > 0
                ? BigDecimal.valueOf(overallWeightedPoints / totalCreditsRegistered).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        String standing;
        if (hasFailures) {
            standing = "ACADEMIC PROBATION";
        } else if (cgpa >= 9.0) {
            standing = "FIRST CLASS WITH DISTINCTION";
        } else if (cgpa >= 7.5) {
            standing = "FIRST CLASS";
        } else if (cgpa >= 6.0) {
            standing = "SECOND CLASS";
        } else if (totalCreditsRegistered > 0) {
            standing = "PASS";
        } else {
            standing = "NO RESULTS RECORDED";
        }

        return AcademicTranscriptDTO.builder()
                .studentId(studentId)
                .studentName(student != null ? student.getName() : "Student #" + studentId)
                .studentEmail(student != null ? student.getEmail() : "N/A")
                .department(student != null ? student.getDepartment() : "General")
                .currentSemester(student != null ? student.getSemester() : 1)
                .studentStatus(student != null ? student.getStatus() : "ACTIVE")
                .overallAttendancePercentage(attendance != null ? attendance.getAttendancePercentage() : 100.0)
                .examEligibility(attendance != null ? attendance.isExamEligible() : true)
                .attendanceStatus(attendance != null ? attendance.getEligibilityStatus() : "ELIGIBLE")
                .totalCreditsRegistered(totalCreditsRegistered)
                .totalCreditsEarned(totalCreditsEarned)
                .cumulativeGPA(cgpa)
                .overallAcademicStanding(standing)
                .semesterBreakdowns(semesterBreakdowns)
                .transcriptGeneratedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Result updateResult(Long id, ResultRequestDTO updated) {
        Result existing = getResultById(id);
        existing.setInternalMarks(updated.getInternalMarks());
        existing.setExternalMarks(updated.getExternalMarks());
        existing.setCredits(updated.getCredits());
        if (updated.getCourseName() != null) {
            existing.setCourseName(updated.getCourseName());
        }
        if (updated.getAcademicYear() != null) {
            existing.setAcademicYear(updated.getAcademicYear());
        }
        computeGradeAndPoints(existing);
        return resultRepository.save(existing);
    }

    @Override
    public void deleteResult(Long id) {
        Result existing = getResultById(id);
        resultRepository.delete(existing);
    }

    private void computeGradeAndPoints(Result result) {
        double total = result.getInternalMarks() + result.getExternalMarks();
        result.setTotalMarks(total);

        // Minimum passing criteria: external marks >= 21 (out of 60) AND total marks >= 50
        if (result.getExternalMarks() < 21.0 || total < 50.0) {
            result.setGrade("F");
            result.setGradePoints(0.0);
            result.setResultStatus("FAIL");
        } else if (total >= 90.0) {
            result.setGrade("S");
            result.setGradePoints(10.0);
            result.setResultStatus("PASS");
        } else if (total >= 80.0) {
            result.setGrade("A");
            result.setGradePoints(9.0);
            result.setResultStatus("PASS");
        } else if (total >= 70.0) {
            result.setGrade("B");
            result.setGradePoints(8.0);
            result.setResultStatus("PASS");
        } else if (total >= 60.0) {
            result.setGrade("C");
            result.setGradePoints(7.0);
            result.setResultStatus("PASS");
        } else {
            result.setGrade("D");
            result.setGradePoints(6.0);
            result.setResultStatus("PASS");
        }
    }
}
