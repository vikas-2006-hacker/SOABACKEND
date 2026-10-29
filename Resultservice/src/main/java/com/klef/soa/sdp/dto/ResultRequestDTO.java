package com.klef.soa.sdp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResultRequestDTO {

    @NotNull(message = "Student ID is mandatory")
    private Long studentId;

    @Min(value = 1, message = "Semester must be at least 1")
    private int semester;

    private String academicYear = "2025-2026";

    @NotBlank(message = "Course code is mandatory")
    private String courseCode;

    @NotBlank(message = "Course name is mandatory")
    private String courseName;

    @Min(value = 1, message = "Credits must be at least 1")
    private int credits = 3;

    @Min(value = 0, message = "Internal marks cannot be negative")
    @Max(value = 40, message = "Internal marks maximum is 40")
    private double internalMarks;

    @Min(value = 0, message = "External marks cannot be negative")
    @Max(value = 60, message = "External marks maximum is 60")
    private double externalMarks;

    public ResultRequestDTO() {
    }

    public ResultRequestDTO(Long studentId, int semester, String academicYear, String courseCode, String courseName,
            int credits, double internalMarks, double externalMarks) {
        this.studentId = studentId;
        this.semester = semester;
        this.academicYear = academicYear;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public double getInternalMarks() {
        return internalMarks;
    }

    public void setInternalMarks(double internalMarks) {
        this.internalMarks = internalMarks;
    }

    public double getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(double externalMarks) {
        this.externalMarks = externalMarks;
    }

    @Override
    public String toString() {
        return "ResultRequestDTO [studentId=" + studentId + ", semester=" + semester + ", academicYear=" + academicYear
                + ", courseCode=" + courseCode + ", courseName=" + courseName + ", credits=" + credits
                + ", internalMarks=" + internalMarks + ", externalMarks=" + externalMarks + "]";
    }
}
