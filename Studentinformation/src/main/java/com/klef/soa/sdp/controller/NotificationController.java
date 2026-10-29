package com.klef.soa.sdp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.sdp.dto.NotificationEventRequest;
import com.klef.soa.sdp.entity.Notification;
import com.klef.soa.sdp.service.NotificationService;
import com.klef.soa.sdp.security.StudentAccessPolicy;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final StudentAccessPolicy accessPolicy;

    public NotificationController(NotificationService service, StudentAccessPolicy accessPolicy) {
        this.service = service;
        this.accessPolicy = accessPolicy;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Notification> list(Authentication authentication) {
        return service.list(authentication.getName(), role(authentication));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> unreadCount(Authentication authentication) {
        return Map.of("count", service.unreadCount(authentication.getName(), role(authentication)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public Notification markRead(@PathVariable Long id, Authentication authentication) {
        return service.markRead(id, authentication.getName(), role(authentication));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> markAllRead(Authentication authentication) {
        service.markAllRead(authentication.getName(), role(authentication));
        return Map.of("updated", true);
    }

    /** Private service-to-service event API; the caller JWT remains the authorization source. */
    @PostMapping("/events")
    @PreAuthorize("hasRole('FACULTY')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public Notification publishEvent(@Valid @RequestBody NotificationEventRequest request) {
        accessPolicy.checkStudentAccess(request.studentId());
        return service.publishStudentEvent(request);
    }

    @PostMapping("/admin-events")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> publishAdminEvent(@Valid @RequestBody NotificationEventRequest request) {
        return Map.of("created", service.publishAdminEvent(request));
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not found"));
    }
}
