# Repository inspection report

## Scope and implementation decision

The repository contains the Spring Boot backend in separate service directories and an existing but empty `FRONTEND/` directory. The frontend was scaffolded in that directory; no backend service, route, DTO, authentication mechanism, or database configuration was changed.

## Backend findings

| Area | Verified implementation |
|---|---|
| API Gateway | Port `8080`; routes `/api/auth/**` and `/api/students/**` to Student Service, `/api/attendance/**` to Attendance Service, and `/api/results/**` to Result Service. The gateway uses Eureka service IDs for routing. |
| Auth | `POST /api/auth/login`; request `{ "email": string, "password": string }`; response `{ "token": string, "tokenType": "Bearer", "email": string, "role": string }`. Login is the only public application API. |
| Token | JWT bearer token; configured expiration is 86,400,000 ms (24 hours). Protected requests must send `Authorization: Bearer <token>`. |
| Roles | `ADMIN`, `FACULTY`, `STUDENT`. Student creation and staff modules are restricted to staff roles; deletion of student records is Admin-only; attendance and result writes are Faculty-only. |
| Registration | No registration endpoint was found in the Auth Controller or Gateway routes. The frontend therefore does not offer self-registration. |
| Students | Base `/api/students`; list and create, details, profile, email/department lookup, update, status update, delete, and count routes. The entity includes `id`, `name`, `email`, `age`, `department`, `courses`, `contact`, `role`, `semester`, `academicYear`, and `status`; password is write-only in JSON. |
| Attendance | Base `/api/attendance`; individual and bulk create, list/details, per-student records and summary, course/date queries, shortage report, update, and delete. Status values called out in the DTO are `PRESENT`, `ABSENT`, `LATE`, and `EXCUSED`. |
| Results | Base `/api/results`; create/batch, list/details, student results, semester summary, CGPA, transcript, update, and delete. Results use internal marks from 0–40 and external marks from 0–60; total, grade, points, and result status are backend-owned. |
| Eureka | Eureka Server uses port `8761`; the browser must not communicate with it. |

### Integration caveat

The Gateway configuration has no global CORS policy. Its global JWT filter also requires a bearer token for every path except the configured public paths, which does not include unauthenticated `OPTIONS` preflight requests. The frontend therefore uses a Vite development proxy (`/api` → `http://localhost:8080`) so development requests are same-origin and still pass through the Gateway. Production deployment must use a same-origin reverse proxy or coordinate a narrowly scoped Gateway CORS/OPTIONS change; no backend compatibility changes were made in this phase.

## Phase 2 and 3 implementation

- Login uses `POST /api/auth/login` through the Axios client and Gateway proxy. Request/response fields follow `LoginRequest` and `LoginResponse`.
- JWT, email, and role persist across refreshes; passwords are not stored. Expired/malformed tokens are cleared on load, active-session expiry is scheduled, and API `401` responses clear auth state and redirect through the protected route.
- The backend has no auth/session validation endpoint. A future-expiry token with an invalid signature cannot be identified by the browser alone; the Gateway rejects it on the next protected API call, and the `401` interceptor then clears the session.
- The frontend recognizes the actual roles (`ADMIN`, `FACULTY`, `STUDENT`). Staff navigation is shown only to Admin/Faculty; student-management deletion is shown only to Admin. Student accounts only see the Dashboard because the backend has no self-lookup endpoint to safely map the logged-in email to a student ID for My Profile/Attendance/Results.
- Registration is omitted because there is no backend endpoint.
- Student list/create/read/profile/update/delete use the verified Gateway routes in the frontend README. The list filters out seeded Faculty/Admin records by role and performs client-side search and pagination because the backend has neither role-filtered listing nor pagination.
- Student form fields and validation follow the entity: name, email, department, age >= 15, semester >= 1, courses, contact, academic year, status, and optional initial password. Role is not editable in this student-focused UI.
- Student profile renders attendance/result DTO data returned by the existing profile endpoint. No mock values or records are used by the application.

## Verification

- `npm test` — passed, 15 tests across auth, API handling, roles, student list, create, update, delete, and profile.
- `npm run lint` — passed with no warnings.
- `npm run build` — passed (TypeScript check and Vite production build).
- Browser login attempt — the form submitted the expected request through the Vite `/api` proxy, but Vite received `502 Bad Gateway`; browser console showed the corresponding failed request. At verification time `localhost:8080` (Gateway), `8761` (Eureka), and service ports `8082`–`8084` were not listening; PostgreSQL `5432` was listening. No real JWT response was received, so live login/dashboard/student CRUD could not be verified.
- Attempted to start Eureka through the Java debugger, which reported a started session, but the subsequent session check showed no active Java debug session and port `8761` remained closed. Per Java launch troubleshooting guidance, no automatic relaunch or terminal workaround was attempted.

## Remaining integration gate

The frontend implementation is ready for backend integration, but Phase 2/3 end-to-end acceptance remains blocked until Eureka, Student Service, and the API Gateway are started and the seeded/valid account can authenticate. Once they are available, verify the actual login, authorized student list, create/view/update/delete cycle, and role restrictions through the browser. Do not proceed to Attendance/Results implementation until that flow passes.
