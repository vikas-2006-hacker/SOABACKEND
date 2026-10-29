package com.klef.soa.faculty.entity;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name="faculty_mfa_challenges")
public class MfaChallenge {
 @Id @Column(length=36) private String id; @Column(nullable=false) private Long facultyId; @Column(nullable=false) private Instant expiresAt; @Column(nullable=false) private int failedAttempts; @Column(length=512) private String pendingSecret;
 public MfaChallenge(){} public MfaChallenge(String id,Long facultyId,Instant expiresAt){this.id=id;this.facultyId=facultyId;this.expiresAt=expiresAt;}
 public String getId(){return id;}public Long getFacultyId(){return facultyId;}public Instant getExpiresAt(){return expiresAt;}public int getFailedAttempts(){return failedAttempts;}public void setFailedAttempts(int v){failedAttempts=v;}public String getPendingSecret(){return pendingSecret;}public void setPendingSecret(String v){pendingSecret=v;}
}
