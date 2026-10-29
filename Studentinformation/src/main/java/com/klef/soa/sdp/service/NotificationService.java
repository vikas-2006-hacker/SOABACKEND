package com.klef.soa.sdp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.sdp.dto.NotificationEventRequest;
import com.klef.soa.sdp.entity.Notification;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.NotificationRepository;
import com.klef.soa.sdp.repository.StudentRepository;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final StudentRepository students;

    public NotificationService(NotificationRepository notifications, StudentRepository students) {
        this.notifications = notifications;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<Notification> list(String email, String role) {
        return notifications.findByUserIdIgnoreCaseAndRoleIgnoreCaseOrderByCreatedAtDesc(email, role);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email, String role) {
        return notifications.countByUserIdIgnoreCaseAndRoleIgnoreCaseAndReadAtIsNull(email, role);
    }

    public Notification markRead(Long id, String email, String role) {
        Notification item = notifications.findById(id)
                .filter(n -> n.getUserId().equalsIgnoreCase(email) && n.getRole().equalsIgnoreCase(role))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (item.getReadAt() == null) item.setReadAt(LocalDateTime.now());
        return item;
    }

    public void markAllRead(String email, String role) {
        notifications.findByUserIdIgnoreCaseAndRoleIgnoreCaseAndReadAtIsNull(email, role)
                .forEach(n -> n.setReadAt(LocalDateTime.now()));
    }

    public Notification publishStudentEvent(NotificationEventRequest event) {
        if (event.studentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID is required for this event");
        }
        Student student = students.findById(event.studentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        String type = event.eventType().trim().toUpperCase();
        String category;
        String title;
        String message;
        switch (type) {
            case "RESULT_PUBLISHED" -> {
                category = "RESULTS";
                title = "Result published";
                message = event.courseCode() == null || event.courseCode().isBlank()
                        ? "A new academic result is available in your Results page."
                        : "A new result for " + event.courseCode().trim() + " is available in your Results page.";
            }
            case "ATTENDANCE_UPDATED" -> {
                category = "ATTENDANCE";
                title = "Attendance updated";
                message = "Your attendance record" + courseSuffix(event.courseCode()) + " was updated. "
                        + safeDetail(event.detail());
            }
            case "ATTENDANCE_WARNING" -> {
                category = "ATTENDANCE";
                title = "Attendance needs attention";
                message = "Your attendance needs attention. " + safeDetail(event.detail())
                        + " Review your Attendance page for the latest totals.";
            }
            case "ENROLLMENT_CONFIRMED" -> {
                category = "ACADEMICS";
                title = "Enrollment confirmed";
                message = event.courseCode() == null || event.courseCode().isBlank()
                        ? "Enrollment confirmed for your selected course."
                        : "Enrollment confirmed for " + event.courseCode().trim() + ".";
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notification event");
        }
        if (type.equals("ATTENDANCE_WARNING")) {
            var recent = notifications.findFirstByUserIdIgnoreCaseAndRoleIgnoreCaseAndTitleAndCreatedAtAfterOrderByCreatedAtDesc(
                    student.getEmail(), "STUDENT", title, LocalDateTime.now().minusDays(7));
            if (recent.isPresent()) return recent.get();
        }
        Notification notification = new Notification();
        notification.setUserId(student.getEmail());
        notification.setRole("STUDENT");
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setMessage(message);
        return notifications.save(notification);
    }

    public Notification notifyUser(String userId, String role, String category, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRole(role.toUpperCase());
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setMessage(message);
        return notifications.save(notification);
    }

    public int publishAdminEvent(NotificationEventRequest event) {
        String type = event.eventType().trim().toUpperCase();
        String title = switch (type) {
            case "FACULTY_CREATED" -> "Faculty account created";
            case "FACULTY_DEACTIVATED" -> "Faculty account deactivated";
            case "STUDENT_CREATED" -> "Student record created";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported notification event");
        };
        String message = switch (type) {
            case "FACULTY_CREATED" -> "A faculty account was created: " + safeDetail(event.detail());
            case "FACULTY_DEACTIVATED" -> "A faculty account was deactivated: " + safeDetail(event.detail());
            default -> "A student record was created: " + safeDetail(event.detail());
        };
        String category = type.startsWith("FACULTY_") ? "FACULTY" : "SYSTEM";
        var admins = students.findByRoleIgnoreCase("ADMIN");
        for (Student admin : admins) {
            Notification notification = new Notification();
            notification.setUserId(admin.getEmail());
            notification.setRole("ADMIN");
            notification.setCategory(category);
            notification.setTitle(title);
            notification.setMessage(message);
            notifications.save(notification);
        }
        return admins.size();
    }

    private String courseSuffix(String courseCode) {
        return courseCode == null || courseCode.isBlank() ? "" : " for " + courseCode.trim();
    }

    private String safeDetail(String detail) {
        return detail == null || detail.isBlank() ? "" : detail.trim().substring(0, Math.min(detail.trim().length(), 180));
    }
}
