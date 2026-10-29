package com.klef.soa.faculty.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.klef.soa.faculty.entity.Faculty;
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByEmailIgnoreCase(String email);
    Optional<Faculty> findByEmployeeIdIgnoreCase(String employeeId);
    List<Faculty> findByStatusIgnoreCase(String status);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmployeeIdIgnoreCase(String employeeId);
}
