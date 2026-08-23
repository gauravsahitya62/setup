# MVP readiness report

Date: 22 August 2026

## Decision

**NOT READY FOR PILOT**

The core marketplace and booking workflows are implemented and covered by automated tests. A controlled pilot still needs a running staging stack and a password-reset path. Those were **not** verified in this environment.

## What works (verified in automated tests)

- Planner and vendor registration, login, refresh, logout, duplicate email, weak password, protected routes, role denial (admin APIs)
- Project create, add products (no inventory reservation), IDOR on another planner’s project
- Marketplace search, product publish, availability (including 100 stock / 70 reserved / 30 remaining)
- Concurrent booking accept: two 60-unit accepts against 100 chairs — one confirms, one conflicts
- Booking request PENDING → vendor accept → inventory reserved → order number → price snapshot (₹18/day × 10 × 3 days = ₹540 remains after catalog price changes to ₹700)
- Delivery, pickup, inventory release, order COMPLETED
- Review 5★, vendor rating update, duplicate review blocked, Verified Booking
- Notifications created on the critical path (non-transactional test)
- File validation and private vs public storage visibility
- Admin authorization tests
- Authenticated planner/vendor dashboards now use live APIs and empty states (no fake revenue, no fake Maya Chen user)

## What does not work / is incomplete

- Outbound email is a queue stub. Users will not receive verification or reset mail.
- Demo seed is off by default and **refused in production**. It is not a substitute for real catalogs.
- Vendor analytics has no historical series (fake charts were removed).
- Client analytics events fire only if `NEXT_PUBLIC_ENABLE_ANALYTICS=true` and there is still no warehouse.

## Known limitations

- No payments. Money on screen is **Estimated rental value** / Gross Booking Value, not collected revenue.
- Rate limiting is in-process.
- Tests use H2, not RDS.
- Docker Compose and AWS (ECS, S3, Secrets Manager, DNS) were not exercised here.

## Features intentionally excluded

- Payments / invoicing / GST e-invoicing
- Native mobile apps
- Multi-region
- Shared Redis rate limits
- Full Playwright UI suite in CI

## Infrastructure readiness

**NOT VERIFIED.** Docker daemon was down. No staging URL was available for `npm run smoke`.

## Security readiness

Application-level authz and IDOR tests pass. Secrets are not committed as production values (`.env.example` uses documented development placeholders). Remaining: live CORS/JWT secrets, email provider, CSP hardening, dependency audit.

## Pilot readiness

Invite real planners only after:

1. Staging frontend + API + Postgres are up, health checks pass, and `npm run smoke` passes against those URLs.
2. Password reset is usable for the cohort (email **or** written operator reset).
3. One operator completes Sharma Wedding → Gold Chairs → booking → accept → order in **Chrome** on that stack.

Onboarding and checklists: [`onboarding.md`](./onboarding.md). Success metrics: [`pilot-success-metrics.md`](./pilot-success-metrics.md).

## Next recommended product changes

None until the three blockers above are done. After that: wire a real email provider, then a metrics sink for the events in [`analytics.md`](./analytics.md).
