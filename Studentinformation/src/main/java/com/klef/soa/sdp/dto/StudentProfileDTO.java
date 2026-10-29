package com.klef.soa.sdp.dto;

import java.util.List;

import com.klef.soa.sdp.entity.Student;

public class StudentProfileDTO {

    private Student studentDetails;
    private List<AttendanceRecordDTO> attendanceRecords;
    private List<ResultRecordDTO> resultRecords;
    private int totalAttendanceRecords;
    private int totalResultRecords;

    public StudentProfileDTO() {
    }

    public StudentProfileDTO(Student studentDetails,
                             List<AttendanceRecordDTO> attendanceRecords,
                             List<ResultRecordDTO> resultRecords) {
        this.studentDetails = studentDetails;
        this.attendanceRecords = attendanceRecords;
        this.resultRecords = resultRecords;
        this.totalAttendanceRecords = attendanceRecords != null ? attendanceRecords.size() : 0;
        this.totalResultRecords = resultRecords != null ? resultRecords.size() : 0;
    }

    public Student getStudentDetails() { return studentDetails; }
    public void setStudentDetails(Student studentDetails) { this.studentDetails = studentDetails; }

    public List<AttendanceRecordDTO> getAttendanceRecords() { return attendanceRecords; }
    public void setAttendanceRecords(List<AttendanceRecordDTO> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
        this.totalAttendanceRecords = attendanceRecords != null ? attendanceRecords.size() : 0;
    }

    public List<ResultRecordDTO> getResultRecords() { return resultRecords; }
    public void setResultRecords(List<ResultRecordDTO> resultRecords) {
        this.resultRecords = resultRecords;
        this.totalResultRecords = resultRecords != null ? resultRecords.size() : 0;
    }

    public int getTotalAttendanceRecords() { return totalAttendanceRecords; }
    public int getTotalResultRecords() { return totalResultRecords; }
}
