package com.klef.soa.sdp.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttendanceRequestDTO {

    @NotNull(message = "Student ID is mandatory")
    private Long studentId;

    @NotBlank(message = "Course code is mandatory")
    private String courseCode;

    @NotNull(message = "Date is mandatory")
    private LocalDate date;

    @NotBlank(message = "Status is mandatory (PRESENT / ABSENT / LATE / EXCUSED)")
    private String status;

    private String remarks;

    private String markedBy;

    public AttendanceRequestDTO() {
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    @Override
    public String toString() {
        return "AttendanceRequestDTO [studentId=" + studentId + ", courseCode=" + courseCode
                + ", date=" + date + ", status=" + status + ", remarks=" + remarks + ", markedBy=" + markedBy + "]";
    }
}
