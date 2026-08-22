# Inventory module

Date-based rental inventory for product variants. Prevents double-booking by calculating availability from standing totals plus CONFIRMED reservations and ACTIVE blocks.

- Architecture: [`inventory-architecture.md`](./inventory-architecture.md)
- Engine: [`availability-engine.md`](./availability-engine.md)
- Rules: [`inventory-business-rules.md`](./inventory-business-rules.md)
- Concurrency ADR: [`../architecture/decisions/0001-inventory-concurrency.md`](../architecture/decisions/0001-inventory-concurrency.md)

## HTTP

| Method | Path | Auth |
| --- | --- | --- |
| GET | `/api/v1/inventory/{variantId}/availability` | Public |
| GET | `/api/v1/vendor/inventory` | VENDOR, ADMIN |
| GET | `/api/v1/vendor/inventory/{variantId}` | Owner or ADMIN |
| PATCH | `/api/v1/vendor/inventory/{variantId}` | Owner or ADMIN |
| POST | `/api/v1/vendor/inventory/{variantId}/adjust` | Owner or ADMIN |
| POST | `/api/v1/vendor/inventory/{variantId}/maintenance` | Owner or ADMIN |
| DELETE | `/api/v1/vendor/inventory/{variantId}/maintenance/{id}` | Owner or ADMIN |
| GET | `/api/v1/vendor/inventory/{variantId}/movements` | Owner or ADMIN |
| GET | `/api/v1/vendor/calendar` | VENDOR, ADMIN |

Legacy `GET/PUT /api/v1/variants/{id}/inventory` still works for catalog tooling.

## Frontend

| Route | Purpose |
| --- | --- |
| `/vendor/inventory` | Overview KPIs and variant table |
| `/vendor/inventory/[variantId]` | Detail, calendar, adjustments, maintenance, history |
| `/vendor/availability` | Month/week calendar with filters |
| `/marketplace/[slug]` | Planner date/qty availability check |

Client: `marketplaceApi.vendorInventory` and `marketplaceApi.inventory.check`.

Orders reuse this module: confirm keeps CONFIRMED reservations; cancel and complete call `releaseInventory`. See [`docs/orders/order-workflow.md`](../orders/order-workflow.md).
