package com.klef.soa.faculty.controller;
import org.springframework.web.bind.annotation.*;
import com.klef.soa.faculty.dto.*;import com.klef.soa.faculty.service.FacultyMfaService;import jakarta.validation.Valid;
@RestController @RequestMapping("/internal/auth")
public class FacultyAuthController {
 private final FacultyMfaService mfa;public FacultyAuthController(FacultyMfaService mfa){this.mfa=mfa;}
 @PostMapping("/login") public MfaChallengeResponse login(@Valid @RequestBody FacultyLoginRequest request){return mfa.login(request);}
 @PostMapping("/mfa/setup") public MfaSetupResponse setup(@Valid @RequestBody ChallengeRequest request){return mfa.setup(request.challengeId());}
 @PostMapping("/mfa/verify") public FacultyVerifyResponse verify(@Valid @RequestBody CodeRequest request){return mfa.verify(request);}
}
