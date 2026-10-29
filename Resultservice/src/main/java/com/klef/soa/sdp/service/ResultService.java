package com.klef.soa.sdp.service;

import java.util.List;
import java.util.Map;

import com.klef.soa.sdp.dto.AcademicTranscriptDTO;
import com.klef.soa.sdp.dto.ResultRequestDTO;
import com.klef.soa.sdp.dto.SemesterResultDTO;
import com.klef.soa.sdp.entity.Result;

public interface ResultService {

    Result recordResult(ResultRequestDTO request);

    List<Result> recordBatchResults(List<ResultRequestDTO> requests);

    List<Result> getAllResults();

    Result getResultById(Long id);

    List<Result> getResultsByStudentId(Long studentId);

    SemesterResultDTO getSemesterResult(Long studentId, int semester);

    Map<String, Object> calculateCumulativeGPA(Long studentId);

    AcademicTranscriptDTO generateAcademicTranscript(Long studentId);

    Result updateResult(Long id, ResultRequestDTO updated);

    void deleteResult(Long id);
}
