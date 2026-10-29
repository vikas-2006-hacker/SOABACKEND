package com.klef.soa.sdp.dto;

public class AttendanceSummaryDTO {
    private long totalClasses;
    private long classesAttended;
    private long classesAbsent;
    private long classesExcused;
    private double attendancePercentage;
    private double preciseAttendancePercentage;
    private boolean examEligible;
    private String attendanceStatus;
    private String attendanceRisk;
    private double requiredAttendanceThreshold;

    public long getTotalClasses() { return totalClasses; }
    public void setTotalClasses(long totalClasses) { this.totalClasses = totalClasses; }
    public long getClassesAttended() { return classesAttended; }
    public void setClassesAttended(long classesAttended) { this.classesAttended = classesAttended; }
    public long getClassesAbsent() { return classesAbsent; }
    public void setClassesAbsent(long classesAbsent) { this.classesAbsent = classesAbsent; }
    public long getClassesExcused() { return classesExcused; }
    public void setClassesExcused(long classesExcused) { this.classesExcused = classesExcused; }
    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
    public double getPreciseAttendancePercentage() { return preciseAttendancePercentage; }
    public void setPreciseAttendancePercentage(double preciseAttendancePercentage) { this.preciseAttendancePercentage = preciseAttendancePercentage; }
    public boolean isExamEligible() { return examEligible; }
    public void setExamEligible(boolean examEligible) { this.examEligible = examEligible; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public String getAttendanceRisk() { return attendanceRisk; }
    public void setAttendanceRisk(String attendanceRisk) { this.attendanceRisk = attendanceRisk; }
    public double getRequiredAttendanceThreshold() { return requiredAttendanceThreshold; }
    public void setRequiredAttendanceThreshold(double requiredAttendanceThreshold) { this.requiredAttendanceThreshold = requiredAttendanceThreshold; }
}
