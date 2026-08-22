# Notification business rules

## Three stores

| Store | Purpose |
| --- | --- |
| `notifications` | Personal alerts requiring attention |
| `project_activity` | User-facing history on a project |
| `audit_logs` | Security / compliance |

Do not merge these tables.

## Preferences

Groups: booking updates, order updates, delivery updates, inventory alerts, system announcements. Default: enabled. Disabled groups skip create. Review types (`REVIEW_ELIGIBLE`, `REVIEW_RECEIVED`, `REVIEW_REPORTED`, `REVIEW_MODERATED`) use system announcements. Review bodies are never copied into vendor notifications.

## Duplicate suppression

Skip creating another **unread** row with the same recipient + type + resource id.

## Authorization

Users may only list, mark read, or delete their own notifications. Cross-user ids return **404** `Notification not found`.

## Transaction isolation

Booking/order/vendor writes must succeed even if notification persistence fails. Listeners run `@TransactionalEventListener(AFTER_COMMIT)` (inventory unavailable uses `AFTER_ROLLBACK`) and swallow exceptions. `NotificationService.createNotification` uses `REQUIRES_NEW`. See [ADR 0003](../architecture/decisions/0003-notification-after-commit.md).

## Project activity

Important project actions also write `project_activity` in the same business transaction: project CRUD, products, vendors, mood board, booking created/accepted/rejected/cancelled, order confirmed, delivery completed, order completed.

## Reminders

`UPCOMING_DELIVERY` and `PICKUP_REMINDER` fire when delivery/pickup is scheduled. Time-based reminder jobs are out of MVP scope.
