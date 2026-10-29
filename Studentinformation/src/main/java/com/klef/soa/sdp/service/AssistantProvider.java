package com.klef.soa.sdp.service;

import java.util.List;

/** Implementations must receive only data already authorized for the authenticated principal. */
public interface AssistantProvider {
    AssistantReply answer(String email, String role, String question);
    boolean isAvailable();

    record AssistantReply(String answer, List<String> sources) { }
}
