# Marketplace module

Rental product catalog: categories, vendor products/variants, images, inventory, availability, collections, and
planner favorites. Search is documented separately in [`docs/search/README.md`](../search/README.md).

## Backend package

`com.flowset.marketplace`

- `category` — category tree (self-referencing, admin-managed)
- `product` — products, variants, images, attributes, tags (vendor-owned)
- `inventory` — standing stock plus the date-window availability engine (reservations, blocks,
  movements, maintenance). See [`docs/inventory/README.md`](../inventory/README.md).
- `search` — `ProductSpecifications` / `ProductSearchPort` / `MarketplaceSearchService` (see search docs)
- `favorite` — planner saved products
- `collection` — vendor-curated product groupings

Each module follows the existing vendor-module shape: `entity` / `repository` / `dto` / `mapper` / `service` /
`controller`.

## Flyway migration

`V3__marketplace.sql` creates all marketplace tables and seeds six baseline categories (Furniture, Lighting,
Decor, Tents & Structures, Audio & Visual, Tableware) so the catalog is usable without a separate admin
bootstrap step. Full table shapes are in [`docs/database/README.md`](../database/README.md).

## Business rules

- **Ownership**: only the owning vendor (or ADMIN) can create/update/delete their own products, variants,
  images, attributes, collections, and manage inventory/availability. Admins may act on behalf of any vendor by
  passing `vendorId` explicitly on create.
- **Visibility**: products/collections default to `DRAFT`/`HIDDEN` and are invisible to the public until
  `PUBLISHED`/`ACTIVE`; owners and admins can always see their own non-public records.
- **Soft delete**: only `products` are soft-deleted (`deleted_at`). Categories, variants, images, attributes,
  collections, and collection items are hard-deleted (with guard rails — e.g. a category with subcategories or
  products cannot be deleted).
- **Slugs**: generated via the existing `SlugUtils.uniqueSlug`, scoped per-vendor for products/collections and
  globally for categories, exactly like the vendor module.
- **Images/documents**: reuse `StorageService`; only public URLs are persisted (`products/{id}/images/...`).
- **Variant → Inventory**: creating a `ProductVariant` always creates a matching `Inventory` row via
  `InventoryService.createForVariant`, with `total = quantity` and
  `reserved = maintenance = damaged = 0`. Standing `available = total − maintenance − damaged`.
- **Date-window availability**: `InventoryAvailabilityService` computes available units from standing
  totals minus peak CONFIRMED reservations and ACTIVE blocks in the inclusive range. PENDING
  reservations do not consume stock. See [`docs/inventory/README.md`](../inventory/README.md).

## API surface (`/api/v1`)

| Area | Endpoints |
| --- | --- |
| Categories | `GET /categories` (tree or flat), `GET /categories/{id}`, `GET /categories/slug/{slug}`, `POST/PUT/DELETE /categories/{id}` (ADMIN) |
| Products | `GET /products/{id}`, `GET /products/slug/{slug}`, `POST/PUT/DELETE /products/{id}` (vendor owner/admin), `GET /vendors/{vendorId}/products` |
| Images | `POST /products/{id}/images` (multipart), `DELETE /products/{id}/images/{imageId}`, `PUT /products/{id}/images/reorder` |
| Attributes | `GET/POST /products/{id}/attributes`, `PUT/DELETE /products/{id}/attributes/{attributeId}` |
| Variants | `GET/POST /products/{productId}/variants`, `GET/PUT/DELETE /products/{productId}/variants/{variantId}` |
| Inventory | `GET/PUT /variants/{id}/inventory` (legacy), `GET /inventory/{id}/availability`, `GET/PATCH /vendor/inventory`, adjust/maintenance/movements, `GET /vendor/calendar` |
| Collections | `GET /collections`, `GET /collections/{id|slug}`, `POST/PUT/DELETE /collections/{id}`, `GET/POST /collections/{id}/items`, `DELETE /collections/{id}/items/{productId}`, `PUT /collections/{id}/items/reorder` |
| Favorites | `GET /favorites`, `POST/DELETE /favorites/{productId}` (PLANNER/ADMIN) |
| Search | `GET /search` — see [`docs/search/README.md`](../search/README.md) |

## Security

`SecurityConfig` permits public `GET` on `/categories/**`, `/products/**`, `/search`, `/collections/**`,
and `/inventory/**` (availability check). Vendor inventory mutations require authentication, with
`@PreAuthorize` role checks plus in-service ownership checks mirroring `VendorService`.

## Testing

- `MarketplaceIntegrationTest` — end-to-end: admin category management, vendor creates
  product → variant (auto inventory) → publish → public search finds it → planner favorites it; plus
  validation rejection tests (negative price, negative variant quantity).
- `ProductSpecificationsTest` — exercises `ProductSpecifications` filters directly against the repository.
- `InventoryServiceTest` — standing `available = total − maintenance − damaged`.
- `OccupancyCalculatorTest` — inclusive overlap, adjacent ranges, peak occupancy.
- `InventoryAvailabilityIntegrationTest` — reservations, maintenance, security, concurrent confirms.

Run: `mvn -f backend test`
