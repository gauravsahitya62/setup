# FlowSet Coding Standards

## General

- Production-ready only — no leftover TODOs
- Prefer reuse over new abstractions
- Absolute imports on frontend (`@/*`)
- Feature-by-feature delivery with compile + test after each slice

## Frontend (TypeScript / React)

- React 19 + Next.js 15 App Router
- Zod for runtime validation; React Hook Form for forms
- Zustand for client auth session; TanStack Query for server state when fetching resources
- Never log secrets or passwords
- One API client surface (`services/`) — extend, do not fork

## Backend (Java 21 / Spring Boot)

- Records for DTOs where immutability fits
- Constructor injection only
- Bean Validation (`@Valid`, `@NotBlank`, custom constraints)
- Soft delete via `deleted_at`
- SLF4J logging for security events (never passwords)
- Flyway is the source of truth for schema
- Controllers stay thin; business rules live in services

## Testing

- Unit tests for services / JWT / validators
- Integration / MockMvc tests for auth endpoints
- Frontend typecheck + lint must stay green

## Security

- BCrypt password hashing
- CSRF disabled for JWT Bearer APIs
- CORS allow-list from env
- Role-based method/route authorization
