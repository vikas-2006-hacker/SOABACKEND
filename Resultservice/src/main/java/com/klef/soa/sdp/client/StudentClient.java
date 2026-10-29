package com.klef.soa.sdp.client;

import java.util.Map;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.klef.soa.sdp.dto.StudentDTO;

@FeignClient(name = "STUDENT-SERVICE")
public interface StudentClient {

    @GetMapping("/api/faculty/my-students")
    List<StudentDTO> getMappedStudents();

    @GetMapping("/api/students/{id}")
    StudentDTO getStudentById(@PathVariable("id") Long id);

    @GetMapping("/api/students/email/{email}")
    StudentDTO getStudentByEmail(@PathVariable("email") String email);

    @GetMapping("/api/students/check/{id}")
    Map<String, Object> checkStudentExists(@PathVariable("id") Long id, @org.springframework.web.bind.annotation.RequestParam(required = false) String courseCode);
}
