# FlowSet Architecture

## Style

Modular monolith (backend) + Next.js App Router (frontend).

```
Frontend (Next.js)  --HTTPS/JSON-->  Backend (Spring Boot)
                                         |
                                    PostgreSQL
```

## Backend layout

```
backend/
  src/main/java/com/flowset/
    auth/           # Auth use cases, controllers, DTOs
    users/          # User aggregate, profiles
    security/       # JWT, filters, SecurityFilterChain
    config/         # CORS, OpenAPI, app properties
    common/         # Shared primitives, enums
    exceptions/     # Problem responses, global advice
    notifications/  # In-app center + channel SPI; EmailNotificationService remains auth placeholders
    orders/         # Orders + delivery/pickup (`com.flowset.orders`)
    reviews/        # Vendor reviews + rating aggregation (`com.flowset.reviews`)
    planner/        # Planner projects + booking requests (`com.flowset.planner.booking`)
    vendor/         # Vendor storefront (profiles, portfolio, documents, storage)
    marketplace/    # Rental catalog: categories, products/variants, inventory,
                    # availability engine, search, favorites, collections (see
                    # docs/marketplace/README.md, docs/search/README.md,
                    # docs/inventory/README.md)
    storage/        # File storage abstraction (local now; S3/R2 later)
    admin/          # Dashboard, users, vendors, products, categories, reports
    audit/          # Immutable audit_logs
    support/        # Support tickets and messages
    settings/       # Typed platform_settings singleton
    shared/         # Cross-cutting DTOs / utils
```

### Vendor storefront

Public discovery and owner-managed digital storefront live in `com.flowset.vendor`.
Binary assets go through `StorageService` (never PostgreSQL). See `docs/vendor-module/README.md`.

### Layering (strict)

Controller → Service → Repository → Database

Controllers never inject repositories.

## Frontend layout

Existing folders under `app/`, `components/`, `services/`, `providers/`, `store/`. Reuse `api-client` patterns; do not duplicate domain UI.

The marketplace domain follows `vendor-api.ts` / `lib/vendor-server.ts`: `services/marketplace-api.ts`
(`marketplaceApi`) wraps every `/categories`, `/products`, `/search`, `/favorites`, `/collections`,
variant/inventory/availability endpoint, plus vendor inventory (`/vendor/inventory`, `/vendor/calendar`)
and public `GET /inventory/{variantId}/availability`. Planner projects live in `services/project-api.ts`
(`projectApi`) wrapping `/api/v1/projects/**`. Booking requests live in `services/booking-api.ts`
(`bookingApi`) wrapping `/api/v1/booking-requests` and `/api/v1/vendor/booking-requests`. Orders live in `services/order-api.ts`
(`orderApi`) wrapping `/api/v1/orders`, `/api/v1/vendor/orders`, `/api/v1/admin/orders`. Reviews live in `services/review-api.ts`
(`reviewApi`) wrapping `/api/v1/reviews`, `/api/v1/vendors/{id}/reviews`, `/api/v1/admin/reviews`. In-app notifications live in `services/notification-api.ts`
(`notificationApi`) wrapping `/api/v1/notifications` and `/api/v1/notification-preferences`. Admin operations live in `services/admin-api.ts`
(`adminApi`, `supportApi`) wrapping `/api/v1/admin/**` and `/api/v1/support/**`. See [`docs/admin/`](./admin/README.md). `lib/marketplace-server.ts` provides server-fetch
helpers for the App Router pages (`app/marketplace/[slug]`, `app/categories/[slug]`).

## Auth token strategy

| Token | Lifetime (default) | Storage |
| --- | --- | --- |
| Access JWT | 15 minutes | Frontend memory (Zustand, non-persisted) |
| Refresh JWT | 7 days | HttpOnly Secure SameSite cookie + server-side hash row |

## API versioning

All HTTP APIs under `/api/v1`.

## Configuration

No secrets in source. JWT secrets, DB credentials, and CORS origins come from environment variables.
