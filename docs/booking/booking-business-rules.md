# Booking business rules

1. `planner_id` and `vendor_id` are never taken from the client. Planner identity is the JWT. Vendor identity is `vendors.owner_user_id` of the authenticated user.
2. Cross-owner GET returns **404**, not 403, so request existence is not leaked.
3. Planners cannot accept, reject, or mutate inventory. Vendors cannot cancel, change quantities, or edit project details.
4. Products on a request must belong to the selected vendor **and** already be on the project. Adding a product to a project still does not reserve stock.
5. Multiple vendors may receive independent requests for the same project requirement. Each has its own id and status.
6. Availability at create is informational. Inventory is reserved only when a vendor **accepts**, via `InventoryAvailabilityService` (CONFIRMED). PENDING holds are not used for booking requests.
7. Insufficient stock at create: 409 with remaining units. Quantity is not reduced.
8. Insufficient stock at accept: 409, request remains PENDING, transaction rolls back. No partial accept.
9. Concurrent accepts on overlapping stock cannot oversell. The inventory engine’s pessimistic lock on the `inventory` row is the source of truth.
10. Duplicate PENDING (same planner, project, vendor, products+quantities, dates) is rejected.
11. Item rows store name / variant / SKU / price-per-day snapshots. `product_variant_id` is `ON DELETE SET NULL`.
12. Estimated rental value is not a payable total. Label it **Estimated Rental Value**.
13. Cancelling CONFIRMED releases reservations through `releaseInventory` unless a linked order is already out for delivery (409). Cancelling COMPLETED is forbidden.
14. Accepting a request creates a CONFIRMED order in the same transaction. Duplicate orders for the same booking request are not created.
15. Notification events (log sink today): created, accepted, rejected, cancelled, inventory no longer available.
16. Audit actions: `BOOKING_REQUEST_CREATED`, `BOOKING_REQUEST_VIEWED`, `BOOKING_REQUEST_ACCEPTED`, `BOOKING_REQUEST_REJECTED`, `BOOKING_REQUEST_CANCELLED`, plus engine `RESERVATION_*` / `INVENTORY_RESERVED` / `INVENTORY_RELEASED`.
