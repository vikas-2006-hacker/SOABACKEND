package com.klef.soa.sdp.service;

import java.time.LocalDate;
import java.util.List;

import com.klef.soa.sdp.dto.AttendanceSummaryDTO;
import com.klef.soa.sdp.dto.AttendanceRequestDTO;
import com.klef.soa.sdp.dto.BulkAttendanceRequestDTO;
import com.klef.soa.sdp.entity.Attendence;

public interface AttendenceService {

    Attendence markAttendance(AttendanceRequestDTO request);

    List<Attendence> markBulkAttendance(BulkAttendanceRequestDTO request);

    List<Attendence> getAllAttendance();

    Attendence getAttendanceById(Long id);

    List<Attendence> getAttendanceByStudentId(Long studentId);

    List<Attendence> getAttendanceByStudentIdAndCourse(Long studentId, String courseCode);

    List<Attendence> getAttendanceByCourse(String courseCode);

    List<Attendence> getAttendanceByDate(LocalDate date);

    AttendanceSummaryDTO getAttendanceSummary(Long studentId, String courseCode);

    List<AttendanceSummaryDTO> getStudentsWithShortage(double thresholdPercentage);

    Attendence updateAttendance(Long id, AttendanceRequestDTO request);

    void deleteAttendance(Long id);
}
