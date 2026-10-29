package com.klef.soa.sdp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
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

import com.klef.soa.sdp.client.StudentClient;
import com.klef.soa.sdp.configuration.AttendanceThresholdPolicy;
import com.klef.soa.sdp.dto.StudentDTO;
import com.klef.soa.sdp.dto.AttendanceSummaryDTO;
import com.klef.soa.sdp.dto.AttendanceRequestDTO;
import com.klef.soa.sdp.dto.BulkAttendanceItemDTO;
import com.klef.soa.sdp.dto.BulkAttendanceRequestDTO;
import com.klef.soa.sdp.entity.Attendence;
import com.klef.soa.sdp.exception.ResourceNotFoundException;
import com.klef.soa.sdp.repository.AttendenceRepository;

@Service
@Transactional
public class AttendenceServiceImpl implements AttendenceService {

    @Autowired
    private AttendenceRepository attendenceRepository;

    @Autowired
    private StudentClient studentClient;

    @Autowired
    private AttendanceThresholdPolicy attendanceThresholdPolicy;

    private boolean isStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
    }

    private boolean isFaculty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
    }

    private String authenticatedFacultyEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !isFaculty()) {
            throw new AccessDeniedException("Authenticated faculty access is required");
        }
        return auth.getName();
    }

    private Set<Long> mappedStudentIds() {
        try {
            return studentClient.getMappedStudents().stream().map(StudentDTO::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to load faculty student assignments.");
        }
    }

    private boolean facultyCanAccess(Long studentId, String courseCode) {
        try {
            Map<String, Object> mapping = studentClient.checkStudentExists(studentId, courseCode);
            return Boolean.TRUE.equals(mapping.get("exists"));
        } catch (FeignException ex) {
            if (ex.status() == 403 || ex.status() == 404) return false;
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify faculty course enrollment.");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify faculty course enrollment.");
        }
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
        checkStudentAccess(studentId, null);
    }

    private void checkStudentAccess(Long studentId, String courseCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) return;
        boolean faculty = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTY"));
        if (faculty) {
            try {
                Map<String, Object> mapping = studentClient.checkStudentExists(studentId, courseCode);
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
    // -----------------------------------------------------------------------
    private void verifyStudentExists(Long studentId, String courseCode) {
        try {
            Map<String, Object> response = studentClient.checkStudentExists(studentId, courseCode);
            Boolean exists = (Boolean) response.get("exists");
            if (exists == null || !exists) {
                throw new ResourceNotFoundException(
                        "Student with ID " + studentId + " is not registered in the Student Information Service. "
                        + "Please add the student first before marking attendance.");
            }
        } catch (ResourceNotFoundException ex) {
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
    public Attendence markAttendance(AttendanceRequestDTO request) {
        // Inter-service check: student must exist in studentdb
        verifyStudentExists(request.getStudentId(), request.getCourseCode());

        // Check if attendance already marked for this student, course, and date
        attendenceRepository.findByStudentIdAndCourseCodeAndDate(
                request.getStudentId(),
                request.getCourseCode(),
                request.getDate()
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("Attendance already marked for student ID "
                    + request.getStudentId() + " in course " + request.getCourseCode()
                    + " on " + request.getDate());
        });

        // Map DTO → Entity (id is NOT set — DB auto-generates it)
        Attendence attendence = new Attendence();
        attendence.setStudentId(request.getStudentId());
        attendence.setCourseCode(request.getCourseCode());
        attendence.setDate(request.getDate());
        attendence.setStatus(request.getStatus().toUpperCase());
        attendence.setRemarks(request.getRemarks());
        attendence.setMarkedBy(authenticatedFacultyEmail());

        return attendenceRepository.save(attendence);
    }

    @Override
    public List<Attendence> markBulkAttendance(BulkAttendanceRequestDTO request) {
        List<Attendence> savedList = new ArrayList<>();
        for (BulkAttendanceItemDTO item : request.getAttendanceRecords()) {
            // Inter-service check: each student must exist in studentdb
            verifyStudentExists(item.getStudentId(), request.getCourseCode());

            // Check if record already exists; if so update, otherwise create new
            Attendence record = attendenceRepository.findByStudentIdAndCourseCodeAndDate(
                    item.getStudentId(),
                    request.getCourseCode(),
                    request.getDate()
            ).orElse(new Attendence());

            record.setStudentId(item.getStudentId());
            record.setCourseCode(request.getCourseCode());
            record.setDate(request.getDate());
            record.setStatus(item.getStatus().toUpperCase());
            record.setRemarks(item.getRemarks());
            record.setMarkedBy(authenticatedFacultyEmail());

            savedList.add(attendenceRepository.save(record));
        }
        return savedList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendence> getAllAttendance() {
        if (isStudent()) return attendenceRepository.findByStudentId(currentStudentId());
        if (isFaculty()) {
            return attendenceRepository.findAll().stream()
                    .filter(a -> facultyCanAccess(a.getStudentId(), a.getCourseCode())).toList();
        }
        return attendenceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Attendence getAttendanceById(Long id) {
        Attendence record = attendenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));
        checkStudentAccess(record.getStudentId(), record.getCourseCode());
        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendence> getAttendanceByStudentId(Long studentId) {
        checkStudentAccess(studentId);
        List<Attendence> records = attendenceRepository.findByStudentId(studentId);
        if (isFaculty()) return records.stream().filter(a -> facultyCanAccess(a.getStudentId(), a.getCourseCode())).toList();
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendence> getAttendanceByStudentIdAndCourse(Long studentId, String courseCode) {
        checkStudentAccess(studentId, courseCode);
        return attendenceRepository.findByStudentIdAndCourseCode(studentId, courseCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendence> getAttendanceByCourse(String courseCode) {
        if (isStudent()) return attendenceRepository.findByStudentIdAndCourseCode(currentStudentId(), courseCode);
        List<Attendence> records = attendenceRepository.findByCourseCode(courseCode);
        if (isFaculty()) return records.stream().filter(a -> facultyCanAccess(a.getStudentId(), a.getCourseCode())).toList();
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendence> getAttendanceByDate(LocalDate date) {
        if (isStudent()) return attendenceRepository.findByStudentIdAndDate(currentStudentId(), date);
        List<Attendence> records = attendenceRepository.findByDate(date);
        if (isFaculty()) return records.stream().filter(a -> facultyCanAccess(a.getStudentId(), a.getCourseCode())).toList();
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryDTO getAttendanceSummary(Long studentId, String courseCode) {
        checkStudentAccess(studentId, courseCode);
        long total;
        long present;
        long absent;
        long late;
        long excused;

        if (isFaculty() && (courseCode == null || courseCode.isBlank())) {
            List<Attendence> authorizedRecords = attendenceRepository.findByStudentId(studentId).stream()
                    .filter(record -> facultyCanAccess(record.getStudentId(), record.getCourseCode())).toList();
            total = authorizedRecords.size();
            present = authorizedRecords.stream().filter(record -> "PRESENT".equalsIgnoreCase(record.getStatus())).count();
            late = authorizedRecords.stream().filter(record -> "LATE".equalsIgnoreCase(record.getStatus())).count();
            absent = authorizedRecords.stream().filter(record -> "ABSENT".equalsIgnoreCase(record.getStatus())).count();
            excused = authorizedRecords.stream().filter(record -> "EXCUSED".equalsIgnoreCase(record.getStatus())).count();
        } else if (courseCode != null && !courseCode.isEmpty()) {
            total   = attendenceRepository.countByStudentIdAndCourseCode(studentId, courseCode);
            present = attendenceRepository.countByStudentIdAndCourseCodeAndStatusIgnoreCase(studentId, courseCode, "PRESENT");
            late    = attendenceRepository.countByStudentIdAndCourseCodeAndStatusIgnoreCase(studentId, courseCode, "LATE");
            absent  = attendenceRepository.countByStudentIdAndCourseCodeAndStatusIgnoreCase(studentId, courseCode, "ABSENT");
            excused = attendenceRepository.countByStudentIdAndCourseCodeAndStatusIgnoreCase(studentId, courseCode, "EXCUSED");
        } else {
            total   = attendenceRepository.countByStudentId(studentId);
            present = attendenceRepository.countByStudentIdAndStatusIgnoreCase(studentId, "PRESENT");
            late    = attendenceRepository.countByStudentIdAndStatusIgnoreCase(studentId, "LATE");
            absent  = attendenceRepository.countByStudentIdAndStatusIgnoreCase(studentId, "ABSENT");
            excused = attendenceRepository.countByStudentIdAndStatusIgnoreCase(studentId, "EXCUSED");
        }

        long attended  = present + late;
        double precisePercentage = 0.0;
        if (total > 0) {
            precisePercentage = (double) attended * 100.0 / total;
        }
        double percentage = BigDecimal.valueOf(precisePercentage)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

        boolean eligible = attendanceThresholdPolicy.examEligible(total, precisePercentage);
        String eligibilityStatus = eligible ? "ELIGIBLE" : "ATTENDANCE SHORTAGE";

        return AttendanceSummaryDTO.builder()
                .studentId(studentId)
                .courseCode(courseCode)
                .totalClasses(total)
                .classesAttended(attended)
                .classesAbsent(absent)
                .classesExcused(excused)
                .attendancePercentage(percentage)
                .preciseAttendancePercentage(precisePercentage)
                .examEligible(eligible)
                .eligibilityStatus(eligibilityStatus)
                .attendanceStatus(attendanceThresholdPolicy.status(total, precisePercentage))
                .attendanceRisk(attendanceThresholdPolicy.risk(total, precisePercentage))
                .requiredAttendanceThreshold(attendanceThresholdPolicy.getWarningThreshold())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummaryDTO> getStudentsWithShortage(double thresholdPercentage) {
        if (isStudent()) {
            AttendanceSummaryDTO ownSummary = getAttendanceSummary(currentStudentId(), null);
            return ownSummary.getTotalClasses() > 0 && ownSummary.getPreciseAttendancePercentage() < thresholdPercentage ? List.of(ownSummary) : List.of();
        }
        Set<Long> allStudentIds = new HashSet<>(attendenceRepository.findAll().stream()
                .map(Attendence::getStudentId)
                .collect(Collectors.toSet()));
        if (isFaculty()) allStudentIds.retainAll(mappedStudentIds());

        List<AttendanceSummaryDTO> shortageList = new ArrayList<>();
        for (Long sId : allStudentIds) {
            AttendanceSummaryDTO summary = getAttendanceSummary(sId, null);
            if (summary.getTotalClasses() > 0 && summary.getPreciseAttendancePercentage() < thresholdPercentage) {
                shortageList.add(summary);
            }
        }
        return shortageList;
    }

    @Override
    public Attendence updateAttendance(Long id, AttendanceRequestDTO request) {
        Attendence existing = getAttendanceById(id);
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getRemarks() != null) {
            existing.setRemarks(request.getRemarks());
        }
        String nextCourseCode = request.getCourseCode() == null ? existing.getCourseCode() : request.getCourseCode();
        verifyStudentExists(existing.getStudentId(), nextCourseCode);
        existing.setMarkedBy(authenticatedFacultyEmail());
        if (request.getCourseCode() != null) {
            existing.setCourseCode(request.getCourseCode());
        }
        if (request.getDate() != null) {
            existing.setDate(request.getDate());
        }
        return attendenceRepository.save(existing);
    }

    @Override
    public void deleteAttendance(Long id) {
        Attendence existing = getAttendanceById(id);
        attendenceRepository.delete(existing);
    }
}
