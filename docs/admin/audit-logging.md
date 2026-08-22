# Audit logging

Table: `audit_logs` (Flyway `V10__admin_platform.sql`).

| Column | Notes |
| --- | --- |
| id | UUID |
| actor_user_id | Admin or user who performed the action |
| action | Stable string, e.g. `VENDOR_VERIFIED` |
| resource_type | Vendor, product, user, review, ticket, settings, … |
| resource_id | UUID of the target |
| ip_address / user_agent | From the HTTP request when available |
| metadata | Reason and extra JSON/text |
| created_at | Immutable timestamp |

## Rules

- Append-only. `AuditLogService` persists rows; there is no update or delete API.
- `DELETE /api/v1/admin/audit-logs` returns **405**.
- Admins can list and filter: action, resource type, actor, resource id, date range.
- UI: `/admin/audit-logs`.

## Tracked actions (non-exhaustive)

Login, vendor verified/rejected/suspended/reactivated, document approved/rejected, product approved/rejected/suspended/restored, category changed, review moderated, user suspended/reactivated, settings changed, support ticket actions.

Indexes: `created_at`, `actor_user_id`, `resource_id`, `action`, `resource_type`.
