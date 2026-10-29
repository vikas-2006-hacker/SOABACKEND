package com.klef.soa.sdp.service;

public class UnavailableAssistantProvider implements AssistantProvider {
    @Override public AssistantReply answer(String email, String role, String question) {
        throw new IllegalStateException("No AI provider is configured.");
    }
    @Override public boolean isAvailable() { return false; }
}
