package com.klef.soa.sdp.dto;

import com.klef.soa.sdp.entity.Student;

/** Safe profile fields for the authenticated user's own student record. */
public record CurrentStudentDTO(
        Long id,
        String name,
        String email,
        String department,
        String courses,
        int semester,
        String academicYear,
        String status) {

    public static CurrentStudentDTO from(Student student) {
        return new CurrentStudentDTO(student.getId(), student.getName(), student.getEmail(),
                student.getDepartment(), student.getCourses(), student.getSemester(),
                student.getAcademicYear(), student.getStatus());
    }
}
