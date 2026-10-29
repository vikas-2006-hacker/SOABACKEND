package com.klef.soa.sdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.klef.soa.sdp.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByEmailIgnoreCase(String email);

    List<Student> findByDepartmentIgnoreCase(String department);

    List<Student> findByStatusIgnoreCase(String status);

    List<Student> findByDepartmentIgnoreCaseAndStatusIgnoreCase(String department, String status);

    boolean existsByEmail(String email);

    List<Student> findByRoleIgnoreCase(String role);

    Optional<Student> findByIdAndRoleIgnoreCase(Long id, String role);

    List<Student> findByAssignedFacultyEmailIgnoreCase(String facultyEmail);

    List<Student> findByAssignedFacultyEmailIgnoreCaseAndDepartmentIgnoreCase(String facultyEmail, String department);

    List<Student> findByAssignedFacultyEmailIgnoreCaseAndStatusIgnoreCase(String facultyEmail, String status);

    List<Student> findByAssignedFacultyEmailIgnoreCaseAndDepartmentIgnoreCaseAndStatusIgnoreCase(String facultyEmail, String department, String status);

}
