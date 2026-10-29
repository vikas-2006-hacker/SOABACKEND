package com.klef.soa.sdp.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "STUDENT-SERVICE", contextId = "attendanceNotificationClient")
public interface NotificationClient {
    @PostMapping("/api/notifications/events")
    Map<String, Object> publishStudentEvent(@RequestBody Map<String, Object> event);
}
