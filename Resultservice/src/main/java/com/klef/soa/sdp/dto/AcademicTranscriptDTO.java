package com.klef.soa.sdp.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AcademicTranscriptDTO {

    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String department;
    private int currentSemester;
    private String studentStatus;

    // Attendance Integration
    private double overallAttendancePercentage;
    private boolean examEligibility;
    private String attendanceStatus;

    // Academic Performance Integration
    private int totalCreditsRegistered;
    private int totalCreditsEarned;
    private double cumulativeGPA; // CGPA across all semesters
    private String overallAcademicStanding; // DISTINCTION, FIRST CLASS, SECOND CLASS, PROBATION

    // Semester Breakdowns
    private List<SemesterResultDTO> semesterBreakdowns;

    private LocalDateTime transcriptGeneratedAt;

    public AcademicTranscriptDTO() {
    }

    public AcademicTranscriptDTO(Long studentId, String studentName, String studentEmail, String department,
            int currentSemester, String studentStatus, double overallAttendancePercentage, boolean examEligibility,
            String attendanceStatus, int totalCreditsRegistered, int totalCreditsEarned, double cumulativeGPA,
            String overallAcademicStanding, List<SemesterResultDTO> semesterBreakdowns,
            LocalDateTime transcriptGeneratedAt) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.department = department;
        this.currentSemester = currentSemester;
        this.studentStatus = studentStatus;
        this.overallAttendancePercentage = overallAttendancePercentage;
        this.examEligibility = examEligibility;
        this.attendanceStatus = attendanceStatus;
        this.totalCreditsRegistered = totalCreditsRegistered;
        this.totalCreditsEarned = totalCreditsEarned;
        this.cumulativeGPA = cumulativeGPA;
        this.overallAcademicStanding = overallAcademicStanding;
        this.semesterBreakdowns = semesterBreakdowns;
        this.transcriptGeneratedAt = transcriptGeneratedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    public String getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus) {
        this.studentStatus = studentStatus;
    }

    public double getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public void setOverallAttendancePercentage(double overallAttendancePercentage) {
        this.overallAttendancePercentage = overallAttendancePercentage;
    }

    public boolean isExamEligibility() {
        return examEligibility;
    }

    public void setExamEligibility(boolean examEligibility) {
        this.examEligibility = examEligibility;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public int getTotalCreditsRegistered() {
        return totalCreditsRegistered;
    }

    public void setTotalCreditsRegistered(int totalCreditsRegistered) {
        this.totalCreditsRegistered = totalCreditsRegistered;
    }

    public int getTotalCreditsEarned() {
        return totalCreditsEarned;
    }

    public void setTotalCreditsEarned(int totalCreditsEarned) {
        this.totalCreditsEarned = totalCreditsEarned;
    }

    public double getCumulativeGPA() {
        return cumulativeGPA;
    }

    public void setCumulativeGPA(double cumulativeGPA) {
        this.cumulativeGPA = cumulativeGPA;
    }

    public String getOverallAcademicStanding() {
        return overallAcademicStanding;
    }

    public void setOverallAcademicStanding(String overallAcademicStanding) {
        this.overallAcademicStanding = overallAcademicStanding;
    }

    public List<SemesterResultDTO> getSemesterBreakdowns() {
        return semesterBreakdowns;
    }

    public void setSemesterBreakdowns(List<SemesterResultDTO> semesterBreakdowns) {
        this.semesterBreakdowns = semesterBreakdowns;
    }

    public LocalDateTime getTranscriptGeneratedAt() {
        return transcriptGeneratedAt;
    }

    public void setTranscriptGeneratedAt(LocalDateTime transcriptGeneratedAt) {
        this.transcriptGeneratedAt = transcriptGeneratedAt;
    }

    public static AcademicTranscriptDTOBuilder builder() {
        return new AcademicTranscriptDTOBuilder();
    }

    public static class AcademicTranscriptDTOBuilder {
        private Long studentId;
        private String studentName;
        private String studentEmail;
        private String department;
        private int currentSemester;
        private String studentStatus;
        private double overallAttendancePercentage;
        private boolean examEligibility;
        private String attendanceStatus;
        private int totalCreditsRegistered;
        private int totalCreditsEarned;
        private double cumulativeGPA;
        private String overallAcademicStanding;
        private List<SemesterResultDTO> semesterBreakdowns;
        private LocalDateTime transcriptGeneratedAt;

        public AcademicTranscriptDTOBuilder studentId(Long studentId) {
            this.studentId = studentId;
            return this;
        }

        public AcademicTranscriptDTOBuilder studentName(String studentName) {
            this.studentName = studentName;
            return this;
        }

        public AcademicTranscriptDTOBuilder studentEmail(String studentEmail) {
            this.studentEmail = studentEmail;
            return this;
        }

        public AcademicTranscriptDTOBuilder department(String department) {
            this.department = department;
            return this;
        }

        public AcademicTranscriptDTOBuilder currentSemester(int currentSemester) {
            this.currentSemester = currentSemester;
            return this;
        }

        public AcademicTranscriptDTOBuilder studentStatus(String studentStatus) {
            this.studentStatus = studentStatus;
            return this;
        }

        public AcademicTranscriptDTOBuilder overallAttendancePercentage(double overallAttendancePercentage) {
            this.overallAttendancePercentage = overallAttendancePercentage;
            return this;
        }

        public AcademicTranscriptDTOBuilder examEligibility(boolean examEligibility) {
            this.examEligibility = examEligibility;
            return this;
        }

        public AcademicTranscriptDTOBuilder attendanceStatus(String attendanceStatus) {
            this.attendanceStatus = attendanceStatus;
            return this;
        }

        public AcademicTranscriptDTOBuilder totalCreditsRegistered(int totalCreditsRegistered) {
            this.totalCreditsRegistered = totalCreditsRegistered;
            return this;
        }

        public AcademicTranscriptDTOBuilder totalCreditsEarned(int totalCreditsEarned) {
            this.totalCreditsEarned = totalCreditsEarned;
            return this;
        }

        public AcademicTranscriptDTOBuilder cumulativeGPA(double cumulativeGPA) {
            this.cumulativeGPA = cumulativeGPA;
            return this;
        }

        public AcademicTranscriptDTOBuilder overallAcademicStanding(String overallAcademicStanding) {
            this.overallAcademicStanding = overallAcademicStanding;
            return this;
        }

        public AcademicTranscriptDTOBuilder semesterBreakdowns(List<SemesterResultDTO> semesterBreakdowns) {
            this.semesterBreakdowns = semesterBreakdowns;
            return this;
        }

        public AcademicTranscriptDTOBuilder transcriptGeneratedAt(LocalDateTime transcriptGeneratedAt) {
            this.transcriptGeneratedAt = transcriptGeneratedAt;
            return this;
        }

        public AcademicTranscriptDTO build() {
            return new AcademicTranscriptDTO(studentId, studentName, studentEmail, department, currentSemester,
                    studentStatus, overallAttendancePercentage, examEligibility, attendanceStatus,
                    totalCreditsRegistered, totalCreditsEarned, cumulativeGPA, overallAcademicStanding,
                    semesterBreakdowns, transcriptGeneratedAt);
        }
    }

    @Override
    public String toString() {
        return "AcademicTranscriptDTO [studentId=" + studentId + ", studentName=" + studentName + ", studentEmail="
                + studentEmail + ", department=" + department + ", currentSemester=" + currentSemester
                + ", studentStatus=" + studentStatus + ", overallAttendancePercentage=" + overallAttendancePercentage
                + ", examEligibility=" + examEligibility + ", attendanceStatus=" + attendanceStatus
                + ", totalCreditsRegistered=" + totalCreditsRegistered + ", totalCreditsEarned=" + totalCreditsEarned
                + ", cumulativeGPA=" + cumulativeGPA + ", overallAcademicStanding=" + overallAcademicStanding
                + ", semesterBreakdowns=" + semesterBreakdowns + ", transcriptGeneratedAt=" + transcriptGeneratedAt
                + "]";
    }
}
