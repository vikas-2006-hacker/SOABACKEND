package com.klef.soa.sdp.service;

import java.util.List;

import com.klef.soa.sdp.dto.StudentProfileDTO;
import com.klef.soa.sdp.entity.Student;

public interface StudentService {

    Student createStudent(Student student);

    List<Student> createStudentsBatch(List<Student> students);

    List<Student> getAllStudents();
    List<Student> getStudentsByFacultyEmail(String facultyEmail);

    Student getStudentById(Long id);

    Student getStudentByEmail(String email);

    List<Student> getStudentsByDepartment(String department);

    List<Student> getStudentsByStatus(String status);

    List<Student> getStudentsByDepartmentAndStatus(String department, String status);

    Student updateStudent(Long id, Student updatedDetails);

    Student updateStudentStatus(Long id, String status);

    void deleteStudent(Long id);

    boolean checkStudentExists(Long id);

    long getTotalStudentCount();

    // Aggregated profile: student + attendance + results
    StudentProfileDTO getStudentProfile(Long id);
}
