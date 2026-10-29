package com.klef.soa.sdp.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.klef.soa.sdp.security.*;
@FeignClient(name="FACULTY-SERVICE", path="/internal/auth")
public interface FacultyAuthClient {
 @PostMapping("/login") MfaChallengeResponse login(@RequestBody LoginRequest request);
 @PostMapping("/mfa/setup") MfaSetupResponse setup(@RequestBody MfaChallengeRequest request);
 @PostMapping("/mfa/verify") FacultyMfaVerifiedResponse verify(@RequestBody MfaVerificationRequest request);
}
