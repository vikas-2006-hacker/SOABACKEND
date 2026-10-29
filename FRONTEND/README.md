# CampusCore frontend

CampusCore is the Smart Academic Management Platform frontend, built with React, TypeScript, and Vite for the existing Spring Boot School Management System. This application calls the Spring Boot API Gateway; it does not call Eureka or individual microservices directly.

## Requirements

- Node.js and npm
- API Gateway at `http://localhost:8080`
- Eureka and the backend services running and registered with Eureka

## Run locally

```sh
npm install
npm run dev
```

The Vite development server proxies `/api` requests to the Gateway. Configure a different Gateway address with `VITE_API_GATEWAY_URL` in a local `.env` file. The browser-facing API prefix can be changed using `VITE_API_BASE_URL`. See `.env.example`.

The Gateway currently has no global CORS/unauthenticated OPTIONS handling. Keep the Vite proxy for local development; production hosting should provide a same-origin reverse proxy or a Gateway CORS configuration.

## Authentication and supported access

- Sign in uses `POST /api/auth/login` with `{ email, password }`.
- The backend returns a bearer JWT, email, and one of `ADMIN`, `FACULTY`, or `STUDENT`.
- The JWT is persisted for refreshes, attached to API calls, and removed on logout, expiry, or an API `401`.
- There is no registration endpoint in the backend, so self-registration is not shown.
- Students can access the dashboard. Admin and Faculty roles can access student, attendance, and results areas; student-management delete controls are Admin-only.

## Student management

The student directory uses the backend entity’s `STUDENT` role records and searches the fetched list locally. Client-side pagination is used because the backend list endpoint does not provide pagination.

| Action | Gateway endpoint |
|---|---|
| List | `GET /api/students` |
| Read | `GET /api/students/{id}` |
| Profile with available attendance/results | `GET /api/students/{id}/profile` |
| Create | `POST /api/students` |
| Update | `PUT /api/students/{id}` |
| Delete (Admin only) | `DELETE /api/students/{id}` |

The form validates the backend’s required name, email, department, and minimum age (15), plus semester minimum (1). Password is optional in the backend entity and is only submitted when an initial password is entered.

## Validation

```sh
npm run lint
npm test
npm run build
```
