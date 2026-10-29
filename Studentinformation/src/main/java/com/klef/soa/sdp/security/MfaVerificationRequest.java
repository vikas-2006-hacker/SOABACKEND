package com.klef.soa.sdp.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MfaVerificationRequest {
    @NotBlank
    private String challengeId;
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Code must contain six digits")
    private String code;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
