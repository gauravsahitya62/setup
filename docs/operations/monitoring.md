# Monitoring

Do not buy an enterprise APM for the first pilot unless the team already has one. Start with **CloudWatch + Vercel analytics + (optional) Sentry**.

## Logs

Backend logs are structured enough for grep:

```
timestamp level logger rid=<X-Request-ID> uid=<user uuid> - http method=... path=... status=... durationMs=...
```

Never log passwords, JWTs, refresh cookies, AWS keys, or private file URLs.

Forward `X-Request-ID` from the browser (`services/api-client.ts`) so a support ticket can cite one ID across Vercel and CloudWatch.

## Health

| Endpoint | Meaning |
| --- | --- |
| `/actuator/health/liveness` | Process is up |
| `/actuator/health/readiness` | Process can take traffic (includes DB via Spring default health) |

Do not expose `/actuator/env`, beans, or heapdump. Production exposure is `health` only.

## Recommended CloudWatch signals

- ECS CPU / memory
- ALB 5xx rate and target response time
- RDS `CPUUtilization`, `FreeStorageSpace`, `DatabaseConnections`, `ReadLatency`/`WriteLatency`
- S3 4xx/5xx (if metrics enabled)
- Deployment failed (ECS service event)

Frontend: Vercel error rate and p95 for document routes.

## Error tracking

`lib/observability.ts` is a no-op until `NEXT_PUBLIC_SENTRY_DSN` is a real DSN. Do not invent a Sentry project in git.

## Business-critical alerts (create after infra exists)

| Alert | Suggested condition | Why |
| --- | --- | --- |
| Database unavailable | Readiness failing or RDS connections = 0 for 2 minutes | Bookings and inventory stop |
| Backend unavailable | ALB 5xx > 5% for 5 minutes or healthy host count 0 | Site down |
| High 5xx | 5xx count spike vs 1h baseline | Deploy or query issue |
| Repeated auth failures | `POST /api/v1/auth/login` 401 burst | Credential stuffing |
| Storage failures | Error logs `Failed to store` / S3 5xx | Documents and proofs |
| Inventory reservation failures | Burst of 409 Conflict on booking accept | Double-book risk |
| Booking accept failures | 5xx on vendor accept | Core workflow |

Inventory and booking alerts need log metric filters on `com.flowset` error logs until dedicated metrics are added. That wiring is **external**.

## Dashboard (manual)

A first CloudWatch dashboard should show: request count, 5xx, p95 latency, RDS connections, ECS CPU/memory, auth 401 count. Create it in AWS after the service exists; this repo does not provision dashboards.
