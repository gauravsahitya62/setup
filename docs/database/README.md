# Vendor database schema

Flyway migration: `backend/src/main/resources/db/migration/V2__vendor_storefront.sql`

Auth’s thin `vendor_profiles` table (from V1) remains for registration metadata. The digital storefront uses `vendors.owner_user_id → users.id`.

## Tables

### vendors

| Column | Notes |
| --- | --- |
| id | UUID PK |
| owner_user_id | FK users, unique storefront owner |
| company_name | Required |
| slug | Unique public URL key |
| description, short_description | About copy |
| gst_number, business_type, established_year | Optional business facts |
| website, email, phone | Contact |
| logo_url, banner_url | Public URLs from StorageService |
| rating, review_count, completed_events, response_time_minutes | Trust metrics |
| verified | Admin-driven flag |
| status | DRAFT / PUBLISHED / SUSPENDED |
| created_at, updated_at, deleted_at | Soft delete only |

### vendor_service_areas

`id`, `vendor_id`, `country`, `state`, `city` — many locations per vendor.

### vendor_portfolio

`id`, `vendor_id`, `image_url`, `title`, `description`, `display_order`.

### vendor_documents

Metadata only: `file_url`, `file_name`, `document_type`, `verification_status`.

Types: `GST_CERTIFICATE`, `BUSINESS_LICENSE`, `PAN`, `IDENTITY`, `OTHER`.

### vendor_social_links

One row per vendor: Instagram, Facebook, LinkedIn, YouTube, Pinterest, Website.

### vendor_business_hours

Future-ready hours rows keyed by `vendor_id` + day of week.

### vendor_team_members

Future-ready team roster (`vendor_id`, user/name/role).

## Relationships

```
users 1──1 vendors
vendors 1──* vendor_service_areas
vendors 1──* vendor_portfolio
vendors 1──* vendor_documents
vendors 1──0..1 vendor_social_links
vendors 1──* vendor_business_hours
vendors 1──* vendor_team_members
```

## Storage

Image/document bytes are **not** stored in PostgreSQL. URLs point at `/api/v1/files/**` (local) or a future object-store CDN.

## Marketplace schema (V3)

Flyway migration: `backend/src/main/resources/db/migration/V3__marketplace.sql`

See [`docs/marketplace/README.md`](../marketplace/README.md) for the full table/relationship rundown. Summary:

| Table | Notes |
| --- | --- |
| `categories` | Self-referencing tree (`parent_category_id`), unique `slug`. Seeded with Furniture, Lighting, Decor, Tents & Structures, Audio & Visual, Tableware. |
| `products` | Belongs to `vendors` + `categories`; `UNIQUE(vendor_id, slug)`; soft delete via `deleted_at`. |
| `product_variants` | SKU-level rentable unit; unique `sku`. |
| `product_images` / `product_attributes` | Ordered child rows per product. |
| `product_tags` / `product_tag_map` | Simple tag vocabulary + many-to-many join table. |
| `product_collections` / `product_collection_items` | Vendor-curated storefront collections. |
| `inventory` | One row per variant; standing `available = total − maintenance − damaged`. Date-window availability is computed from reservations and blocks, not this column. Optimistic lock via `version`. |
| `inventory_reservations` | Inclusive date holds. Only `CONFIRMED` consumes stock. `PENDING` does not. |
| `inventory_blocks` | Vendor date-range holds (maintenance, private booking, damage, unavailability). |
| `inventory_movements` | Audit trail of stock changes. |
| `maintenance_records` | Vendor-facing maintenance history; each active row points at an `inventory_blocks` row. |
| `availability_calendar` | Legacy per-day status table; the availability engine no longer treats it as source of truth. |
| `favorite_products` | Planner ↔ product bookmark, `UNIQUE(planner_user_id, product_id)`. |

Images/documents continue to live outside PostgreSQL via `StorageService`; only public URLs are persisted.

## Inventory schema (V4)

Flyway migration: `backend/src/main/resources/db/migration/V4__inventory_availability.sql`

Extends V3 `inventory` with `created_at` and `version`. Adds reservation, block, movement, and maintenance tables. Indexes cover `product_variant_id`, `start_date`, `end_date`, `status`, and `booking_request_id`. Full rules: [`docs/inventory/README.md`](../inventory/README.md).

## Planner projects (V5)

Flyway: `backend/src/main/resources/db/migration/V5__planner_projects.sql`

`planner_id` → `users.id` (the authenticated planner). Soft delete on `projects.deleted_at`.

| Table | Notes |
| --- | --- |
| `projects` | Event workspace. Indexes on `planner_id`, `event_date`, `status`, `created_at`. Unique `(planner_id, slug)`. |
| `project_products` | Shortlist of variants. Price/name snapshots. `product_variant_id ON DELETE SET NULL`. Unique `(project_id, product_variant_id)`. **Does not reserve inventory.** |
| `project_vendors` | Saved vendors. Unique `(project_id, vendor_id)`. Unsaving does not cancel bookings. |
| `project_mood_boards` | One board per project (created with the project). |
| `project_mood_board_items` | Image URL + caption + `display_order`. Bytes in StorageService. |
| `project_notes` | Title/content, optional `pinned`. |
| `project_files` | Metadata only (`file_url`, name, type, size). |
| `project_activity` | Append-only timeline. |

Rules: [`docs/projects/project-business-rules.md`](../projects/project-business-rules.md).

## Booking requests (V6)

Flyway: `backend/src/main/resources/db/migration/V6__booking_requests.sql`

Does **not** recreate `inventory_reservations` (V4). Reservations are written only by `InventoryAvailabilityService`.

| Table | Notes |
| --- | --- |
| `booking_requests` | Planner → vendor request. Indexes on `planner_id`, `vendor_id`, `project_id`, `status`, `event_date`, `created_at`. |
| `booking_request_items` | Product/variant/price snapshots. `product_variant_id ON DELETE SET NULL`. Indexes on `booking_request_id`, `product_variant_id`. |
| `booking_request_status_history` | Timeline (`event_type` + from/to status). |
| `booking_request_notes` | Optional planner/vendor notes. |
| `booking_request_delivery_details` | Venue delivery/pickup details (1:1). |

Rules: [`docs/booking/booking-business-rules.md`](../booking/booking-business-rules.md).

## Orders and delivery (V7)

Flyway: `backend/src/main/resources/db/migration/V7__orders_and_delivery.sql`

Created from a CONFIRMED booking request. Does **not** duplicate `inventory_reservations`.

| Table | Notes |
| --- | --- |
| `order_number_counters` | `counter_year` PK + `last_value` for `FS-YYYY-NNNNNN`. |
| `orders` | Unique `order_number` and `booking_request_id`. Indexes on `order_number`, `booking_request_id`, `project_id`, `planner_id`, `vendor_id`, `status`, `delivery_date`, `pickup_date`, `created_at`. |
| `order_items` | Price/name snapshots. `product_variant_id ON DELETE SET NULL`. |
| `order_status_history` | Timeline (`event_type` + from/to status). |
| `deliveries` | 1:1 with order. Venue/contact/status. |
| `delivery_status_history` | Delivery transitions. |
| `delivery_proofs` | Metadata only (`file_url`); bytes in StorageService. |
| `pickup_details` | 1:1 with order. |
| `order_notes` | Planner / vendor / admin notes. |

`total_amount` = rental subtotal + optional delivery fee. Security deposit is informational and not included.

Rules: [`docs/orders/`](../orders/README.md), [`docs/delivery/delivery-workflow.md`](../delivery/delivery-workflow.md).

## Notifications schema (V8)

Flyway: `backend/src/main/resources/db/migration/V8__notifications.sql`

| Table | Notes |
| --- | --- |
| `notifications` | Personal inbox. `recipient_user_id`, typed `type`, `action_url`, JSON `metadata` TEXT, `is_read`. |
| `notification_preferences` | One row per user; five boolean groups, default true. |
| `notification_templates` | Title/body templates per `NotificationType`. |
| `notification_delivery_log` | Channel attempt log. MVP writes `IN_APP` / `SENT`. |

Notifications, `project_activity`, and `audit_logs` stay separate. See [`docs/notifications/`](../notifications/notification-architecture.md).

## Reviews schema (V9)

Flyway: `backend/src/main/resources/db/migration/V9__reviews.sql`

| Table | Notes |
| --- | --- |
| `reviews` | One row per order (`UNIQUE order_id`). Integer star columns. Soft delete. Nullable `vendor_response_*` for a future reply feature. |
| `review_reports` | Reporter, typed reason, OPEN/RESOLVED/DISMISSED. |
| `review_moderation_actions` | Admin hide/restore/remove/note history. |

Indexes: `vendor_id`, `planner_id`, `order_id`, `status`, `created_at`, `overall_rating`, plus `(vendor_id, status, created_at DESC)` for the public profile.

`vendors.rating` / `review_count` are a cache recomputed from published reviews. Rules: [`docs/reviews/`](../reviews/review-business-rules.md).

## Admin platform schema (V10)

Flyway: `backend/src/main/resources/db/migration/V10__admin_platform.sql`

| Table / column | Notes |
| --- | --- |
| `users.last_login_at` | Set on successful login |
| `vendors.verification_status` | PENDING / UNDER_REVIEW / VERIFIED / REJECTED / SUSPENDED. `verified` is derived |
| `vendors.featured` | Admin featured vendor |
| `vendor_documents.review_reason` | Admin document review |
| `products.view_count`, `moderation_reason` | Public GET increments views; reject/suspend reason |
| `categories.featured`, status `ARCHIVED` | Archive keeps products assigned |
| `audit_logs` | Immutable. Indexes on created_at, actor, resource |
| `support_categories`, `support_tickets`, `support_messages` | Ticket workflow |
| `platform_settings` | Typed singleton (not a generic key/value bag) |

Also indexed: user status (V1), vendor status (V2), product status (V3), review status (V9), support ticket status (V10).

Rules: [`docs/admin/`](../admin/README.md), [`docs/security/`](../security/README.md).
