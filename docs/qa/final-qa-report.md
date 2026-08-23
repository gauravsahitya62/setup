# Final QA report

Date: 22 August 2026  
Scope: FlowSet MVP code in this repository (Next.js + Spring Boot). Not a live AWS environment.

## Build status

| Gate | Result |
| --- | --- |
| `npm install` | Pass (11 npm audit findings remain, non-blocking) |
| `npm run lint` | Pass |
| `npm run typecheck` | Pass |
| `npm test` (Vitest) | Pass — 40 tests |
| `npm run build` | Pass |
| `mvn -B test` | Pass — 109 tests (H2 + Flyway V1–V10) |
| `python scripts/validate-flyway.py` | Pass — 10 files V1..V10 |
| Docker image / Compose startup | **NOT VERIFIED** — Docker Desktop engine was not running (`dockerDesktopLinuxEngine` missing) |
| Clean PostgreSQL (non-H2) migrations | **NOT VERIFIED** — tests apply Flyway on in-memory H2 (`MODE=PostgreSQL`) |
| Staging HTTP smoke (`npm run smoke`) | **NOT VERIFIED** — no reachable staging URL in this environment |

## Test status

| Suite | Executed | Passed | Failed | Skipped |
| --- | --- | --- | --- | --- |
| Frontend Vitest | 40 | 40 | 0 | 0 |
| Backend Surefire | 109 | 109 | 0 | 0 |
| **Total automated** | **149** | **149** | **0** | **0** |

After a logging-only change, `AuthIntegrationTest` was re-run: 10/10 passed. That subset is not added to the 149 total.

## E2E status

The critical planner → vendor → order → review path is automated as `PilotCriticalPathIntegrationTest` (MockMvc). It passed.

A Playwright/browser suite against a running UI was **not** added: CI would fail without a live stack. Staging smoke is `npm run smoke` / `scripts/smoke-test.mjs` and must be pointed at real URLs after deploy.

Browser E2E against Chrome/Edge/Firefox: **NOT VERIFIED**.

## Security status

Covered in automated tests: auth register/login/refresh/logout, IDOR on projects/bookings/admin, file magic-byte validation, storage visibility (public logo vs private GST), rate-limit disabled only in `application-test.yml`.

Fixed this pass:

- Email verification / password-reset **tokens no longer logged**
- Registration/login logs no longer include email addresses
- Authenticated dashboards no longer render mock users, fake vendors, or fake **revenue**

Open (not P0/P1 in code):

- No SES/SMTP — verification and reset messages are not delivered
- In-memory rate limiter is per process
- CSP still allows `unsafe-inline` / `unsafe-eval`
- `npm audit`: 11 issues (2 moderate, 9 high)

SQL injection / XSS / CSRF: no new exploit was demonstrated. JWT access tokens stay in memory; refresh is HttpOnly cookie. **NOT VERIFIED:** dedicated Burp/ZAP scan, production CORS, and live JWT secrets.

## Performance findings

No production APM. MockMvc integration tests for marketplace, inventory, bookings, and orders completed in seconds on a developer machine.

**NOT VERIFIED:** marketplace payload size, image CDN, N+1 under production data volumes. No premature optimization was applied. Vendor dashboard now calls inventory overview + orders (one extra round trip vs fake constants).

## Known bugs

See [`bug-report.md`](./bug-report.md).

| Severity | Open | Fixed this pass |
| --- | --- | --- |
| P0 | 0 | 0 confirmed |
| P1 | 0 remaining in app code | QA-001–QA-004 (mocks + token logs) |
| P2 | QA-007 email, QA-008 rate limit, QA-010 Docker, QA-013 analytics sink | QA-005 dead category links |
| P3 | QA-009 CSP, QA-011 devices/a11y, QA-012 npm audit | QA-006 USD, QA-015 email in logs |

## Pilot readiness

**NOT READY FOR PILOT** until the blockers in [`mvp-readiness-report.md`](../pilot/mvp-readiness-report.md) are closed. The booking engine itself is not the blocker: concurrent inventory, price snapshot, fulfillment, and reviews passed in CI-equivalent tests.

## Recommended fixes (minimum)

1. Start Docker Desktop (or deploy staging) and run `npm run smoke` plus one manual Chrome booking.
2. Decide how the cohort resets passwords (email provider **or** operator procedure).
3. Keep `DEMO_SEED_ENABLED=false` on any database that will hold real planner data.

Do not add product features until those are done.
