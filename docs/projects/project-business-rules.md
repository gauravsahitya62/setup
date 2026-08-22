# Project business rules

The Project is a planner's event workspace (e.g. Sharma Wedding). It is **not** a booking and **does not** move inventory.

## Ownership

- `planner_id` is always the authenticated user's id. The client cannot set it.
- Planners may only read/write their own non-deleted projects.
- Another planner's project (or a vendor request) is **404 Not Found** — never 403 — so existence is not leaked.
- Admins may **view** any project for support. They cannot create, update, or delete planner projects.
- Vendors cannot mutate planner projects. Booking requests grant vendors access to **their** requests only (`/api/v1/vendor/booking-requests`).

## Inventory

1. Creating a project does not reserve inventory.
2. Adding a product variant does not reserve inventory.
3. Changing quantity or notes does not reserve inventory.
4. Removing a product does not change inventory.
5. Inventory is reserved only through the booking workflow via `InventoryAvailabilityService.reserveInventory`.
6. Availability checks on a project product call the existing public availability API. They are read-only.

See [`docs/booking/`](../booking/README.md).

## Catalog snapshots

`project_products` stores `product_name`, `variant_name`, `sku`, `image_url`, `unit_price_per_day`, and `currency` at add time.

- Vendor price changes do not rewrite historical project rows. `currentPricePerDay` is informational.
- `product_variant_id` is `ON DELETE SET NULL`. Deleted variants keep the snapshot so the project still loads.
- Duplicate variants on the same project are rejected (unique `(project_id, product_variant_id)`).

## Vendors

Saving or removing a vendor is a shortlist only. It must not create, confirm, or cancel bookings.

## Soft delete

`DELETE /projects/{id}` sets `deleted_at`. Child rows remain. Activity history is append-only (no update/delete endpoints).

## Progress

Planning progress is **not** hardcoded. Ten equally weighted setup tasks:

1. Event date
2. Guest count &gt; 0
3. Venue name or city
4. Description
5. Cover image
6. ≥1 product
7. ≥1 saved vendor
8. ≥1 mood board image
9. ≥1 note
10. ≥1 file

`percent = round(100 × completed / 10)`. The API returns the task list so the UI can render it.

## Event types

Stored as VARCHAR / Java enum (`EventType`). There is no SQL CHECK on the type list so a new type is a code + label change, not a Flyway migration. Statuses (`DRAFT` … `CANCELLED`) are a closed CHECK list.

## Storage

Mood board images, cover images, and project files go through `StorageService`. PostgreSQL stores URLs and metadata only. Project files allow office documents in addition to images/PDF (`AllowedContentTypes.PROJECT_DOCUMENTS`).
