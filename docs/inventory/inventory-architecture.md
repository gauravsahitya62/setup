# Inventory architecture

FlowSet inventory belongs to **product variants**, not products. A product such as Royal Gold Chair can have Gold / White / Silver SKUs; each SKU has its own totals, reservations, and blocks.

## Package

`com.flowset.marketplace.inventory`

| Layer | Types |
| --- | --- |
| Engine | `DateRange`, `OccupancyCalculator` — inclusive overlap and peak occupancy |
| Service | `InventoryAvailabilityService` — date-window availability; `InventoryService` — standing stock, vendor overview, movements |
| Persistence | `inventory`, `inventory_reservations`, `inventory_blocks`, `inventory_movements`, `maintenance_records` |
| HTTP | `PublicAvailabilityController`, `VendorInventoryController`, legacy `/variants/{id}/inventory` |

Controllers never compute availability. Booking requests (`com.flowset.planner.booking`) call:

- `checkAvailability` (create + accept)
- `reserveInventory` with `CONFIRMED` (accept only)
- `releaseInventory` (planner cancel of CONFIRMED)

## Standing vs date-window

`inventory.total_quantity`, `maintenance`, and `damaged` are standing. Available quantity for a window is:

```
standingPool = total − maintenance − damaged
peakOccupied = peak(CONFIRMED reservations ∪ ACTIVE blocks) on any day in [start, end]
available    = max(0, standingPool − peakOccupied)
```

`inventory.available` and `inventory.reserved` are **denormalized snapshots**, not the source of truth for a booking window. See [`availability-engine.md`](./availability-engine.md).

## Ownership

Vendor id is never taken from the client. The authenticated user is resolved to their storefront (`vendors.owner_user_id`). Admins may manage any variant. Planners may only call the public availability GET.

## Low availability

Configurable `flowset.inventory.low-stock-threshold-percent` (default **20**). When `available ≤ 20% of total` the row is flagged Low stock. Products are **not** hidden from the marketplace.
