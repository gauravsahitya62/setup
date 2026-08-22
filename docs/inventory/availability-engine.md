# Availability engine

`InventoryAvailabilityService` is the only place date logic lives.

## Inclusive dates

Start `12 Dec` and end `14 Dec` reserves the 12th, 13th, and 14th. Same-day bookings (`start == end`) are valid. `end < start` is rejected.

Two ranges overlap when `a.start <= b.end AND a.end >= b.start`. Adjacent ranges do not overlap: `10–12` and `13–15` are independent.

## Peak occupancy

Summing every overlapping reservation across a multi-day window over-counts intervals that do not share a day. The engine uses a **sweep line** (`OccupancyCalculator.peakOccupied`) so:

- A: 12–13 × 40 and B: 15–16 × 30 → peak in 12–16 is **40**, not 70
- A: 10–12 × 40 and B: 11–13 × 30 → peak is **70** on the 11th–12th

CONFIRMED reservations and ACTIVE blocks are combined into one occupancy set before the peak is taken.

## Status effects

| Status | Consumes stock? |
| --- | --- |
| Reservation `PENDING` | No |
| Reservation `CONFIRMED` | Yes |
| Reservation `RELEASED` / `CANCELLED` / `EXPIRED` | No |
| Block `ACTIVE` | Yes |
| Block `RELEASED` | No |

## `GET /api/v1/inventory/{variantId}/availability`

Query: `startDate`, `endDate`, optional `quantity`.

Returns `available`, `requestedQuantity`, `availableQuantity`, `status` (`AVAILABLE` \| `PARTIAL` \| `UNAVAILABLE`), `message` (e.g. “Only 45 available for these dates.”), `conflicts` (days below the requested quantity), and a per-day `calendar` for that window only. The engine does not load the entire `availability_calendar` table.

## Concurrency

See [`../architecture/decisions/0001-inventory-concurrency.md`](../architecture/decisions/0001-inventory-concurrency.md). Confirm/reserve takes a pessimistic lock on the variant’s `inventory` row, then re-reads occupancy.

Booking accept (`BookingRequestService.accept`) calls `reserveInventory` with `CONFIRMED` inside the same transaction. It must not insert reservation rows itself.
