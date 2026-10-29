# Backend local configuration

Use Java 21 for all backend commands. This project was verified with Java 21.0.13 available at `/opt/homebrew/opt/openjdk@21`; the machine's default `java` may point elsewhere. Run Maven with `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.

The five JWT validators/issuers use the same `JWT_SECRET` property. Local development defaults are available only when `SPRING_PROFILES_ACTIVE=dev`; production must use `SPRING_PROFILES_ACTIVE=prod` and a unique, high-entropy `JWT_SECRET` of at least 32 characters shared by API Gateway, Student, Faculty, Attendance, and Results services. Production startup fails when the value is missing, too short, or the development fallback.

Copy `backend.local.env.example` to `backend.local.env`, enter the local PostgreSQL passwords, then export it before starting services:

```sh
set -a
. ./backend.local.env
set +a
```

The database URLs remain isolated by service: `studentdb`, `facultydb`, `Attendencedb`, and `Resultdb`. Faculty Service listens on port `8085` and owns only `facultydb`. Do not commit the copied environment file or use the development fallback outside local development. `FRONTEND_ORIGIN` defaults to `http://localhost:5173` and controls the Gateway's credential-free CORS allowlist. Do not use `*`.

Faculty TOTP secrets are encrypted with `MFA_ENCRYPTION_KEY`. Production requires a separate stable key of at least 32 characters; preserve it in the deployment secret store and back it up, because changing it makes stored MFA seeds unreadable. The UI uses bearer tokens and does not rely on cookies; CORS remains credential-free.

Start infrastructure in this order: PostgreSQL, Eureka (`8761`), Student (`8082`), Faculty (`8085`), Attendance (`8083`), Results (`8084`), API Gateway (`8080`), and the Vite frontend. Gateway routes cover `/api/auth/**`, `/api/students/**`, `/api/faculty/**`, `/api/attendance/**`, `/api/results/**`, `/api/insights/**`, `/api/notifications/**`, and `/api/assistant/**`.

Attendance thresholds are centralized in Attendance Service and configurable with `ATTENDANCE_EXCELLENT_THRESHOLD`, `ATTENDANCE_GOOD_THRESHOLD`, and `ATTENDANCE_WARNING_THRESHOLD` (defaults: 90, 80, and 75). Values must satisfy `0 <= warning < good < excellent <= 100`. Attendance summaries expose status, risk, exam eligibility, and the configured minimum; dashboard labels and recovery estimates use these server values.

The Student Service owns notifications in `studentdb`. Notification list and read operations are scoped to the authenticated email and role. Attendance, result, faculty create/deactivate, and admin student-create events publish notifications using the caller's JWT over Eureka-discovered Feign calls. Attendance warnings are rate-limited to one per student per seven days. Notifications are enabled in the frontend example; polling reads the authenticated account's unread count every 45 seconds and stops when the shell unmounts on logout.

Student-to-faculty ownership is stored on each student as an assigned faculty email. Admins can set it from the student form; the Student Service checks that the Faculty Service account exists and is active. Faculty directory, attendance, and result reads/writes are scoped to that assignment. Faculty-created student records are assigned to the creating faculty account.

Academic-course ownership follows the same pattern with Course + FacultyCourseAssignment + StudentCourseEnrollment tables in the Student service. Enrollment is the source of truth: a student may only hold one active enrollment per course/semester, and faculty access is derived from the authenticated email plus the course assignment, never from a client-supplied facultyId.

The assistant endpoint is `POST /api/assistant/chat`. It uses a deterministic provider that answers supported academic questions from published results, attendance summaries, and assigned-student records. It does not send student data to an external model. Student questions always resolve to their own record; faculty lookups are limited to assigned students; admin directory questions use admin role authorization. Responses include source labels. The `AssistantProvider` boundary remains available for a future configured model, which must receive only data already authorized for the caller. Never pass user IDs or roles as authority; the backend derives identity and role from the JWT.

An optional development admin can be seeded only under the `dev` profile by setting `EDUPULSE_DEV_ADMIN_EMAIL`, `EDUPULSE_DEV_ADMIN_NAME`, and `EDUPULSE_DEV_ADMIN_PASSWORD`; no default admin password or fabricated account is created. Faculty development seeding likewise requires `FACULTY_DEV_PASSWORD`.

For troubleshooting, first confirm PostgreSQL answers on `5432` and each named database exists with its configured credentials. Then confirm Eureka at `http://localhost:8761`, wait for all service IDs to register, and check the API Gateway at `http://localhost:8080`. If browser requests fail, confirm the exact browser origin matches `FRONTEND_ORIGIN`; do not weaken CORS or bypass JWT validation.
