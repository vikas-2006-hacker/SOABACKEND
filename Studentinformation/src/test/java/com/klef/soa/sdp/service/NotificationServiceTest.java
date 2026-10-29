package com.klef.soa.sdp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.sdp.dto.NotificationEventRequest;
import com.klef.soa.sdp.entity.Notification;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.NotificationRepository;
import com.klef.soa.sdp.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notifications;
    @Mock StudentRepository students;
    @InjectMocks NotificationService service;

    @Test void listsOnlyTheAuthenticatedAccountAndRole() {
        when(notifications.findByUserIdIgnoreCaseAndRoleIgnoreCaseOrderByCreatedAtDesc("a@example.edu", "STUDENT"))
                .thenReturn(List.of());
        assertTrue(service.list("a@example.edu", "STUDENT").isEmpty());
        verify(notifications).findByUserIdIgnoreCaseAndRoleIgnoreCaseOrderByCreatedAtDesc("a@example.edu", "STUDENT");
    }

    @Test void preventsReadingAnotherAccountsNotification() {
        Notification item = new Notification();
        item.setUserId("b@example.edu"); item.setRole("STUDENT");
        when(notifications.findById(2L)).thenReturn(Optional.of(item));
        assertThrows(ResponseStatusException.class, () -> service.markRead(2L, "a@example.edu", "STUDENT"));
    }

    @Test void resultEventUsesStudentIdentityFromTheDatabase() {
        Student student = new Student(); student.setEmail("student@example.edu");
        when(students.findById(9L)).thenReturn(Optional.of(student));
        when(notifications.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));
        Notification result = service.publishStudentEvent(new NotificationEventRequest(9L, "RESULT_PUBLISHED", "CS101", null));
        assertEquals("student@example.edu", result.getUserId());
        assertEquals("STUDENT", result.getRole());
        assertEquals("RESULTS", result.getCategory());
        assertTrue(result.getMessage().contains("CS101"));
    }

    @Test void rateLimitsRepeatedAttendanceWarningsForTheSameStudent() {
        Student student = new Student(); student.setEmail("student@example.edu");
        Notification existing = new Notification(); existing.setTitle("Attendance needs attention");
        when(students.findById(9L)).thenReturn(Optional.of(student));
        when(notifications.findFirstByUserIdIgnoreCaseAndRoleIgnoreCaseAndTitleAndCreatedAtAfterOrderByCreatedAtDesc(
                eq("student@example.edu"), eq("STUDENT"), eq("Attendance needs attention"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existing));
        Notification result = service.publishStudentEvent(new NotificationEventRequest(9L, "ATTENDANCE_WARNING", null, null));
        assertSame(existing, result);
        verify(notifications, never()).save(any());
    }

    @Test void attendanceWarningUsesTheCalculatedThresholdDetail() {
        Student student = new Student(); student.setEmail("student@example.edu");
        when(students.findById(9L)).thenReturn(Optional.of(student));
        when(notifications.findFirstByUserIdIgnoreCaseAndRoleIgnoreCaseAndTitleAndCreatedAtAfterOrderByCreatedAtDesc(
                eq("student@example.edu"), eq("STUDENT"), eq("Attendance needs attention"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(notifications.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        Notification result = service.publishStudentEvent(new NotificationEventRequest(9L, "ATTENDANCE_WARNING", null,
                "Attendance is 72.50% (CRITICAL). The recommended minimum is 70.00%."));

        assertTrue(result.getMessage().contains("72.50% (CRITICAL)"));
        assertTrue(result.getMessage().contains("minimum is 70.00%"));
        assertFalse(result.getMessage().contains("75%"));
    }

    @Test void sendsSystemActivityToConfiguredAdminAccounts() {
        Student admin = new Student(); admin.setEmail("admin@example.edu");
        when(students.findByRoleIgnoreCase("ADMIN")).thenReturn(List.of(admin));
        when(notifications.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));
        int created = service.publishAdminEvent(new NotificationEventRequest(null, "STUDENT_CREATED", null, "New learner"));
        assertEquals(1, created);
        verify(notifications).save(argThat(item -> item.getRole().equals("ADMIN")
                && item.getCategory().equals("SYSTEM") && item.getUserId().equals("admin@example.edu")));
    }
}
