# Security checklist (OWASP-oriented)

This is a living checklist for the FlowSet **pilot**. Items marked *external* cannot be proven from source alone.

## Broken access control

- [x] Method security on admin APIs (`hasRole('ADMIN')`)
- [x] Ownership checks on projects, bookings, orders, reviews, support (IDOR tests in integration tests)
- [x] Private file prefixes require auth + ownership (`StorageAccessService`)
- [x] Public DTOs omit vendor documents
- [x] Frontend middleware is UX-only; API is authoritative

## Injection

- [x] JPA/parameterized queries; no string-concatenated SQL in app code
- [x] Flyway migrations are versioned files, not runtime user SQL
- [x] JSON error handler does not echo SQL or Hibernate internals

## Authentication

- [x] BCrypt passwords
- [x] Access JWT in memory on the client (not `localStorage`)
- [x] Refresh token HttpOnly cookie + rotation + server hash
- [x] Login/register/forgot/reset/verify rate limited
- [x] Suspended users cannot authenticate
- [ ] Production cookie `Secure` + HTTPS *external*

## Cryptographic failures

- [x] JWT secrets required and fail-fast in staging/production if weak/default
- [x] S3 SSE-S3 when `STORAGE_PROVIDER=s3`
- [ ] RDS encryption at rest *external*

## Security misconfiguration

- [x] CORS allow-list (no `*` in staging/production — startup check)
- [x] Swagger disabled on `staging`/`prod` profiles
- [x] Actuator: health only; probes on; details hidden
- [x] `server.error.include-message=never`
- [x] Security headers on API and Next.js
- [ ] Cloudflare TLS and security features *external*

## XSS

- [x] No `dangerouslySetInnerHTML` in the frontend
- [x] SVG/HTML/JS uploads blocked
- [x] Moderate CSP on Next.js (allows `'unsafe-inline'` scripts so the App Router still works)

## Insecure file uploads

- [x] Size limit 10MB
- [x] Extension denylist
- [x] Magic-byte check vs declared MIME
- [x] Path traversal rejected
- [x] Private vs public path split

## Logging / monitoring failures

- [x] Request ID header `X-Request-ID`
- [x] Safe 500 message to clients; stack traces in logs only
- [ ] CloudWatch alarms *external*
- [ ] Sentry DSN *external* (hook present, no fake credentials)

## SSRF

- [x] Uploads are files, not fetched from user-supplied URLs in storage
- [ ] Review any future “import from URL” features before enabling

## Sensitive data exposure

- [x] `.env*` gitignored; `.env.example` has placeholders only
- [x] Production secrets not in the repo
- [ ] Confirm GitHub secret scanning / Dependabot on the remote *external*

## Known follow-ups

- In-memory rate limit is per instance.
- CSP is intentionally not nonce-strict yet (would break Next.js 15 without extra work).
- Planner navbar still uses a placeholder display name (`PLANNER_USER`) — cosmetic, not authorization.
