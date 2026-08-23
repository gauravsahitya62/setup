# Bug report

Issues found during the FlowSet MVP QA pass (August 2026). Status reflects the code after this pass.

Severity: **P0** data loss / security breach / double booking / production unavailable. **P1** core booking, auth, or order workflow. **P2** important feature. **P3** minor UI.

| ID | Severity | Module | Description | Steps to reproduce | Expected | Actual | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| QA-001 | P1 | Planner shell | Authenticated planner navbar used a hardcoded `PLANNER_USER` (Maya Chen / Northline Events). | Sign in as any planner. Open `/planner`. | Session name and company. | Fake Maya Chen identity. | **Fixed** — `useAuth()`. |
| QA-002 | P1 | Planner dashboard | Activity timeline, recommended vendors, and welcome stats mixed live projects with mock quotes, vendors, and “Open requests: 3”. | Sign in as a new planner with empty data. | Empty or live data. | Fake Atlas/Lumen vendors and activity. | **Fixed** — notifications, `vendorApi.list`, live counts. |
| QA-003 | P1 | Vendor dashboard | KPIs and charts used `VENDOR_KPI` / `VENDOR_REVENUE_SERIES`, including a **Revenue** label and fake month-over-month change. | Sign in as vendor. Open `/vendor`. | Estimated rental value from orders. | Fake USD revenue and utilization. | **Fixed** — inventory overview + order totals; never labeled revenue. |
| QA-004 | P1 | Auth / email | `EmailNotificationService` logged raw verification and password-reset tokens plus recipient email. | Register or request password reset. Read API logs. | Token never in logs. | `token=` and `to=` in INFO logs. | **Fixed** — log line omits recipient and token. |
| QA-005 | P2 | Marketing | Landing category cards linked to invented slugs (`seating`, `linens`) and showed fake product counts (1240…). | Open `/` and click a category. | Real `/categories/{slug}` from the API. | 404 for most cards. | **Fixed** — live category tree, no fake counts. |
| QA-006 | P3 | Marketing | Landing rental cards mapped API prices with `currency: "USD"`. | Open `/` with published INR products. | INR. | USD formatting. | **Fixed**. |
| QA-007 | P2 | Email | No SES/SMTP provider. Verification and reset emails are queued only. | Register and wait for email. | Inbox message with link. | No email delivered. | **Open** — known limitation. Accounts can still sign in (verification is not a login gate). |
| QA-008 | P2 | Rate limit | In-memory `RateLimitFilter` is per process. | Run two API replicas. | Shared limit. | Each replica has its own counters. | **Open** — acceptable for a single-task staging pilot; not for multi-instance production. |
| QA-009 | P3 | CSP | Content-Security-Policy still allows `unsafe-inline` / `unsafe-eval` for Next.js. | Inspect response headers. | Strict CSP. | Relaxed CSP. | **Open** — do not treat as a pilot blocker. |
| QA-010 | P2 | Ops | Docker image build and Compose startup. | `docker compose up`. | Healthy web + API + Postgres. | Depends on Docker Desktop. | **See final QA report** (`NOT VERIFIED` if daemon is down). |
| QA-011 | P3 | UX | Responsive (360/768/1024/1440), Chrome/Edge/Firefox, and full accessibility audit. | Manual device/browser pass. | Usable layouts, keyboard, contrast. | Not executed in this environment. | **Open — NOT VERIFIED**. |
| QA-012 | P3 | Dependencies | `npm audit` reports high/moderate issues in the frontend tree. | `npm audit`. | Clean or accepted exceptions. | Issues remain; CI is non-blocking. | **Open**. |
| QA-013 | P2 | Analytics | Product events are client-side only and no-op unless `NEXT_PUBLIC_ENABLE_ANALYTICS=true`. No warehouse. | Complete a booking with flag off. | Durable product metrics. | Events discarded. | **Open** — definition documented; sink not connected. |
| QA-015 | P3 | Auth logs | Registration/login logs included email addresses. | Register and read logs. | No email in logs. | `email=` on success/failure. | **Fixed**. |

No **P0** defects were confirmed in the booking, inventory, or order engines during this pass. Concurrent accept (100 chairs, two 60-unit accepts) remains covered by `BookingRequestIntegrationTest`.
