//package com.klef.soa.sdp;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.time.LocalDate;
//import java.util.List;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.klef.soa.sdp.dto.AttendanceSummaryDTO;
//import com.klef.soa.sdp.dto.BulkAttendanceItemDTO;
//import com.klef.soa.sdp.dto.BulkAttendanceRequestDTO;
//import com.klef.soa.sdp.entity.Attendence;
//import com.klef.soa.sdp.repository.AttendenceRepository;
//import com.klef.soa.sdp.service.AttendenceService;
//
//@SpringBootTest
//@Transactional
//class AttendenceServiceTest {
//
//    @Autowired
//    private AttendenceService attendenceService;
//
//    @Autowired
//    private AttendenceRepository attendenceRepository;
//
//    @BeforeEach
//    void setUp() {
//        attendenceRepository.deleteAll();
//    }
//
//    @Test
//    void testMarkAttendanceAndSummary() {
//        Attendence att1 = new Attendence(null, 101L, "CS301", LocalDate.of(2026, 9, 1), "PRESENT", "On time", "Prof. Smith");
//        Attendence att2 = new Attendence(null, 101L, "CS301", LocalDate.of(2026, 9, 2), "PRESENT", "On time", "Prof. Smith");
//        Attendence att3 = new Attendence(null, 101L, "CS301", LocalDate.of(2026, 9, 3), "ABSENT", "Unexcused", "Prof. Smith");
//        Attendence att4 = new Attendence(null, 101L, "CS301", LocalDate.of(2026, 9, 4), "PRESENT", "On time", "Prof. Smith");
//
//        attendenceService.markAttendance(att1);
//        attendenceService.markAttendance(att2);
//        attendenceService.markAttendance(att3);
//        attendenceService.markAttendance(att4);
//
//        AttendanceSummaryDTO summary = attendenceService.getAttendanceSummary(101L, "CS301");
//        assertEquals(4, summary.getTotalClasses());
//        assertEquals(3, summary.getClassesAttended());
//        assertEquals(1, summary.getClassesAbsent());
//        assertEquals(75.0, summary.getAttendancePercentage());
//        assertTrue(summary.isExamEligible());
//        assertEquals("ELIGIBLE", summary.getEligibilityStatus());
//    }
//
//    @Test
//    void testBulkAttendanceAndShortage() {
//        BulkAttendanceRequestDTO bulkReq = new BulkAttendanceRequestDTO(
//                "CS302",
//                LocalDate.of(2026, 9, 5),
//                "Dr. John",
//                List.of(
//                        new BulkAttendanceItemDTO(101L, "ABSENT", "Sick"),
//                        new BulkAttendanceItemDTO(102L, "PRESENT", "Active")
//                )
//        );
//
//        List<Attendence> saved = attendenceService.markBulkAttendance(bulkReq);
//        assertEquals(2, saved.size());
//
//        // Student 101 attended 0/1 classes -> 0%
//        AttendanceSummaryDTO summary101 = attendenceService.getAttendanceSummary(101L, "CS302");
//        assertEquals(0.0, summary101.getAttendancePercentage());
//        assertFalse(summary101.isExamEligible());
//
//        List<AttendanceSummaryDTO> shortageList = attendenceService.getStudentsWithShortage(75.0);
//        assertTrue(shortageList.stream().anyMatch(s -> s.getStudentId().equals(101L)));
//    }
//}
