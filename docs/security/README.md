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

## Rate limiting (future)

Not implemented. When added, start with login, password reset, support creation, and moderation POSTs. Prefer a servlet filter + token bucket; do not rely on the Next.js layer.

## Files

Vendor document URLs are private. Public vendor DTOs omit `documents`. Serve files only to the owner or an admin.
