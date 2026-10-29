package com.klef.soa.sdp.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class BulkAttendanceRequestDTO {

    @NotBlank(message = "Course code is mandatory")
    private String courseCode;

    @NotNull(message = "Date is mandatory")
    private LocalDate date;

    private String markedBy;

    @NotEmpty(message = "Attendance list cannot be empty")
    @Valid
    private List<BulkAttendanceItemDTO> attendanceRecords;

    public BulkAttendanceRequestDTO() {
    }

    public BulkAttendanceRequestDTO(String courseCode, LocalDate date, String markedBy, List<BulkAttendanceItemDTO> attendanceRecords) {
        this.courseCode = courseCode;
        this.date = date;
        this.markedBy = markedBy;
        this.attendanceRecords = attendanceRecords;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public List<BulkAttendanceItemDTO> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<BulkAttendanceItemDTO> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    @Override
    public String toString() {
        return "BulkAttendanceRequestDTO [courseCode=" + courseCode + ", date=" + date + ", markedBy=" + markedBy
                + ", attendanceRecords=" + attendanceRecords + "]";
    }
}
