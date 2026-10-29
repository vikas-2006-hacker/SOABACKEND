package com.klef.soa.sdp.security;

public record MfaChallengeResponse(boolean mfaRequired, boolean setupRequired, String challengeId) {}
