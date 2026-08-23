# Operations runbook

## Start of day (pilot)

1. Vercel deployment is Ready.
2. `GET https://api.flowset.in/actuator/health/readiness` (placeholder host) returns 200.
3. No open Sev-1 in CloudWatch / Sentry.

## Deploy

See `docs/deployment/deployment-guide.md`. Production is **manual** (`workflow_dispatch` + GitHub Environment approval).

## Incident: API down

1. Check ECS running count and ALB healthy hosts.
2. Check RDS status.
3. Read last 15 minutes of API logs; note `rid=`.
4. Rollback image if the last deploy correlates. `docs/deployment/rollback.md`.

## Incident: cannot log in

1. Confirm CORS origin matches the exact frontend origin (scheme + host + port).
2. Confirm `COOKIE_SECURE=true` and HTTPS on API.
3. Confirm `SameSite=None` if app and API are on different sites.
4. Rate limit: 429 with `Retry-After`. In-memory limiter resets per task after ~1 minute.

## Incident: booking accept / inventory

1. Look for HTTP 409 — another reservation won. Ask the vendor to retry.
2. If 500, check stack traces in logs (not returned to clients).
3. Do not edit `inventory` rows by hand. Prefer compensating API actions.

## Incident: file upload failed

1. Confirm MIME + extension + magic bytes (executables and SVG are rejected).
2. Confirm size ≤ 10MB.
3. Production: task role can `s3:PutObject` on the bucket.

## Support tickets

Users create tickets via `/api/v1/support/tickets` (rate limited). Admins use `/admin/support`. Do not delete `audit_logs`.

## Contacts

Fill after the pilot team is named. Do not invent on-call rotations in git.
