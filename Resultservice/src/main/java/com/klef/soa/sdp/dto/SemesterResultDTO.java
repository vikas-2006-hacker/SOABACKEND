package com.klef.soa.sdp.dto;

import java.util.List;

import com.klef.soa.sdp.entity.Result;

public class SemesterResultDTO {
    private Long studentId;
    private int semester;
    private String academicYear;
    private int totalCredits;
    private int creditsEarned;
    private double semesterGPA; // Grade Point Average
    private String semesterStanding; // PASS, PROBATION, WITHHELD
    private List<Result> courseResults;

    public SemesterResultDTO() {
    }

    public SemesterResultDTO(Long studentId, int semester, String academicYear, int totalCredits, int creditsEarned,
            double semesterGPA, String semesterStanding, List<Result> courseResults) {
        this.studentId = studentId;
        this.semester = semester;
        this.academicYear = academicYear;
        this.totalCredits = totalCredits;
        this.creditsEarned = creditsEarned;
        this.semesterGPA = semesterGPA;
        this.semesterStanding = semesterStanding;
        this.courseResults = courseResults;
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

    public int getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(int totalCredits) {
        this.totalCredits = totalCredits;
    }

    public int getCreditsEarned() {
        return creditsEarned;
    }

    public void setCreditsEarned(int creditsEarned) {
        this.creditsEarned = creditsEarned;
    }

    public double getSemesterGPA() {
        return semesterGPA;
    }

    public void setSemesterGPA(double semesterGPA) {
        this.semesterGPA = semesterGPA;
    }

    public String getSemesterStanding() {
        return semesterStanding;
    }

    public void setSemesterStanding(String semesterStanding) {
        this.semesterStanding = semesterStanding;
    }

    public List<Result> getCourseResults() {
        return courseResults;
    }

    public void setCourseResults(List<Result> courseResults) {
        this.courseResults = courseResults;
    }

    public static SemesterResultDTOBuilder builder() {
        return new SemesterResultDTOBuilder();
    }

    public static class SemesterResultDTOBuilder {
        private Long studentId;
        private int semester;
        private String academicYear;
        private int totalCredits;
        private int creditsEarned;
        private double semesterGPA;
        private String semesterStanding;
        private List<Result> courseResults;

        public SemesterResultDTOBuilder studentId(Long studentId) {
            this.studentId = studentId;
            return this;
        }

        public SemesterResultDTOBuilder semester(int semester) {
            this.semester = semester;
            return this;
        }

        public SemesterResultDTOBuilder academicYear(String academicYear) {
            this.academicYear = academicYear;
            return this;
        }

        public SemesterResultDTOBuilder totalCredits(int totalCredits) {
            this.totalCredits = totalCredits;
            return this;
        }

        public SemesterResultDTOBuilder creditsEarned(int creditsEarned) {
            this.creditsEarned = creditsEarned;
            return this;
        }

        public SemesterResultDTOBuilder semesterGPA(double semesterGPA) {
            this.semesterGPA = semesterGPA;
            return this;
        }

        public SemesterResultDTOBuilder semesterStanding(String semesterStanding) {
            this.semesterStanding = semesterStanding;
            return this;
        }

        public SemesterResultDTOBuilder courseResults(List<Result> courseResults) {
            this.courseResults = courseResults;
            return this;
        }

        public SemesterResultDTO build() {
            return new SemesterResultDTO(studentId, semester, academicYear, totalCredits, creditsEarned, semesterGPA, semesterStanding, courseResults);
        }
    }

    @Override
    public String toString() {
        return "SemesterResultDTO [studentId=" + studentId + ", semester=" + semester + ", academicYear="
                + academicYear + ", totalCredits=" + totalCredits + ", creditsEarned=" + creditsEarned
                + ", semesterGPA=" + semesterGPA + ", semesterStanding=" + semesterStanding + ", courseResults="
                + courseResults + "]";
    }
}
