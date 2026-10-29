package com.klef.soa.sdp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.klef.soa.sdp.dto.AcademicTranscriptDTO;
import com.klef.soa.sdp.dto.ResultRequestDTO;
import com.klef.soa.sdp.dto.SemesterResultDTO;
import com.klef.soa.sdp.entity.Result;
import com.klef.soa.sdp.client.StudentClient;
import com.klef.soa.sdp.repository.ResultRepository;
import com.klef.soa.sdp.service.ResultService;

@SpringBootTest
@Transactional
class ResultServiceTest {

    @Autowired
    private ResultService resultService;

    @Autowired
    private ResultRepository resultRepository;

    @MockBean
    private StudentClient studentClient;

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        when(studentClient.checkStudentExists(anyLong(), any())).thenReturn(Map.of("exists", true));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void testRecordResultAutoGrading() {
        // Test S Grade (>=90)
        ResultRequestDTO r1 = new ResultRequestDTO(201L, 1, "2025-2026", "CS301", "Database Systems", 4, 38.0, 56.0); // total 94
        Result res1 = resultService.recordResult(r1);
        assertEquals(94.0, res1.getTotalMarks());
        assertEquals("S", res1.getGrade());
        assertEquals(10.0, res1.getGradePoints());
        assertEquals("PASS", res1.getResultStatus());

        // Test B Grade (70-79)
        ResultRequestDTO r2 = new ResultRequestDTO(201L, 1, "2025-2026", "CS302", "Operating Systems", 3, 28.0, 45.0); // total 73
        Result res2 = resultService.recordResult(r2);
        assertEquals(73.0, res2.getTotalMarks());
        assertEquals("B", res2.getGrade());
        assertEquals(8.0, res2.getGradePoints());
        assertEquals("PASS", res2.getResultStatus());

        // Test F Grade (<50 or external < 21)
        ResultRequestDTO r3 = new ResultRequestDTO(201L, 1, "2025-2026", "CS303", "Computer Networks", 3, 10.0, 18.0); // external 18 < 21
        Result res3 = resultService.recordResult(r3);
        assertEquals("F", res3.getGrade());
        assertEquals(0.0, res3.getGradePoints());
        assertEquals("FAIL", res3.getResultStatus());
    }

    @Test
    void testSemesterResultAndGPA() {
        ResultRequestDTO r1 = new ResultRequestDTO(202L, 2, "2025-2026", "CS401", "Algorithms", 4, 35.0, 50.0); // 85 -> A (9.0 pts), 4 credits
        ResultRequestDTO r2 = new ResultRequestDTO(202L, 2, "2025-2026", "CS402", "Cloud Computing", 4, 30.0, 45.0); // 75 -> B (8.0 pts), 4 credits
        resultService.recordBatchResults(List.of(r1, r2));

        SemesterResultDTO semResult = resultService.getSemesterResult(202L, 2);
        assertEquals(8, semResult.getTotalCredits());
        assertEquals(8, semResult.getCreditsEarned());
        // GPA = (9.0*4 + 8.0*4) / 8 = (36 + 32) / 8 = 68 / 8 = 8.5
        assertEquals(8.5, semResult.getSemesterGPA());
        assertEquals("PASS", semResult.getSemesterStanding());
    }

    @Test
    void testCumulativeGPAAndTranscript() {
        // Semester 1
        ResultRequestDTO r1 = new ResultRequestDTO(203L, 1, "2025-2026", "CS101", "Intro to CS", 4, 36.0, 56.0); // 92 -> S (10.0), 4 cr
        // Semester 2
        ResultRequestDTO r2 = new ResultRequestDTO(203L, 2, "2025-2026", "CS201", "OOP with Java", 4, 32.0, 49.0); // 81 -> A (9.0), 4 cr
        resultService.recordBatchResults(List.of(r1, r2));

        Map<String, Object> cgpaMap = resultService.calculateCumulativeGPA(203L);
        // CGPA = (10.0*4 + 9.0*4) / 8 = 9.5 -> FIRST CLASS WITH DISTINCTION
        assertEquals(9.5, cgpaMap.get("cgpa"));
        assertEquals("FIRST CLASS WITH DISTINCTION", cgpaMap.get("academicStanding"));

        // Generate full transcript
        AcademicTranscriptDTO transcript = resultService.generateAcademicTranscript(203L);
        assertNotNull(transcript);
        assertEquals(203L, transcript.getStudentId());
        assertEquals(9.5, transcript.getCumulativeGPA());
        assertEquals("FIRST CLASS WITH DISTINCTION", transcript.getOverallAcademicStanding());
        assertEquals(2, transcript.getSemesterBreakdowns().size());
    }
}
