package com.klef.soa.sdp.repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import com.klef.soa.sdp.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdIgnoreCaseAndRoleIgnoreCaseOrderByCreatedAtDesc(String userId, String role);
    long countByUserIdIgnoreCaseAndRoleIgnoreCaseAndReadAtIsNull(String userId, String role);
    List<Notification> findByUserIdIgnoreCaseAndRoleIgnoreCaseAndReadAtIsNull(String userId, String role);
    java.util.Optional<Notification> findFirstByUserIdIgnoreCaseAndRoleIgnoreCaseAndTitleAndCreatedAtAfterOrderByCreatedAtDesc(String userId, String role, String title, LocalDateTime since);
}
