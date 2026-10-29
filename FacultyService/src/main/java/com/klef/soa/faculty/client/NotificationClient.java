package com.klef.soa.faculty.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "STUDENT-SERVICE", contextId = "facultyNotificationClient")
public interface NotificationClient {
    @PostMapping("/api/notifications/admin-events")
    Map<String, Integer> publishAdminEvent(@RequestBody Map<String, String> event);
}
