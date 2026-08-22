# Review moderation

Admins moderate at `/admin/reviews` and `GET/PATCH /api/v1/admin/reviews/**`.

## Reports

Reasons: `SPAM`, `ABUSIVE`, `FAKE`, `INAPPROPRIATE`, `PERSONAL_INFORMATION`, `OTHER`.

`review_reports`: `reported_by`, `reason`, `description`, `status` (`OPEN` / `RESOLVED` / `DISMISSED`), `created_at`, `resolved_at`.

A report does not hide the review. `GET /admin/reviews/reported` lists reviews with open reports.

Authors cannot report their own review.

## Actions

`PATCH /admin/reviews/{id}/moderate` `{ action, note }`

| Action | Effect |
| --- | --- |
| `HIDE` | `HIDDEN`. Dropped from public list and vendor aggregates. |
| `RESTORE` | `PUBLISHED`, clears soft-delete fields, resolves open reports, recalculates rating. |
| `REMOVE` | Soft delete + `REMOVED`, resolves open reports, recalculates rating. |
| `NOTE` | History row only; no rating change. |

Every action writes `review_moderation_actions` and `AuditLogService` (`REVIEW_HIDE`, `REVIEW_RESTORE`, `REVIEW_REMOVE`, `REVIEW_NOTE`). `GET /admin/reviews/{id}/history` returns that trail.

Vendors cannot hide, restore, or remove reviews.

## After moderation

`VendorRatingService.recalculate` runs so marketplace cards and the public profile never keep a stale average.
