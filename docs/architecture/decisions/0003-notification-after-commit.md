# Notification delivery after commit

## Status

Accepted

## Context

Notifications are side effects of bookings, orders, inventory, and vendor verification. If notification insert fails inside the same database transaction as “accept booking”, the booking would roll back even though inventory was reserved and the order was created.

We evaluated:

| Approach | Fit |
| --- | --- |
| Call `NotificationService` inside the domain transaction | Couples fulfillment to inbox I/O. A unique-constraint or template error undoes the business write. |
| `@Async` without transaction hooks | Can run before commit and see uncommitted data, or lose the event on crash. |
| Kafka immediately | Correct long-term, extra ops for MVP. |

## Decision

Publish Spring application events from domain services. Consume with `@TransactionalEventListener(phase = AFTER_COMMIT)`. Persist notifications in `REQUIRES_NEW`. Catch listener exceptions and log them.

`InventoryUnavailableEvent` uses `AFTER_ROLLBACK` because accept failed; the planner still needs an alert.

The event records in `com.flowset.events` are the seam for Kafka/RabbitMQ later.

## Consequences

- A committed booking/order stays committed if the inbox write fails.
- `@Transactional` tests do not fire `AFTER_COMMIT` listeners (the test transaction never commits). Notification flow tests run without class-level `@Transactional`.
- At-least-once delivery is not guaranteed across process crash between commit and listener. Acceptable for MVP in-app mail.
