# Order workflow

```
Booking request ACCEPTED
  → CONFIRMED reservations via InventoryAvailabilityService
  → Booking CONFIRMED
  → Order created (CONFIRMED, unique booking_request_id)
      order number FS-YYYY-NNNNNN
  → Vendor schedules delivery          CONFIRMED → DELIVERY_SCHEDULED
  → Vendor marks out for delivery      DELIVERY_SCHEDULED → OUT_FOR_DELIVERY
  → Vendor marks delivered             OUT_FOR_DELIVERY → DELIVERED
  → Vendor marks event completed       DELIVERED → EVENT_COMPLETED
  → Vendor schedules pickup            EVENT_COMPLETED → PICKUP_SCHEDULED
  → Vendor marks picked up             PICKUP_SCHEDULED → PICKED_UP → COMPLETED
      inventory released
      booking → COMPLETED
```

Planner cancel is allowed only while the order is `PENDING`, `CONFIRMED`, or `DELIVERY_SCHEDULED`. After the goods leave for delivery, cancellation is rejected (409).

## Creation

Orders are created inside the vendor **accept** transaction (`BookingRequestService.accept` → `OrderService.createFromConfirmedBooking`).

- One order per booking request (`UNIQUE booking_request_id`). A second create returns the existing order.
- Customer-facing number is **not** the UUID. `OrderNumberService` allocates `FS-2026-000001` from `order_number_counters` with a pessimistic lock on the year row.
- Line snapshots: product name, variant name, SKU, quantity, price/day, rental days, subtotal.
- Inclusive rental days (12–14 Dec = 3). Formula: `price_per_day × quantity × rental_days`.
- `estimated_subtotal` = sum of line subtotals. `total_amount` = subtotal + optional delivery fee.
- `security_deposit` is stored but **not** included in `total_amount`. It is informational. Payment is not collected.

## Delivery and pickup

Vendor owns delivery status. Planner can view. Admin can view and update details for support.

Proof of delivery is optional (JPEG/PNG/WebP/GIF/PDF, max 10 MB) via `StorageService`. Bytes are not stored in PostgreSQL.

Pickup cannot be scheduled before the delivery date. Completion cannot happen before pickup (`OrderStateMachine`).

When the order becomes `COMPLETED`, `OrderCompletedEvent` also drives `REVIEW_ELIGIBLE`. Review rules: [`docs/reviews/review-workflow.md`](../reviews/review-workflow.md).

## Inventory

Confirmed reservations remain linked to `booking_request_id`. Cancel and complete call `InventoryAvailabilityService.releaseInventory` (idempotent) in the same transaction.

## Notifications and audit

`OrderNotificationService` emits log events (placeholder for the future dispatcher): order confirmed, delivery scheduled, out for delivery, delivered, pickup scheduled, picked up, completed, cancelled, proof uploaded.

Audit: `ORDER_CREATED`, `ORDER_CONFIRMED`, `DELIVERY_SCHEDULED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `EVENT_COMPLETED`, `PICKUP_SCHEDULED`, `PICKED_UP`, `ORDER_COMPLETED`, `ORDER_CANCELLED`, `PROOF_UPLOADED`, `INVENTORY_RELEASED`.
