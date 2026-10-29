package com.klef.soa.sdp.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.klef.soa.sdp.dto.ResultRecordDTO;

@FeignClient(name = "RESULT-SERVICE")
public interface ResultClient {

    @GetMapping("/api/results/student/{studentId}")
    List<ResultRecordDTO> getResultsByStudentId(@PathVariable("studentId") Long studentId);
}
