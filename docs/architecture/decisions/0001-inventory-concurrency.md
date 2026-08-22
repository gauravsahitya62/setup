# Inventory concurrency

## Status

Accepted

## Context

FlowSet rentals are quantity-based and date-based. Two planners can confirm the same variant for overlapping inclusive dates at the same moment. Available quantity is **not** a single stored column: it is computed from standing totals minus CONFIRMED reservations and ACTIVE blocks in the requested window.

We evaluated:

| Approach | Fit |
| --- | --- |
| Optimistic locking (`@Version` on `inventory`) alone | Insufficient. Confirming a reservation inserts a row; it does not always change `inventory.version`. Two overlapping confirms would both pass. |
| `SERIALIZABLE` isolation | Correct but noisy: serialization failures require retry loops, and H2/PostgreSQL behaviour differs. |
| PostgreSQL exclusion constraints | Cannot express “sum of overlapping quantities ≤ total”. They only prevent range overlap, not partial-quantity stacking. |
| Pessimistic row lock on `inventory` | Simple mutex per variant. After `SELECT … FOR UPDATE`, the occupancy query sees committed CONFIRMED rows. |

## Decision

Use **pessimistic write locks on the `inventory` row** (`LockModeType.PESSIMISTIC_WRITE` / `SELECT … FOR UPDATE`) inside `READ COMMITTED` transactions for:

- `reserveInventory` (when status is CONFIRMED)
- `confirmReservation`
- `releaseInventory`
- `blockInventory` / `unblockInventory`
- maintenance create/delete

Keep **`@Version` on `inventory`** for standing-stock updates (`PATCH`, adjust). Concurrent quantity edits fail with HTTP 409 instead of silent overwrites.

Default isolation remains **READ COMMITTED**. The row lock is the mutex; we do not raise isolation.

## Consequences

- Concurrent CONFIRMED reservations for the same variant serialize. The second caller recomputes occupancy and receives `409 Conflict` with “Only N available for these dates.”
- PENDING reservations do not take the consume path (they still lock to avoid duplicate row races when immediately confirmed).
- Callers of standing PATCH should retry on 409.
- Prompt 13 (bookings) must call `InventoryAvailabilityService` inside the same transactional style; it must not insert reservation rows from a controller.
