package com.klef.soa.sdp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BulkAttendanceItemDTO {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotBlank(message = "Status is required (PRESENT, ABSENT, LATE, EXCUSED)")
    private String status;

    private String remarks;

    public BulkAttendanceItemDTO() {
    }

    public BulkAttendanceItemDTO(Long studentId, String status, String remarks) {
        this.studentId = studentId;
        this.status = status;
        this.remarks = remarks;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    @Override
    public String toString() {
        return "BulkAttendanceItemDTO [studentId=" + studentId + ", status=" + status + ", remarks=" + remarks + "]";
    }
}
