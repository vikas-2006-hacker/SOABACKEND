package com.klef.soa.sdp.dto;

public class AttendanceSummaryDTO {

    private Long studentId;
    private String courseCode; // null if overall summary across all courses
    private long totalClasses;
    private long classesAttended; // Present + Late
    private long classesAbsent;
    private long classesExcused;
    private double attendancePercentage; // formatted to 2 decimals
    private double preciseAttendancePercentage;
    private boolean examEligible; // true when the configured attendance minimum is met
    private String eligibilityStatus; // "ELIGIBLE" or "ATTENDANCE SHORTAGE"
    private String attendanceStatus;
    private String attendanceRisk;
    private double requiredAttendanceThreshold;

    public AttendanceSummaryDTO() {
    }

    public AttendanceSummaryDTO(Long studentId, String courseCode, long totalClasses, long classesAttended,
            long classesAbsent, long classesExcused, double attendancePercentage, boolean examEligible,
            String eligibilityStatus) {
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.totalClasses = totalClasses;
        this.classesAttended = classesAttended;
        this.classesAbsent = classesAbsent;
        this.classesExcused = classesExcused;
        this.attendancePercentage = attendancePercentage;
        this.examEligible = examEligible;
        this.eligibilityStatus = eligibilityStatus;
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

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getClassesAttended() {
        return classesAttended;
    }

    public void setClassesAttended(long classesAttended) {
        this.classesAttended = classesAttended;
    }

    public long getClassesAbsent() {
        return classesAbsent;
    }

    public void setClassesAbsent(long classesAbsent) {
        this.classesAbsent = classesAbsent;
    }

    public long getClassesExcused() {
        return classesExcused;
    }

    public void setClassesExcused(long classesExcused) {
        this.classesExcused = classesExcused;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public double getPreciseAttendancePercentage() { return preciseAttendancePercentage; }
    public void setPreciseAttendancePercentage(double preciseAttendancePercentage) { this.preciseAttendancePercentage = preciseAttendancePercentage; }

    public boolean isExamEligible() {
        return examEligible;
    }

    public void setExamEligible(boolean examEligible) {
        this.examEligible = examEligible;
    }

    public String getEligibilityStatus() {
        return eligibilityStatus;
    }

    public void setEligibilityStatus(String eligibilityStatus) {
        this.eligibilityStatus = eligibilityStatus;
    }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public String getAttendanceRisk() { return attendanceRisk; }
    public void setAttendanceRisk(String attendanceRisk) { this.attendanceRisk = attendanceRisk; }
    public double getRequiredAttendanceThreshold() { return requiredAttendanceThreshold; }
    public void setRequiredAttendanceThreshold(double requiredAttendanceThreshold) { this.requiredAttendanceThreshold = requiredAttendanceThreshold; }

    public static AttendanceSummaryDTOBuilder builder() {
        return new AttendanceSummaryDTOBuilder();
    }

    public static class AttendanceSummaryDTOBuilder {
        private Long studentId;
        private String courseCode;
        private long totalClasses;
        private long classesAttended;
        private long classesAbsent;
        private long classesExcused;
        private double attendancePercentage;
        private double preciseAttendancePercentage;
        private boolean examEligible;
        private String eligibilityStatus;
        private String attendanceStatus;
        private String attendanceRisk;
        private double requiredAttendanceThreshold;

        public AttendanceSummaryDTOBuilder studentId(Long studentId) {
            this.studentId = studentId;
            return this;
        }

        public AttendanceSummaryDTOBuilder courseCode(String courseCode) {
            this.courseCode = courseCode;
            return this;
        }

        public AttendanceSummaryDTOBuilder totalClasses(long totalClasses) {
            this.totalClasses = totalClasses;
            return this;
        }

        public AttendanceSummaryDTOBuilder classesAttended(long classesAttended) {
            this.classesAttended = classesAttended;
            return this;
        }

        public AttendanceSummaryDTOBuilder classesAbsent(long classesAbsent) {
            this.classesAbsent = classesAbsent;
            return this;
        }

        public AttendanceSummaryDTOBuilder classesExcused(long classesExcused) {
            this.classesExcused = classesExcused;
            return this;
        }

        public AttendanceSummaryDTOBuilder attendancePercentage(double attendancePercentage) {
            this.attendancePercentage = attendancePercentage;
            return this;
        }

        public AttendanceSummaryDTOBuilder preciseAttendancePercentage(double preciseAttendancePercentage) {
            this.preciseAttendancePercentage = preciseAttendancePercentage;
            return this;
        }

        public AttendanceSummaryDTOBuilder examEligible(boolean examEligible) {
            this.examEligible = examEligible;
            return this;
        }

        public AttendanceSummaryDTOBuilder eligibilityStatus(String eligibilityStatus) {
            this.eligibilityStatus = eligibilityStatus;
            return this;
        }

        public AttendanceSummaryDTOBuilder attendanceStatus(String attendanceStatus) {
            this.attendanceStatus = attendanceStatus;
            return this;
        }

        public AttendanceSummaryDTOBuilder attendanceRisk(String attendanceRisk) {
            this.attendanceRisk = attendanceRisk;
            return this;
        }

        public AttendanceSummaryDTOBuilder requiredAttendanceThreshold(double requiredAttendanceThreshold) {
            this.requiredAttendanceThreshold = requiredAttendanceThreshold;
            return this;
        }

        public AttendanceSummaryDTO build() {
            AttendanceSummaryDTO summary = new AttendanceSummaryDTO(studentId, courseCode, totalClasses, classesAttended, classesAbsent, classesExcused, attendancePercentage, examEligible, eligibilityStatus);
            summary.setPreciseAttendancePercentage(preciseAttendancePercentage);
            summary.setAttendanceStatus(attendanceStatus);
            summary.setAttendanceRisk(attendanceRisk);
            summary.setRequiredAttendanceThreshold(requiredAttendanceThreshold);
            return summary;
        }
    }

    @Override
    public String toString() {
        return "AttendanceSummaryDTO [studentId=" + studentId + ", courseCode=" + courseCode + ", totalClasses="
                + totalClasses + ", classesAttended=" + classesAttended + ", classesAbsent=" + classesAbsent
                + ", classesExcused=" + classesExcused + ", attendancePercentage=" + attendancePercentage
                + ", examEligible=" + examEligible + ", eligibilityStatus=" + eligibilityStatus + "]";
    }
}
