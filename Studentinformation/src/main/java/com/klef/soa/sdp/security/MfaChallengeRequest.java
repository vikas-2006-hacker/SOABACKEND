package com.klef.soa.sdp.security;

import jakarta.validation.constraints.NotBlank;

public class MfaChallengeRequest {
    @NotBlank
    private String challengeId;
    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
}
