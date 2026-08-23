# Architecture decisions

ADRs for FlowSet backend choices.

| ADR | Title |
| --- | --- |
| [0001](./decisions/0001-inventory-concurrency.md) | Inventory concurrency (pessimistic lock + version) |
| [0002](./decisions/0002-project-snapshots.md) | Project catalog snapshots (no inventory side effects) |
| [0003](./decisions/0003-notification-after-commit.md) | Notification delivery after commit |
| [0004](./decisions/0004-review-rating-storage.md) | Review stars as SMALLINT; vendor average cached NUMERIC(3,2) |

Projects (`planner_id` = authenticated `users.id`, snapshots on `project_products`, no inventory side effects) are documented in [`docs/projects/project-business-rules.md`](../projects/project-business-rules.md). Booking requests reserve inventory only through `InventoryAvailabilityService` — [`docs/booking/`](../booking/README.md). Confirmed bookings create orders; inventory stays on `booking_request_id` — [`docs/orders/`](../orders/README.md). In-app notifications are documented in [`docs/notifications/`](../notifications/notification-architecture.md). Vendor reviews are documented in [`docs/reviews/`](../reviews/review-business-rules.md). Admin portal, verification, moderation, support, and immutable audit logs are documented in [`docs/admin/`](../admin/README.md).

Pilot operations (environments, Docker, CI, RDS/S3/ECS placeholders): [`docs/deployment/production-architecture.md`](../deployment/production-architecture.md).

QA and pilot: [`docs/qa/bug-report.md`](../qa/bug-report.md), [`docs/qa/final-qa-report.md`](../qa/final-qa-report.md), [`docs/pilot/onboarding.md`](../pilot/onboarding.md).
