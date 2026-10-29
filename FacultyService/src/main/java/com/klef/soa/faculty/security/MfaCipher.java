package com.klef.soa.faculty.security;
import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import javax.crypto.*;import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;
@Component public class MfaCipher {
 private final SecretKeySpec key;private static final SecureRandom RANDOM=new SecureRandom();
 public MfaCipher(@Value("${MFA_ENCRYPTION_KEY:${jwt.secret}}")String secret){try{key=new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(("edupulse-faculty-mfa:"+secret).getBytes(StandardCharsets.UTF_8)),"AES");}catch(Exception e){throw new IllegalStateException(e);}}
 public String encrypt(String value){try{byte[] iv=new byte[12];RANDOM.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));byte[] body=c.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=Arrays.copyOf(iv,iv.length+body.length);System.arraycopy(body,0,all,iv.length,body.length);return "v1:"+Base64.getEncoder().encodeToString(all);}catch(Exception e){throw new IllegalStateException(e);}}
 public String decrypt(String value){try{byte[] all=Base64.getDecoder().decode(value.substring(3));Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOf(all,12)));return new String(c.doFinal(Arrays.copyOfRange(all,12,all.length)),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException(e);}}
}
