# 0002 — Project catalog snapshots

## Status

Accepted.

## Context

Project products must survive vendor price changes and hard-deleted variants. Adding a product must not reserve inventory.

## Decision

`project_products` stores a price/name/image snapshot at add time. `product_variant_id` is `ON DELETE SET NULL`. Availability and current price are read from live catalog when present. Inventory reservation is owned exclusively by `InventoryAvailabilityService` (Prompt 13).

## Consequences

Historical project boards stay readable. Planners see “Now ₹X” when live price diverges. Booking must copy snapshot vs live price explicitly when that workflow lands.
