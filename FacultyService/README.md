# Faculty Service

Independent Faculty account service. It listens on `8085`, registers as `FACULTY-SERVICE`, and owns the `faculty` table in PostgreSQL database `facultydb`.

Configure `FACULTY_DATABASE_URL`, `FACULTY_DATABASE_USERNAME`, `FACULTY_DATABASE_PASSWORD`, `JWT_SECRET`, and `EUREKA_URL` in the runtime environment. `JWT_SECRET` must match the issuer and contain at least 32 characters; configure that same value for Gateway, Student, Attendance, Result, and Faculty services. Each service now reads it from the environment. Create `facultydb` before starting the service with `db/create_facultydb.sql`. No database or token secrets are stored in this project.

For a development-only seed account, activate the `dev` profile and set `FACULTY_DEV_PASSWORD`; the initializer creates `faculty@gmail.com` only when no account with that email exists. MFA is initialized disabled.

The Gateway routes `/api/faculty/**` through Eureka to this service. Administrative collection and mutation routes require an ADMIN role in the existing signed JWT. `GET /api/faculty/me` is available to FACULTY. `DELETE` performs deactivation so historical references remain valid.

Student Service retains student registration and JWT issuance. For faculty login, it delegates password verification and the full TOTP challenge/setup/verification lifecycle to this service, then issues the shared JWT only after successful MFA verification. Faculty account records and MFA state live only in `facultydb`.
