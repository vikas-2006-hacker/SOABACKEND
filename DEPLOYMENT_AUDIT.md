# CampusCore deployment preparation audit

Audit date: 2026-10-29. Repository was not initialized as Git when inspected. No remote, infrastructure, or production data was changed.

## Repository and build

- Frontend is React/Vite in `FRONTEND`; `npm run build` passed during this audit.
- Backend consists of six independent Spring Boot Maven projects: `Studentinformation`, `FacultyService`, `Attendenceservice`, `Resultservice`, `ApiGateway`, and `EurekaServer`.
- All six POMs target Java 21. The audit built all six from their own directories with Java 21 and `-DskipTests`; all six package builds passed. Five projects have Maven wrappers. FacultyService has no wrapper. The five checked-in wrapper scripts are not executable, so use `sh mvnw` unless their executable bit is fixed. Wrapper distribution is Maven 3.9.16. FacultyService can use installed Maven (3.9.16 in this environment).
- No shared project module or root Maven aggregator is referenced by the POMs. Each service packages independently. Runtime communication depends on Eureka and Feign; these do not create Maven build dependencies between services.
- The shell's default `java` was 17 while Maven's default runtime was 26. Java 21 exists at `/opt/homebrew/opt/openjdk@21`; all six package builds used that JDK. Java target was not changed.

## Configuration and security findings

- PostgreSQL databases and owners: Student Service → `studentdb`; Faculty Service → `facultydb`; Attendance Service → `Attendencedb`; Result Service → `Resultdb`. Each datasource URL is independently configurable. All services used the same local database username/password in configuration; those credentials have been removed from application configuration and username/password now bind to the existing per-service env names. Supply credentials through the deployment secret store.

| Owner | Database | JDBC URL env | Username env | Password env |
| --- | --- | --- | --- | --- |
| Student Service | `studentdb` | `STUDENT_DATABASE_URL` | `STUDENT_DATABASE_USERNAME` | `STUDENT_DATABASE_PASSWORD` |
| Faculty Service | `facultydb` | `FACULTY_DATABASE_URL` | `FACULTY_DATABASE_USERNAME` | `FACULTY_DATABASE_PASSWORD` |
| Attendance Service | `Attendencedb` | `ATTENDANCE_DATABASE_URL` | `ATTENDANCE_DATABASE_USERNAME` | `ATTENDANCE_DATABASE_PASSWORD` |
| Result Service | `Resultdb` | `RESULT_DATABASE_URL` | `RESULT_DATABASE_USERNAME` | `RESULT_DATABASE_PASSWORD` |

Create a distinct least-privilege login role for each service, scoped to its owned database. JDBC URLs must use `jdbc:postgresql://<private-host>:<port>/<database-name>`.
- Database connection pattern is JDBC PostgreSQL. Local/default configuration uses `spring.jpa.hibernate.ddl-auto=update`; the new `prod` profile switches all four data services to `validate` and disables SQL printing. No Flyway or Liquibase migrations were found. Student, attendance, and result schemas are entity-managed. Faculty also has `FacultyService/db/create_facultydb.sql`; it creates the Faculty database and tables. No schema was dropped, reset, or overwritten. `validate` prevents startup from applying entity-driven DDL, but a reviewed baseline plus versioned migrations are still required before the first production startup.
- Local service ports are Student 8082, Attendance 8083, Results 8084, Faculty 8085, Gateway 8080, and Eureka 8761. Gateway has explicit routes for auth, students, notifications, insights, assistant, courses/enrollment/admin, faculty, attendance, and results; it also enables Eureka discovery-based service lookup. Route definitions remain unchanged.
- JWT signing/validation is shared by Student, Faculty, Attendance, Results, and Gateway through `JWT_SECRET`; `JWT_EXPIRATION_MS` is used by Student. Existing config has a development fallback JWT secret. Replace it with a unique high-entropy value in production, identical across all five services; rotate if the committed repository has ever been shared. Do not use the dev fallback in production.
- Faculty MFA TOTP secrets are encrypted using `MFA_ENCRYPTION_KEY`, falling back to the JWT secret. Set a dedicated stable key in production and back it up securely; changing it without a planned re-encryption breaks decryption of existing MFA seeds. The root `.env.example` documents this existing application setting.
- Spring Boot Actuator is now included in all six services. Only `/actuator/health` is exposed, health component details are hidden, and each endpoint is permitted without changing business endpoint authorization. Java tests verify each endpoint returns `UP` without component details.
- Eureka server is standalone and has self-registration/fetch disabled. Clients currently default to localhost Eureka. Client address configuration was adjusted to allow a deployed hostname via `EUREKA_INSTANCE_HOSTNAME` (or Render's external hostname fallback). On Render, set the advertised instance hostname to each service's private-network hostname and the Eureka URL to Eureka's private host and port. Do not advertise localhost or a container IP; otherwise gateway/Feign discovery cannot reach registered services. Keep services in one Render region.
- No database migrations or production health checks are present. Development seed settings remain opt-in; do not set development profile passwords for public production services.

## Frontend configuration

- Client requests use `VITE_API_BASE_URL` (default `/api`). The production build should keep `/api` and send it to the same-origin API Gateway through a reverse proxy/CDN routing rule. This avoids hard-coding a backend URL into browser bundles and avoids an additional browser CORS origin.
- `VITE_API_GATEWAY_URL` configures only Vite's development proxy. It defaults to localhost:8080 and is ignored by production routing. Set it only when the local gateway differs.
- `VITE_NOTIFICATIONS_ENABLED` and `VITE_EDUPULSE_ASSISTANT_ENABLED` enable those client features unless set to the string `false`.
- `FRONTEND/.env` exists locally and is ignored. Its values were not read or recorded. `FRONTEND/.env.example` is already present. Root `.env.example` now documents the existing frontend keys and backend deployment inputs using placeholders only.

## Git ignore and repository hygiene

- Existing root and module ignore files exclude environment files, Maven `target`, npm dependencies, and frontend build output, but the root did not exclude Eclipse workspace metadata. The root ignore rules now cover nested secret env files (while allowing examples), all `node_modules`, `dist`, `build`, and `target` output, logs, `.metadata`, and common IDE files.
- The repository contains a local Eclipse `.metadata` workspace with logs/history/launch state and a local `FRONTEND/.env`. Both must stay untracked. Inspect staged files and use a secret scanner before the first commit; ignore rules cannot remove secrets already committed or stop secrets copied into tracked source/docs.
- Current app configs previously included a database password and reusable development JWT fallback. Database passwords were removed. The JWT fallback is now restricted to `dev`; `prod` and Render startup require a sufficiently long, non-development key. Treat the former JWT fallback as compromised if it was ever used/shared and rotate it.

## Dockerfiles

Six module-local Dockerfiles now build each service from its own POM and `src` tree using Maven 3.9.16 on Temurin 21, then copy the Spring Boot jar into a Temurin 21 JRE runtime image. FacultyService uses Maven directly because it has no wrapper. Runtime containers use a non-root UID and bind Spring to `0.0.0.0`. The Gateway honors Render's `PORT` (default 10000); other services keep their existing ports for private traffic. Build contexts are the individual module directories, so no root aggregator is required. Each context has a `.dockerignore`. Docker image builds were not executable locally because Docker is not installed; module POM and artifact paths were checked against all six Dockerfiles.

## Render Blueprint

`render.yaml` defines six backend services and a Vite static site. Eureka, Student, Faculty, Attendance, and Results are private services; only the Gateway is public. Render references Eureka's private `hostport` for clients and each service's private `host` for Eureka registration. Do not replace these with localhost, container IPs, or public URLs. Keep these services in one Render region/workspace. The Gateway's public URL is passed to the static frontend at build time; `VITE_API_BASE_URL=/api` remains the API path. Render's static site rewrite sends unknown frontend routes to `/index.html`.

The Blueprint generates a common `JWT_SECRET` for the five JWT services and a separate `MFA_ENCRYPTION_KEY` for Faculty. It does not provision databases. It prompts for the four existing JDBC database URLs and per-database usernames/passwords, plus the exact frontend origin. Enter each database's private host/port and database name in its JDBC URL; use a least-privilege application role for each database. No service route or hostname is hard-coded in the Blueprint.

The Gateway has a Render HTTP health check at `/actuator/health`. All six apps expose Actuator health only, with health details hidden. Protected service health paths are explicitly permitted while business API authorization remains unchanged. Private services retain Render's private-service TCP port probing; `/actuator/health` is available for internal checks.

Render's `FRONTEND_ORIGIN` must be entered as the exact static-site or custom-domain origin (scheme and host, no wildcard). `VITE_API_GATEWAY_URL` is a build-time public Gateway URL, not a secret. No secret is exposed through a `VITE_*` key. Render static-site environment values are build-time inputs. See [Render private networking](https://render.com/docs/private-network), [Blueprint YAML reference](https://render.com/docs/blueprint-spec), and [static-site rewrites](https://render.com/docs/redirects-rewrites).

## Production secrets and data migration

Set `SPRING_PROFILES_ACTIVE=prod` on services. Each JWT service rejects a missing or too-short signing key and the development fallback. The Blueprint's generated shared JWT variable keeps issuer and validators aligned. Faculty also rejects a missing, short, or JWT-identical `MFA_ENCRYPTION_KEY`. Preserve this key across deploys: existing encrypted TOTP seeds cannot be read after a key change. CORS is credential-free and restricted to the manually configured frontend origin. The application uses bearer tokens rather than cookies.

Do not rely on `ddl-auto=update` for production change management. The `prod` profile now sets Hibernate to `validate`, so service startup will fail until each existing schema matches its entities. Back up each existing database, introduce Flyway migrations beginning from a reviewed baseline that matches the live schema, run migrations in a staging copy, then connect production. Do not apply the Faculty creation SQL to an existing database; it is only for a newly created empty Faculty database. No migration was applied and no database was changed by this audit.

## Remaining manual steps before first GitHub push / Render sync

1. Inspect the full diff and verify ignored `.env` files, `.metadata`, generated output, credentials, and local DB files are absent from the files selected for commit. Run a secret scan.
2. Decide whether the four databases will live on one external PostgreSQL cluster or four separately managed instances. Keep their names and ownership as above; enter private JDBC URLs and distinct least-privilege users in Render.
3. Choose a Render region and frontend/custom domain, then set `FRONTEND_ORIGIN` to its exact origin. Keep all private services in the same region.
4. Securely retain a backup of the MFA encryption key before any production faculty MFA setup.
5. Install Docker and build all six Docker contexts locally. The YAML parses locally, but validate `render.yaml` with Render CLI or Blueprint validation before syncing it.
6. All Java tests and frontend tests pass locally (Java tests need the Mockito agent in this sandbox). The frontend production build passes. Before release, inspect the actual Render-built static bundle and verify that its Gateway URL is the generated Render URL and that no secrets are present.
7. Plan and stage Flyway baselines/migrations before connecting any production database.
8. FacultyService still lacks a Maven wrapper; the Dockerfile uses Maven directly. The five existing wrapper scripts are not executable; invoke them with `sh mvnw` or set executable bits in a later cleanup.
9. Git remains uninitialized. No Render resource, GitHub repository, remote, database, or production data has been created or changed.

## Verification performed for this preparation

- Frontend: `npm test` passed (100 tests across 21 files); `npm run build` passed.
- Backend: all six Java 21 `package -DskipTests` builds passed. All Java tests passed: Student 38, Faculty 16, Attendance 10, Results 12, Gateway 1, Eureka 1. Each of the six actuator health endpoints returned `UP` without component details in its test.
- Render/Docker preparation: Ruby parsed `render.yaml` as YAML and a local check confirmed all six Dockerfiles, POMs, source directories, ignored build output, and artifact names match. The Render CLI/schema validator and Docker CLI are unavailable here, so image builds and schema-level Blueprint validation were not performed.
- No live Render networking, external database connection, provisioning, or deployment was tested.
