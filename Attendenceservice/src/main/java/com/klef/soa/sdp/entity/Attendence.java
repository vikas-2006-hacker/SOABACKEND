package com.klef.soa.sdp.entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "attendance")
public class Attendence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Student ID is mandatory")
    @Column(nullable = false)
    private Long studentId;

    @NotBlank(message = "Course code is mandatory")
    @Column(nullable = false)
    private String courseCode; // e.g., "CS301 - Data Structures"

    @NotNull(message = "Date is mandatory")
    @Column(nullable = false)
    private LocalDate date;

    @NotBlank(message = "Status is mandatory")
    @Column(nullable = false)
    private String status; // PRESENT, ABSENT, LATE, EXCUSED

    private String remarks;

    private String markedBy; // Faculty name or ID

    public Attendence() {
    }

    public Attendence(Long id, Long studentId, String courseCode, LocalDate date, String status, String remarks, String markedBy) {
        this.id = id;
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.date = date;
        this.status = status;
        this.remarks = remarks;
        this.markedBy = markedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return "Attendence [id=" + id + ", studentId=" + studentId + ", courseCode=" + courseCode + ", date=" + date
                + ", status=" + status + ", remarks=" + remarks + ", markedBy=" + markedBy + "]";
    }
}
