# Booking request state machine

Implemented by `BookingRequestStateMachine`. Arbitrary status writes are rejected with 409.

| From | To |
| --- | --- |
| DRAFT | PENDING |
| PENDING | ACCEPTED, REJECTED, CANCELLED |
| ACCEPTED | CONFIRMED |
| CONFIRMED | CANCELLED, COMPLETED |
| REJECTED | — |
| CANCELLED | — |
| COMPLETED | — |

`COMPLETED` is set when the linked order is picked up (`OrderService.markPickedUp`). This workflow creates requests as **PENDING** (no DRAFT persist) and moves `PENDING → ACCEPTED → CONFIRMED` inside the vendor accept transaction, which also creates the Order.

## Timeline events

Stored on `booking_request_status_history`:

`REQUEST_SENT` → `VENDOR_VIEWED` → `VENDOR_ACCEPTED` → `INVENTORY_RESERVED` → `CONFIRMED`

or `VENDOR_REJECTED` / `PLANNER_CANCELLED` / `INVENTORY_RELEASED`.

## Who may change status

| Action | Role | Endpoint |
| --- | --- | --- |
| Create (→ PENDING) | PLANNER | `POST /booking-requests` |
| Cancel | PLANNER (owner) | `POST /booking-requests/{id}/cancel` |
| Accept | VENDOR (storefront owner) | `POST /vendor/booking-requests/{id}/accept` |
| Reject | VENDOR (storefront owner) | `POST /vendor/booking-requests/{id}/reject` |
| Admin | view only | `GET /booking-requests` |

Admins cannot flip status without a dedicated audited admin action (not in this module).
