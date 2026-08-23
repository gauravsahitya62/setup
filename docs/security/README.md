# Security

## Authentication

JWT access tokens + HttpOnly refresh cookies. See [`docs/06-authentication.md`](../06-authentication.md).

Suspended users cannot log in. `last_login_at` is recorded on successful login.

## Authorization

Spring Security method security (`@PreAuthorize`). Roles: `PLANNER`, `VENDOR`, `ADMIN`.

- Admin APIs: `hasRole('ADMIN')` on `/api/v1/admin/**`
- Vendor cannot verify themselves or read another vendor’s documents
- Planner/vendor cannot access admin APIs (**403**)
- Support ticket IDOR → **404** for non-owners
- Audit logs are immutable (no PATCH/DELETE)

Frontend role cookies only redirect UX. They are not authorization.

## Passwords

Admins send a reset email through the existing forgot-password flow. Admins never set passwords. Impersonation is not in MVP.

## Rate limiting

In-memory per instance (`RateLimitFilter`). Disabled in the `test` profile.

Protected: login, register, forgot/reset password, email verify/resend, `POST /support/tickets`, `GET /search`, non-GET `/admin/**`.

Not shared across replicas. See `docs/security/security-checklist.md`.

## Files

Vendor document URLs are private. Public vendor DTOs omit `documents`. `StorageAccessService` allows public prefixes without auth; GST/PAN, project files, and delivery proofs require the owner or an admin. Production uses S3 (`STORAGE_PROVIDER=s3`); objects are not public-read.
