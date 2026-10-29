package com.klef.soa.sdp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Student ID is mandatory")
    @Column(nullable = false)
    private Long studentId;

    @Min(value = 1, message = "Semester must be at least 1")
    private int semester;

    private String academicYear = "2025-2026";

    @NotBlank(message = "Course code is mandatory")
    @Column(nullable = false)
    private String courseCode; // e.g., "CS301"

    @NotBlank(message = "Course name is mandatory")
    @Column(nullable = false)
    private String courseName; // e.g., "Database Management Systems"

    @Min(value = 1, message = "Credits must be at least 1")
    private int credits = 3;

    @Min(value = 0, message = "Internal marks cannot be negative")
    @Max(value = 40, message = "Internal marks maximum is 40")
    private double internalMarks;

    @Min(value = 0, message = "External marks cannot be negative")
    @Max(value = 60, message = "External marks maximum is 60")
    private double externalMarks;

    private double totalMarks;

    private String grade; // S (>=90), A (80-89), B (70-79), C (60-69), D (50-59), F (<50)

    private double gradePoints; // S=10.0, A=9.0, B=8.0, C=7.0, D=6.0, F=0.0

    private String resultStatus; // PASS, FAIL, WITHHELD

    public Result() {
    }

    public Result(Long id, Long studentId, int semester, String academicYear, String courseCode, String courseName,
            int credits, double internalMarks, double externalMarks, double totalMarks, String grade,
            double gradePoints, String resultStatus) {
        this.id = id;
        this.studentId = studentId;
        this.semester = semester;
        this.academicYear = academicYear;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.totalMarks = totalMarks;
        this.grade = grade;
        this.gradePoints = gradePoints;
        this.resultStatus = resultStatus;
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

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public double getGradePoints() {
        return gradePoints;
    }

    public void setGradePoints(double gradePoints) {
        this.gradePoints = gradePoints;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    @Override
    public String toString() {
        return "Result [id=" + id + ", studentId=" + studentId + ", semester=" + semester + ", academicYear="
                + academicYear + ", courseCode=" + courseCode + ", courseName=" + courseName + ", credits=" + credits
                + ", internalMarks=" + internalMarks + ", externalMarks=" + externalMarks + ", totalMarks=" + totalMarks
                + ", grade=" + grade + ", gradePoints=" + gradePoints + ", resultStatus=" + resultStatus + "]";
    }
}
