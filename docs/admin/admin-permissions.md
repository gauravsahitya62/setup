# Admin permissions

Only the `ADMIN` role can operate the Admin Portal.

## Roles

| Role | Admin APIs | Notes |
| --- | --- | --- |
| `ADMIN` | Allowed | Spring Security `hasRole('ADMIN')` on every `/api/v1/admin/**` controller. |
| `PLANNER` | Forbidden | Frontend middleware redirects `/admin` to `/planner`. APIs still return **403**. |
| `VENDOR` | Forbidden | Frontend middleware redirects `/admin` to `/vendor`. APIs still return **403**. |

Never trust `role`, `admin_id`, `vendor_id`, or `planner_id` from the request body. Identity always comes from the JWT (`UserPrincipal`).

## What admins can do

- Dashboard and date-window trends
- User suspend / reactivate / password-reset email
- Vendor verification (start review, verify, reject, suspend, reactivate, feature)
- Document approve / reject / request resubmission
- Product approve / reject / suspend / restore / feature
- Category create / edit / archive / reorder / feature / image
- Review hide / restore / remove / note
- Support assign / reply / resolve / close
- Typed platform settings
- Read-only audit log and reports

## What admins cannot do

- Impersonate users
- Set a user’s password directly (forgot-password email only)
- Bulk delete, bulk suspend, or bulk reject
- Edit or delete audit logs
- Collect payments (there is no payment processor in MVP)

## Frontend vs backend

`middleware.ts` gates `/admin` on the `flowset_role=admin` cookie. That is a UX guard only.

Every mutating and listing admin endpoint is annotated `@PreAuthorize("hasRole('ADMIN')")`. Planner/vendor tokens receive **403**.

## IDOR

- Support tickets: non-owners who are not admin receive **404**
- Vendor documents: another vendor receives **404**
- Admin APIs load the resource by id from the database; missing rows are **404**

## Rate limiting (future)

There is no distributed rate limiter yet.

When added, apply it first to:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/support/tickets`
- Admin moderation POSTs (verify, reject, suspend, product moderate)

Suggested approach: per-IP + per-user token bucket (Bucket4j or Redis) in the Spring filter chain. Do not put this logic in the Next.js app.
