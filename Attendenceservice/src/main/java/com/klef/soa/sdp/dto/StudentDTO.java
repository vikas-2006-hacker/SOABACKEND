package com.klef.soa.sdp.dto;

public class StudentDTO {
    private Long id;
    private String name;
    private String email;
    private int age;
    private String department;
    private String courses;
    private String contact;
    private int semester;
    private String status;

    public StudentDTO() {
    }

    public StudentDTO(Long id, String name, String email, int age, String department,
            String courses, String contact, int semester, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.department = department;
        this.courses = courses;
        this.contact = contact;
        this.semester = semester;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCourses() {
        return courses;
    }

    public void setCourses(String courses) {
        this.courses = courses;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StudentDTO [id=" + id + ", name=" + name + ", email=" + email + ", age="
                + age + ", department=" + department + ", courses=" + courses + ", contact=" + contact + ", semester="
                + semester + ", status=" + status + "]";
    }
}
