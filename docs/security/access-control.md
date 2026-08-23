# Access control

Roles: `PLANNER`, `VENDOR`, `ADMIN`. Authorization is enforced on the **API**, not by Next.js cookies.

## Token strategy

| Token | Where | Lifetime (default) |
| --- | --- | --- |
| Access JWT | Authorization header; Zustand memory | 15 minutes |
| Refresh JWT | HttpOnly cookie `flowset_refresh` (path `/api/v1/auth`) + SHA-256 row | 7 days, rotated |

Logout clears the refresh cookie and server token row. Access tokens expire naturally.

Frontend cookies `flowset_session` and `flowset_role` only drive middleware redirects.

## Resource rules

| Resource | Planner | Vendor | Admin |
| --- | --- | --- | --- |
| Own project / project files | owner | no | yes |
| Other planner project | no (404/403) | no | yes |
| Own booking requests | owner | assigned vendor | yes |
| Inventory writes | no | owner vendor | yes |
| Public marketplace GET | yes | yes | yes |
| Vendor GST/PAN files | no | owner | yes |
| Order delivery proof | order planner | order vendor | yes |
| `/api/v1/admin/**` | 403 | 403 | yes |
| Audit logs | no | no | read-only |

IDs in URLs are never trusted alone. Services load the entity and compare `planner_id` / `vendor.owner_user_id` to `UserPrincipal`.

## Files

`GET /api/v1/files/**` is reachable without a token **only** for public prefixes:

- `vendors/{id}/logo|banner|portfolio/`
- `products/{id}/images/`
- `categories/{id}/`

Everything else requires a valid access token and an ownership check.

## CORS

Exact origins. Credentials on. Production rejects `CORS_ALLOWED_ORIGINS=*` at startup.
