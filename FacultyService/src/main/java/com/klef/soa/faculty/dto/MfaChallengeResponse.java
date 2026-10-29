package com.klef.soa.faculty.dto;
public record MfaChallengeResponse(boolean mfaRequired,boolean setupRequired,String challengeId){}
