package com.klef.soa.sdp.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.sdp.service.AssistantProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantProvider provider;
    public AssistantController(AssistantProvider provider) { this.provider = provider; }

    public record ChatRequest(@NotBlank @Size(max = 1000) String message) { }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        if (!provider.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI assistant unavailable: no provider is configured.");
        }
        String role = authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not found"));
        AssistantProvider.AssistantReply reply = provider.answer(authentication.getName(), role, request.message());
        return Map.of("answer", reply.answer(), "sources", reply.sources());
    }
}
