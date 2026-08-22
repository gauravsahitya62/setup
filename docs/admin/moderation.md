# Moderation

## Products

Statuses: `DRAFT`, `PUBLISHED`, `PENDING_REVIEW`, `REJECTED`, `SUSPENDED`, `ARCHIVED`.

Admin (`POST /api/v1/admin/products/{id}/…`):

- Approve → `PUBLISHED`
- Reject → `REJECTED` (reason required)
- Suspend → `SUSPENDED`
- Restore → `PUBLISHED`
- Feature / unfeature

Vendors cannot set `featured` or admin-only statuses. Public GET increments `view_count` for anonymous/planner viewers.

### Public visibility (server-side)

A product is listed publicly only when:

- `status = PUBLISHED`
- `deleted_at` is null
- Vendor is not `SUSPENDED` and not deleted

Frontend filters are never the source of truth.

## Categories

Admin can create, edit, archive, reorder, change parent, upload an image, and feature.

Rules:

- Unique slugs
- No circular parent relationships
- Archive does **not** detach products; they stay assigned so they do not silently disappear
- Do not physically delete a category that still has products

## Reviews

See [`docs/reviews/review-moderation.md`](../reviews/review-moderation.md). Admin hide / restore / remove / note are audited. UI confirmation is required for hide and remove.

## Featured content

Reuse existing `featured` flags on vendors, products, and categories. Do not create a second marketplace catalog.

## Dangerous actions

Suspend, reject, archive, and remove require a confirmation dialog and a reason where specified. MVP does **not** offer bulk destructive actions.
