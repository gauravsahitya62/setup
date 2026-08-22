# Support tickets

Planner and vendor users can open tickets. Admins triage them.

## Tables

- `support_categories`
- `support_tickets`
- `support_messages`

## Ticket fields

`id`, `created_by`, `subject`, `description`, `category`, `priority`, `status`, `assigned_admin_id`, `created_at`, `updated_at`, `resolved_at`

Status: `OPEN`, `IN_PROGRESS`, `WAITING_FOR_USER`, `RESOLVED`, `CLOSED`

Priority: `LOW`, `MEDIUM`, `HIGH`, `URGENT`

## User APIs (`/api/v1/support`)

| Method | Path |
| --- | --- |
| POST | `/tickets` |
| GET | `/tickets` |
| GET | `/tickets/{id}` |
| POST | `/tickets/{id}/messages` |
| PATCH | `/tickets/{id}/close` |
| GET | `/categories` |

Non-owners receive **404**.

## Admin APIs (`/api/v1/admin/support`)

| Method | Path |
| --- | --- |
| GET | `/tickets` |
| GET | `/tickets/{id}` |
| PATCH | `/tickets/{id}` assign / priority / status |
| POST | `/tickets/{id}/messages` |

UI: `/admin/tickets`, `/planner/support`, `/vendor/support`.
