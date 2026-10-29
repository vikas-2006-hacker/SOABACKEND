package com.klef.soa.sdp.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.klef.soa.sdp.dto.AttendanceRecordDTO;

@FeignClient(name = "ATTENDANCE-SERVICE")
public interface AttendenceClient {

    @GetMapping("/api/attendance/student/{studentId}")
    List<AttendanceRecordDTO> getAttendanceByStudentId(@PathVariable("studentId") Long studentId);
}
