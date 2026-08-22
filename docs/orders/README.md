# Orders

Operational rental orders created when a booking request is **CONFIRMED**. Backend: `com.flowset.orders`. Frontend: `services/order-api.ts`, `/planner/orders`, `/vendor/bookings`.

- Workflow: [order-workflow.md](./order-workflow.md)
- State machine: [order-state-machine.md](./order-state-machine.md)
- Delivery: [delivery-workflow.md](../delivery/delivery-workflow.md)
- Schema: Flyway `V7__orders_and_delivery.sql` (see [database README](../database/README.md))
- HTTP: [api README](../api/README.md)

MVP does **not** include online payments, payment gateways, driver apps, GPS, route optimization, logistics marketplaces, dynamic delivery pricing, or platform commission.

Completed orders unlock a single verified vendor review. See [`docs/reviews/`](../reviews/review-business-rules.md).

Inventory stays on `inventory_reservations.booking_request_id` (V4). This module does not create a second reservation system.
