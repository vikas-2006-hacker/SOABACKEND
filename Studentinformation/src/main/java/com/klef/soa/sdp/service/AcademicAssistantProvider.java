package com.klef.soa.sdp.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.klef.soa.sdp.dto.AcademicInsightsDTO;
import com.klef.soa.sdp.entity.Student;
import com.klef.soa.sdp.repository.StudentRepository;
import com.klef.soa.sdp.security.StudentAccessPolicy;

/** Deterministic assistant for academic questions. It never sends student data to an external model. */
@Component
public class AcademicAssistantProvider implements AssistantProvider {
    private static final Pattern ID_PATTERN = Pattern.compile("\\bstudent\\s*(?:id\\s*)?#?([0-9]{1,18})\\b", Pattern.CASE_INSENSITIVE);
    private final StudentRepository students;
    private final AcademicInsightService insights;
    private final StudentAccessPolicy accessPolicy;

    public AcademicAssistantProvider(StudentRepository students, AcademicInsightService insights,
            StudentAccessPolicy accessPolicy) {
        this.students = students;
        this.insights = insights;
        this.accessPolicy = accessPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantReply answer(String email, String role, String question) {
        String normalizedRole = role == null ? "" : role.toUpperCase(Locale.ROOT);
        String query = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);

        if ("FACULTY".equals(normalizedRole) && asksForAssignedCount(query)) {
            long count = students.findByAssignedFacultyEmailIgnoreCase(email).stream()
                    .filter(student -> "STUDENT".equalsIgnoreCase(student.getRole())).count();
            return reply("You have " + count + " assigned student" + (count == 1 ? "" : "s") + ".", "faculty assignments");
        }
        if ("ADMIN".equals(normalizedRole) && asksForStudentCount(query)) {
            long count = students.findByRoleIgnoreCase("STUDENT").size();
            return reply("There are " + count + " student accounts in the directory.", "student directory");
        }

        Student target = resolveTarget(email, normalizedRole, question);
        if (target == null) {
            String instruction = "FACULTY".equals(normalizedRole)
                    ? "Name an assigned student or include their student ID so I can check their academic records."
                    : "Name a student or include their student ID so I can check that student's academic records.";
            return reply(instruction);
        }
        accessPolicy.checkStudentAccess(target.getId());
        AcademicInsightsDTO record = insights.forStudentId(target.getId());
        return answerFromRecords(record, query);
    }

    private Student resolveTarget(String email, String role, String question) {
        if ("STUDENT".equals(role)) {
            return students.findByEmail(email)
                    .filter(student -> "STUDENT".equalsIgnoreCase(student.getRole()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        }
        List<Student> visible = "FACULTY".equals(role)
                ? students.findByAssignedFacultyEmailIgnoreCase(email).stream()
                    .filter(student -> "STUDENT".equalsIgnoreCase(student.getRole())).toList()
                : "ADMIN".equals(role) ? students.findByRoleIgnoreCase("STUDENT") : List.of();
        if (visible.isEmpty()) return null;

        Matcher idMatcher = ID_PATTERN.matcher(question == null ? "" : question);
        if (idMatcher.find()) {
            long requestedId;
            try { requestedId = Long.parseLong(idMatcher.group(1)); }
            catch (NumberFormatException ignored) { return null; }
            return visible.stream().filter(student -> Objects.equals(student.getId(), requestedId)).findFirst().orElse(null);
        }
        String normalizedQuestion = question == null ? "" : question.toLowerCase(Locale.ROOT);
        List<Student> matches = visible.stream()
                .filter(student -> student.getName() != null && !student.getName().isBlank())
                .filter(student -> normalizedQuestion.contains(student.getName().toLowerCase(Locale.ROOT)))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private AssistantReply answerFromRecords(AcademicInsightsDTO record, String query) {
        AcademicInsightsDTO.Attendance attendance = record.attendance();
        AcademicInsightsDTO.Performance performance = record.performance();
        if (asksAttendance(query)) {
            if (attendance == null) return reply("I couldn’t retrieve attendance records just now. Please try again.", "attendance service unavailable");
            if (attendance.totalClasses() == 0) return reply("There are no attendance records for " + record.studentName() + " yet.", "attendance records");
            return reply(String.format(Locale.ROOT, "%s's recorded attendance is %.2f%%: %d of %d classes attended, with %d absences. Status: %s; risk: %s.",
                    record.studentName(), attendance.percentage(), attendance.classesAttended(), attendance.totalClasses(),
                    attendance.classesAbsent(), attendance.status(), attendance.risk()), "attendance summary");
        }
        if (asksCredits(query)) {
            if (performance == null || performance.creditsRegistered() == null) return reply("Credit totals aren’t available in the current academic record.", "transcript unavailable");
            return reply(record.studentName() + " has earned " + value(performance.creditsEarned())
                    + " of " + performance.creditsRegistered() + " registered credits.", "academic transcript");
        }
        if (asksStrongest(query)) {
            if (performance == null || performance.strongestSubject() == null) return reply("No published subject results are available to compare yet.", "published results");
            var subject = performance.strongestSubject();
            return reply(String.format(Locale.ROOT, "%s is %s's strongest recorded subject, with %.1f grade points and %.2f marks.",
                    subject.courseName(), record.studentName(), subject.gradePoints(), subject.marks()), "published results");
        }
        if (asksFocus(query)) {
            if (performance == null || performance.focusSubject() == null) return reply("There isn’t enough published result data to suggest a subject for extra review yet.", "published results");
            var subject = performance.focusSubject();
            return reply(String.format(Locale.ROOT, "Based on published results, %s has the lowest recorded grade points for %s (%.1f). You could consider giving it extra review time.",
                    subject.courseName(), record.studentName(), subject.gradePoints()), "published results");
        }
        if (asksLatestResult(query)) {
            if (performance == null || performance.latestResult() == null) return reply("There are no published results in the current academic record yet.", "published results");
            var result = performance.latestResult();
            return reply(String.format(Locale.ROOT, "The latest recorded result is %s (%s, Semester %d): %.2f marks, grade %s, %d credits, status %s.",
                    result.courseName(), result.courseCode(), result.semester(), result.marks(), result.grade(),
                    result.credits(), result.resultStatus()), "published results");
        }
        if (asksGpa(query)) {
            if (performance == null || performance.cumulativeGpa() == null) return reply("CGPA isn’t available in the current transcript.", "transcript unavailable");
            return reply(String.format(Locale.ROOT, "%s's current CGPA is %.2f. Academic standing: %s.",
                    record.studentName(), performance.cumulativeGpa(), performance.standing()), "academic transcript");
        }
        if (asksPerformance(query)) {
            StringBuilder answer = new StringBuilder("Here is the current recorded academic overview for ")
                    .append(record.studentName()).append(": ");
            boolean hasData = false;
            if (attendance != null && attendance.totalClasses() > 0) {
                answer.append(String.format(Locale.ROOT, "attendance %.2f%% (%s); ", attendance.percentage(), attendance.status()));
                hasData = true;
            }
            if (performance != null && performance.cumulativeGpa() != null) {
                answer.append(String.format(Locale.ROOT, "CGPA %.2f, standing %s; ", performance.cumulativeGpa(), performance.standing()));
                hasData = true;
            }
            if (!hasData) return reply("There isn’t enough academic data available to summarize this record yet.", "academic records");
            answer.setLength(answer.length() - 2);
            return reply(answer.toString(), "academic records");
        }
        return reply("I can answer questions about attendance, CGPA, credits, strongest subjects, results, and where published results suggest extra review may help.");
    }

    private boolean asksAttendance(String query) { return query.contains("attendance") || query.contains("classes attended") || query.contains("absent"); }
    private boolean asksCredits(String query) { return query.contains("credit"); }
    private boolean asksStrongest(String query) { return query.contains("strongest") || query.contains("best subject") || query.contains("highest mark"); }
    private boolean asksFocus(String query) { return query.contains("focus") || query.contains("improve") || query.contains("study") || query.contains("weakest"); }
    private boolean asksLatestResult(String query) { return query.contains("latest") || query.contains("recent result"); }
    private boolean asksGpa(String query) { return query.contains("cgpa") || query.matches(".*\\bgpa\\b.*"); }
    private boolean asksPerformance(String query) { return query.contains("perform") || query.contains("overall") || query.contains("academic standing"); }
    private boolean asksForAssignedCount(String query) { return query.contains("assigned") && query.contains("student"); }
    private boolean asksForStudentCount(String query) { return query.contains("how many") && query.contains("student"); }
    private String value(Integer number) { return number == null ? "0" : number.toString(); }
    private AssistantReply reply(String answer, String... sources) { return new AssistantReply(answer, List.of(sources)); }
    @Override public boolean isAvailable() { return true; }
}
