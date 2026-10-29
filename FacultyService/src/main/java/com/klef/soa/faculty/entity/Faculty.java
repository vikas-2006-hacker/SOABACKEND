package com.klef.soa.faculty.entity;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "faculty")
public class Faculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false, unique = true) private String employeeId;
    @Column(nullable = false) private String department;
    private String designation;
    private String phone;
    @Column(nullable = false, length = 16) private String status = "ACTIVE";
    @JsonIgnore @Column(nullable = false) private String passwordHash;
    @Column(nullable = false) private boolean mfaEnabled;
    @JsonIgnore @Column(length = 512) private String mfaSecretEncrypted;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void insertDates() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updateDate() { updatedAt = LocalDateTime.now(); }
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
    public String getDesignation(){return designation;} public void setDesignation(String v){designation=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public boolean isMfaEnabled(){return mfaEnabled;} public void setMfaEnabled(boolean v){mfaEnabled=v;}
    public String getMfaSecretEncrypted(){return mfaSecretEncrypted;} public void setMfaSecretEncrypted(String v){mfaSecretEncrypted=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
