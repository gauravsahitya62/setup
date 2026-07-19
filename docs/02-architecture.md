# FlowSet Architecture

## Style

Modular monolith (backend) + Next.js App Router (frontend).

```
Frontend (Next.js)  --HTTPS/JSON-->  Backend (Spring Boot)
                                         |
                                    PostgreSQL
```

## Backend layout

```
backend/
  src/main/java/com/flowset/
    auth/           # Auth use cases, controllers, DTOs
    users/          # User aggregate, profiles
    security/       # JWT, filters, SecurityFilterChain
    config/         # CORS, OpenAPI, app properties
    common/         # Shared primitives, enums
    exceptions/     # Problem responses, global advice
    notifications/  # Email placeholders
    planner/        # Planner-domain (future)
    vendor/         # Vendor-domain (future)
    admin/          # Admin-domain (future)
    shared/         # Cross-cutting DTOs / utils
```

### Layering (strict)

Controller → Service → Repository → Database

Controllers never inject repositories.

## Frontend layout

Existing folders under `app/`, `components/`, `services/`, `providers/`, `store/`. Reuse `api-client` patterns; do not duplicate domain UI.

## Auth token strategy

| Token | Lifetime (default) | Storage |
| --- | --- | --- |
| Access JWT | 15 minutes | Frontend memory (Zustand, non-persisted) |
| Refresh JWT | 7 days | HttpOnly Secure SameSite cookie + server-side hash row |

## API versioning

All HTTP APIs under `/api/v1`.

## Configuration

No secrets in source. JWT secrets, DB credentials, and CORS origins come from environment variables.
