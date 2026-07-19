# Authentication Architecture

## Overview

FlowSet auth is a JWT modular-monolith slice under `backend/` with a Next.js client integration.

## Backend packages

| Package | Responsibility |
| --- | --- |
| `auth` | Controllers, DTOs, AuthService use cases |
| `users` | Entities + repositories (users, tokens, profiles) |
| `security` | Spring Security filter chain, JWT filter, UserPrincipal |
| `notifications` | Placeholder email delivery (tokens logged) |
| `exceptions` | Global JSON error envelope |
| `config` | JWT/CORS/cookie/OpenAPI properties |

## Token model

- **Access token:** JWT, 15m default, `Authorization: Bearer`
- **Refresh token:** JWT, 7d default, HttpOnly cookie `flowset_refresh` (path `/api/v1/auth`) + SHA-256 hash in `refresh_tokens`
- Refresh rotation on each `/auth/refresh`

## Endpoints

| Method | Path | Auth |
| --- | --- | --- |
| POST | `/api/v1/auth/register/planner` | Public |
| POST | `/api/v1/auth/register/vendor` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/logout` | Public (cookie) |
| POST | `/api/v1/auth/refresh` | Public (cookie) |
| POST | `/api/v1/auth/forgot-password` | Public |
| POST | `/api/v1/auth/reset-password` | Public |
| POST | `/api/v1/auth/verify-email` | Public (placeholder) |
| POST | `/api/v1/auth/resend-verification` | Public (placeholder) |
| GET | `/api/v1/auth/me` | Bearer |

## Error envelope

```json
{
  "timestamp": "2026-07-19T12:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Email already exists",
  "path": "/api/v1/auth/register/planner",
  "fieldErrors": { "email": "..." }
}
```

## Frontend

- Axios client with credentials + 401 refresh interceptor (`services/api-client.ts`)
- `AuthProvider` restores session via refresh cookie on boot
- Access token kept in memory (Zustand, not persisted)
- Non-secret hint cookies `flowset_session` + `flowset_role` for Next middleware route guards
- Role redirects: planner → `/planner`, vendor → `/vendor`, admin → `/admin`

## Swagger

UI: `http://localhost:8080/swagger-ui.html`

## Run

```bash
docker compose up -d postgres
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend: set `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1` in `.env.local`.
