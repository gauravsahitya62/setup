# Vendor module

Digital storefront for rental vendors. Planners use the public profile to decide whether to trust and book.

## Backend package

`com.flowset.vendor`

- `controller` — REST endpoints (Swagger annotated)
- `service` — business rules, ownership checks, search
- `repository` — Spring Data JPA
- `entity` / `dto` / `mapper` — persistence + API shapes
- `validation` — Bean Validation on requests

Cross-cutting:

- `com.flowset.storage.StorageService` — local filesystem today; S3 / R2 / MinIO later without frontend changes
- `AuditLogService` — logs profile/logo/banner/portfolio/document events

## Business rules

- Soft delete only (`deleted_at`).
- One storefront per owner user.
- **Vendor registration automatically creates a DRAFT storefront** (`vendors` row) with company name + slug from `businessName`.
- Public list/slug endpoints return **PUBLISHED** vendors (owners/admins can still load drafts).
- Only owner or ADMIN mutates profile, media, areas, social links, documents.
- Document uploads store metadata in DB; bytes via StorageService.
- Status flow: DRAFT → PUBLISHED (wizard “Publish”); SUSPENDED by admin.

## Frontend routes

| Path | Purpose |
| --- | --- |
| `/vendor/profile` | Company profile overview |
| `/vendor/profile/wizard` | 7-step setup + Save as draft |
| `/vendor/service-areas` | Manage locations |
| `/vendor/portfolio` | Manage gallery |
| `/vendor/documents` | Upload verification docs |
| `/vendor/social` | Social URLs |
| `/vendor/settings` | Shortcuts into storefront tools |
| `/vendors` | Public directory (API-backed) |
| `/vendors/[slug]` | Public profile (API-backed), including live reviews |

`vendors.rating` / `review_count` are maintained by `VendorRatingService` from published reviews. See [`docs/reviews/`](../reviews/review-business-rules.md). Vendors cannot edit or delete planner reviews.

Wizard steps: Business → Service areas → Social → Logo → Banner → Portfolio → Review & publish.

## Client integration

- `services/vendor-api.ts` — Axios client + DTO mappers
- `lib/vendor-server.ts` — RSC `fetch` for public pages
- `hooks/use-my-vendor.ts` — owner load helper

## Testing

Backend: unit (`SlugUtils`), repository/integration (`VendorIntegrationTest`) under `backend/src/test`.

Run: `mvn -f backend test`
