package com.klef.soa.sdp.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.klef.soa.sdp.client.AttendenceClient;
import com.klef.soa.sdp.client.ResultClient;
import com.klef.soa.sdp.dto.AttendanceRecordDTO;
import com.klef.soa.sdp.dto.ResultRecordDTO;
import com.klef.soa.sdp.dto.StudentProfileDTO;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.exception.ResourceNotFoundException;
import com.klef.soa.sdp.repository.StudentRepository;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendenceClient attendenceClient;

    @Autowired
    private ResultClient resultClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------
    @Override
    public Student createStudent(Student student) {
        if (student.getEmail() != null && studentRepository.existsByEmail(student.getEmail())) {
            throw new IllegalArgumentException("Student with email " + student.getEmail() + " already exists");
        }

        encodePasswordAndNormalizeRole(student);
        return studentRepository.save(student);
    }

    @Override
    public List<Student> createStudentsBatch(List<Student> students) {
        for (Student student : students) {
            if (student.getEmail() != null && studentRepository.existsByEmail(student.getEmail())) {
                throw new IllegalArgumentException("Student with email " + student.getEmail() + " already exists");
            }
            encodePasswordAndNormalizeRole(student);
        }
        return studentRepository.saveAll(students);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByFacultyEmail(String facultyEmail) {
        return studentRepository.findByAssignedFacultyEmailIgnoreCase(facultyEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByDepartment(String department) {
        return studentRepository.findByDepartmentIgnoreCase(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByStatus(String status) {
        return studentRepository.findByStatusIgnoreCase(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> getStudentsByDepartmentAndStatus(String department, String status) {
        return studentRepository.findByDepartmentIgnoreCaseAndStatusIgnoreCase(department, status);
    }

    @Override
    public Student updateStudent(Long id, Student updatedDetails) {
        Student existingStudent = getStudentById(id);

        if (updatedDetails.getName() != null) {
            existingStudent.setName(updatedDetails.getName());
        }
        if (updatedDetails.getEmail() != null
                && !updatedDetails.getEmail().equalsIgnoreCase(existingStudent.getEmail())) {
            if (studentRepository.existsByEmail(updatedDetails.getEmail())) {
                throw new IllegalArgumentException(
                        "Email " + updatedDetails.getEmail() + " is already in use by another student");
            }
            existingStudent.setEmail(updatedDetails.getEmail());
        }
        if (updatedDetails.getAge() > 0) {
            existingStudent.setAge(updatedDetails.getAge());
        }
        if (updatedDetails.getDepartment() != null) {
            existingStudent.setDepartment(updatedDetails.getDepartment());
        }
        if (updatedDetails.getCourses() != null) {
            existingStudent.setCourses(updatedDetails.getCourses());
        }
        if (updatedDetails.getContact() != null) {
            existingStudent.setContact(updatedDetails.getContact());
        }
        if (updatedDetails.getSemester() > 0) {
            existingStudent.setSemester(updatedDetails.getSemester());
        }
        if (updatedDetails.getAcademicYear() != null) {
            existingStudent.setAcademicYear(updatedDetails.getAcademicYear());
        }
        if (updatedDetails.getStatus() != null) {
            existingStudent.setStatus(updatedDetails.getStatus());
        }
        if (isCurrentUserAdmin()) {
            existingStudent.setAssignedFacultyEmail(updatedDetails.getAssignedFacultyEmail() == null
                    || updatedDetails.getAssignedFacultyEmail().isBlank()
                    ? null : updatedDetails.getAssignedFacultyEmail().trim().toLowerCase());
        }
        if (isCurrentUserAdmin() && updatedDetails.getRole() != null && !updatedDetails.getRole().isBlank()) {
            existingStudent.setRole(updatedDetails.getRole().toUpperCase());
        }
        if (updatedDetails.getPassword() != null && !updatedDetails.getPassword().isBlank()) {
            existingStudent.setPassword(passwordEncoder.encode(updatedDetails.getPassword()));
        }

        return studentRepository.save(existingStudent);
    }

    @Override
    public Student updateStudentStatus(Long id, String status) {
        Student student = getStudentById(id);
        student.setStatus(status.toUpperCase());
        return studentRepository.save(student);
    }

    @Override
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        studentRepository.delete(student);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkStudentExists(Long id) {
        return studentRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalStudentCount() {
        return studentRepository.count();
    }

    private void encodePasswordAndNormalizeRole(Student student) {
        if (student.getPassword() != null && !student.getPassword().isBlank()) {
            student.setPassword(passwordEncoder.encode(student.getPassword()));
        }
        if (isCurrentUserAdmin() && student.getRole() != null && !student.getRole().isBlank()) {
            student.setRole(student.getRole().toUpperCase());
        } else {
            student.setRole("STUDENT");
        }
        if (student.getStatus() == null || student.getStatus().isBlank()) {
            student.setStatus("ACTIVE");
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    // ------------------------------------------------------------------
    // Aggregated profile: student + attendance records + result records
    // ------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public StudentProfileDTO getStudentProfile(Long id) {
        // 1. Fetch student from local DB (throws 404 if not found)
        Student student = getStudentById(id);

        // 2. Fetch attendance from Attendenceservice (graceful fallback to empty list)
        List<AttendanceRecordDTO> attendanceRecords;
        try {
            attendanceRecords = attendenceClient.getAttendanceByStudentId(id);
            if (attendanceRecords == null) attendanceRecords = Collections.emptyList();
        } catch (Exception e) {
            attendanceRecords = Collections.emptyList();
        }

        // 3. Fetch results from Resultservice (graceful fallback to empty list)
        List<ResultRecordDTO> resultRecords;
        try {
            resultRecords = resultClient.getResultsByStudentId(id);
            if (resultRecords == null) resultRecords = Collections.emptyList();
        } catch (Exception e) {
            resultRecords = Collections.emptyList();
        }

        return new StudentProfileDTO(student, attendanceRecords, resultRecords);
    }
}
