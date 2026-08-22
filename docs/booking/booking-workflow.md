# Booking workflow

```
Planner project
  → Select vendor
  → Select products / quantities
  → Event + delivery details
  → Review (estimated rental value)
  → POST /api/v1/booking-requests   (PENDING, no inventory hold)
  → Vendor views request            (VENDOR_VIEWED)
  → Accept  → re-check availability
            → lock via InventoryAvailabilityService
            → CONFIRMED reservations
            → ACCEPTED then CONFIRMED
            → Order created (FS-YYYY-NNNNNN)
     or Reject → PENDING → REJECTED
  → Planner may cancel PENDING or CONFIRMED (CONFIRMED releases stock unless the order is already out for delivery)
```

Inventory is reserved **only** on vendor accept, through `InventoryAvailabilityService.reserveInventory(..., CONFIRMED)`. Booking code must not insert `inventory_reservations` rows itself.

A planner may send the **same requirement** to Vendor A, B, and C as independent requests (separate IDs and lifecycles). Accepting one does not cancel the others.

## Create

1. Planner owns the project (`ProjectAccessService.requireWritable`).
2. Vendor exists, is not deleted, and is `PUBLISHED`.
3. Each variant is on the project, belongs to that vendor, and is still live.
4. Quantities ≥ 1, rental dates valid (inclusive), event date inside the rental window.
5. Availability is checked **informationally**. If stock is short the API returns 409:
   `Only {n} units are currently available for the selected dates.`
   Quantity is never silently reduced.
6. Snapshots of product name, variant name, SKU, vendor, and price/day are stored on items.

## Duplicate pending requests

Same planner + project + vendor + same variant/quantity set + same rental dates + an existing **PENDING** request → 409:

`A pending booking request already exists for this vendor, products, and dates.`

A new request is allowed after the previous one is rejected, cancelled, or confirmed, or if products/dates/quantities differ.

## Accept (single transaction)

1. Re-check availability.
2. `PENDING → ACCEPTED` (history).
3. `reserveInventory` for each item, sorted by `variantId` to avoid deadlocks.
4. History `INVENTORY_RESERVED`.
5. `ACCEPTED → CONFIRMED`, set `responded_at`.
6. Audit + notification events.

If any step fails (including oversell), the transaction rolls back. The request stays **PENDING**. Message:

`Inventory is no longer available for the requested dates.`

## Reject / cancel

- Reject requires a reason (`INVENTORY_UNAVAILABLE`, `DATES_UNAVAILABLE`, `QUANTITY_UNAVAILABLE`, `SERVICE_AREA`, `OTHER`) and optional note.
- Cancel PENDING: status only. Cancel CONFIRMED: `releaseInventory` for reservations with that `booking_request_id`.
- COMPLETED (future orders module) cannot be cancelled here.

## Pricing

MVP: **Estimated rental value** = `price_per_day × quantity × inclusive_rental_days`. Currency INR. No tax, commission, fees, or discounts.
