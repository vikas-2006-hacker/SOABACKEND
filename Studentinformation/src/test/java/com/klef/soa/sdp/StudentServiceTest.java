//package com.klef.soa.sdp;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.util.List;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.klef.soa.sdp.entity.Student;
//import com.klef.soa.sdp.repository.StudentRepository;
//import com.klef.soa.sdp.service.StudentService;
//
//@SpringBootTest
//@Transactional
//class StudentServiceTest {
//
//    @Autowired
//    private StudentService studentService;
//
//    @Autowired
//    private StudentRepository studentRepository;
//
//    @BeforeEach
//    void setUp() {
//        studentRepository.deleteAll();
//    }
//
//    @Test
//    void testCreateAndGetStudent() {
//        Student student = new Student();
//        student.setStudentId("EDU2026001");
//        student.setName("Alice Walker");
//        student.setEmail("alice@edupulse.edu");
//        student.setAge(20);
//        student.setDepartment("Computer Science");
//        student.setCourses("Data Structures, Algorithms");
//        student.setContact("+1-555-0101");
//        student.setSemester(3);
//
//        Student created = studentService.createStudent(student);
//        assertNotNull(created.getId());
//        assertEquals("Alice Walker", created.getName());
//        assertEquals("ACTIVE", created.getStatus());
//
//        Student retrieved = studentService.getStudentById(created.getId());
//        assertEquals("alice@edupulse.edu", retrieved.getEmail());
//
//        assertTrue(studentService.checkStudentExists(created.getId()));
//    }
//
//    @Test
//    void testUpdateAndStatusChange() {
//        Student student = new Student();
//        student.setName("Bob Dylan");
//        student.setEmail("bob@edupulse.edu");
//        student.setAge(22);
//        student.setDepartment("Electronics");
//        Student created = studentService.createStudent(student);
//
//        Student updateDetails = new Student();
//        updateDetails.setDepartment("AI & Data Science");
//        Student updated = studentService.updateStudent(created.getId(), updateDetails);
//        assertEquals("AI & Data Science", updated.getDepartment());
//
//        Student statusUpdated = studentService.updateStudentStatus(created.getId(), "GRADUATED");
//        assertEquals("GRADUATED", statusUpdated.getStatus());
//    }
//
//    @Test
//    void testFindByDepartment() {
//        Student s1 = new Student(null, "EDU001", "John", "john@edu.com", 20, "CSE", "CS101", "123", "pass", 1, "2025-2026", "ACTIVE");
//        Student s2 = new Student(null, "EDU002", "Jane", "jane@edu.com", 21, "ECE", "EC101", "456", "pass", 1, "2025-2026", "ACTIVE");
//        studentService.createStudent(s1);
//        studentService.createStudent(s2);
//
//        List<Student> cseStudents = studentService.getStudentsByDepartment("CSE");
//        assertEquals(1, cseStudents.size());
//        assertEquals("John", cseStudents.get(0).getName());
//    }
//}
