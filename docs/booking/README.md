# Booking requests

Core planner → vendor transaction workflow. Backend: `com.flowset.planner.booking`. Frontend: `services/booking-api.ts`, `/planner/booking-requests`, `/vendor/requests`.

- Workflow: [booking-workflow.md](./booking-workflow.md)
- State machine: [booking-state-machine.md](./booking-state-machine.md)
- Business rules: [booking-business-rules.md](./booking-business-rules.md)
- Schema: Flyway `V6__booking_requests.sql` (see [database README](../database/README.md))
- HTTP: [api README](../api/README.md)

Orders and delivery: accepting a request creates a CONFIRMED order (`com.flowset.orders`). Booking **COMPLETED** is set when the order is picked up. See [`docs/orders/`](../orders/README.md).
