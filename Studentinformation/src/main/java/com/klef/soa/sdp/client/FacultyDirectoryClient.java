package com.klef.soa.sdp.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.klef.soa.sdp.dto.FacultyDirectoryDTO;

@FeignClient(name = "FACULTY-SERVICE", contextId = "studentFacultyDirectoryClient")
public interface FacultyDirectoryClient {
    @GetMapping("/api/faculty/email/{email}")
    Map<String, Object> getFacultyByEmail(@PathVariable("email") String email);

    @GetMapping("/api/faculty/directory/{email}")
    FacultyDirectoryDTO getFacultyDirectoryByEmail(@PathVariable("email") String email);
}
