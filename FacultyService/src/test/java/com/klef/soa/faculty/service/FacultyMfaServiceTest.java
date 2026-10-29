package com.klef.soa.faculty.service;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.time.Instant;import java.util.Optional;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;import org.springframework.security.crypto.password.PasswordEncoder;
import com.klef.soa.faculty.dto.CodeRequest;import com.klef.soa.faculty.entity.*;import com.klef.soa.faculty.repository.*;import com.klef.soa.faculty.security.MfaCipher;import com.warrenstrange.googleauth.GoogleAuthenticator;
@ExtendWith(MockitoExtension.class)
class FacultyMfaServiceTest {
 @Mock FacultyRepository facultyRepository;@Mock MfaChallengeRepository challengeRepository;@Mock PasswordEncoder encoder;MfaCipher cipher=new MfaCipher("FacultyServiceTestEncryptionKey2026");
 private FacultyMfaService service(){return new FacultyMfaService(facultyRepository,challengeRepository,encoder,cipher);}
 private static Faculty faculty(){Faculty f=new Faculty();f.setId(12L);f.setEmail("faculty@school.edu");f.setStatus("ACTIVE");return f;}
 private static MfaChallenge challenge(){MfaChallenge c=new MfaChallenge("id",12L,Instant.now().plusSeconds(300));c.setPendingSecret("encrypted");return c;}
 @Test void validTotpEnrollsFacultyAndConsumesChallenge(){Faculty f=faculty();MfaChallenge c=challenge();String secret="JBSWY3DPEHPK3PXP";c.setPendingSecret(cipher.encrypt(secret));when(challengeRepository.lock("id")).thenReturn(Optional.of(c));when(facultyRepository.findById(12L)).thenReturn(Optional.of(f));int code=new GoogleAuthenticator().getTotpPassword(secret);
  var result=service().verify(new CodeRequest("id",String.format("%06d",code)));
  assertEquals("SUCCESS",result.status());assertTrue(f.isMfaEnabled());assertEquals(c.getPendingSecret(),f.getMfaSecretEncrypted());assertEquals(secret,cipher.decrypt(f.getMfaSecretEncrypted()));verify(facultyRepository).save(f);verify(challengeRepository).delete(c);
 }
 @Test void invalidCodesInvalidateChallengeAfterFiveAttempts(){Faculty f=faculty();MfaChallenge c=challenge();String secret="JBSWY3DPEHPK3PXP";c.setPendingSecret(cipher.encrypt(secret));int actual=new GoogleAuthenticator().getTotpPassword(secret);int wrong=actual==111111?222222:111111;when(challengeRepository.lock("id")).thenReturn(Optional.of(c));when(facultyRepository.findById(12L)).thenReturn(Optional.of(f));
  for(int i=0;i<4;i++)assertEquals("INVALID",service().verify(new CodeRequest("id",String.format("%06d",wrong))).status());
  assertEquals("RATE_LIMITED",service().verify(new CodeRequest("id",String.format("%06d",wrong))).status());verify(challengeRepository).delete(c);verify(facultyRepository,never()).save(any());
 }
 @Test void expiredChallengeIsRejectedAndRemoved(){MfaChallenge c=new MfaChallenge("id",12L,Instant.now().minusSeconds(1));when(challengeRepository.lock("id")).thenReturn(Optional.of(c));assertEquals("EXPIRED",service().verify(new CodeRequest("id","123456")).status());verify(challengeRepository).delete(c);}
}
