# Inventory business rules

1. Inventory is per **variant** (SKU). Creating a variant always creates an `inventory` row (`InventoryService.createForVariant`).
2. Available quantity is **calculated**. Clients cannot persist an `available` figure as source of truth.
3. Standing formula: `available_standing = total − maintenance − damaged ≥ 0`.
4. Date-window formula: standing pool minus peak CONFIRMED reservations and ACTIVE blocks in the inclusive range.
5. A PENDING booking request must not permanently consume stock. Vendor **accept** re-checks availability and then `reserveInventory(..., CONFIRMED)`.
6. Cancelling a CONFIRMED booking must `releaseInventory` unless a linked order is already out for delivery. Completing an order (picked up) also `releaseInventory`. Reject does not create reservations.
7. Vendors may block quantity for maintenance, private bookings, damage, or unavailability. Maintenance CRUD writes `maintenance_records` **and** an `inventory_blocks` row so occupancy stays in one place (no double count).
8. Every manual stock change requires a **reason** and writes `inventory_movements`.
9. Low availability is `available ≤ 20% of total` (configurable). Listings stay visible.
10. Vendors mutate only their own variants; admins may mutate any; planners cannot mutate inventory.
11. Oversell attempts return **409** with the remaining quantity. Negative/zero request quantities and inverted dates return **400**.
12. Audit log actions: `INVENTORY_CREATED`, `INVENTORY_ADJUSTED`, `INVENTORY_BLOCKED`, `INVENTORY_UNBLOCKED`, `MAINTENANCE_CREATED`, `MAINTENANCE_REMOVED`, `RESERVATION_CREATED`, `RESERVATION_RELEASED` (plus `RESERVATION_CONFIRMED`).
