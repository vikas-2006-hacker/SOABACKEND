package com.klef.soa.sdp.security;

public record MfaSetupResponse(String qrCodeDataUrl, int expiresInSeconds) {}
