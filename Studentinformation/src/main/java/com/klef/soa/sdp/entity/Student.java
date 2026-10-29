package com.klef.soa.sdp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @Min(value = 15, message = "Age must be at least 15")
    private int age;

    @NotBlank(message = "Department is mandatory")
    @Column(nullable = false)
    private String department;

    private String courses;
    private String contact;

    @Column(length = 254)
    private String assignedFacultyEmail;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(length = 20)
    private String role = "STUDENT"; // STUDENT, FACULTY, ADMIN

    private int semester = 1;
    private String academicYear = "2025-2026";
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, GRADUATED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Student() {
    }

    public Student(Long id, String name, String email, int age, String department,
            String courses, String contact, String password, int semester, String academicYear, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.department = department;
        this.courses = courses;
        this.contact = contact;
        this.password = password;
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCourses() { return courses; }
    public void setCourses(String courses) { this.courses = courses; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getAssignedFacultyEmail() { return assignedFacultyEmail; }
    public void setAssignedFacultyEmail(String assignedFacultyEmail) { this.assignedFacultyEmail = assignedFacultyEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    @jakarta.persistence.PrePersist
    void beforeInsert() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @jakarta.persistence.PreUpdate
    void beforeUpdate() { updatedAt = LocalDateTime.now(); }
    @Override
    public String toString() {
        return "Student [id=" + id + ", name=" + name + ", email=" + email + ", age=" + age
                + ", department=" + department + ", courses=" + courses + ", contact=" + contact
                + ", role=" + role + ", semester=" + semester + ", academicYear=" + academicYear
                + ", status=" + status + "]";
    }
}
