package com.klef.soa.sdp.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.klef.soa.sdp.dto.AttendanceSummaryDTO;

@FeignClient(name = "ATTENDANCE-SERVICE", contextId = "studentAcademicAttendanceSummaryClient")
public interface AttendanceSummaryClient {
    @GetMapping("/api/attendance/student/{studentId}/summary")
    AttendanceSummaryDTO getSummary(@PathVariable("studentId") Long studentId,
            @RequestParam(name = "courseCode", required = false) String courseCode);
}
