# Review rating storage

## Status

Accepted

## Context

Reviews must not trust floating-point stars from the client. We need exact 1–5 scores, a vendor average for marketplace cards, and a distribution histogram.

## Decision

1. **Per-review stars** are `SMALLINT` (JPA `Integer`) with `CHECK (BETWEEN 1 AND 5)`. No `FLOAT`/`DOUBLE` on `reviews`.
2. **Vendor cache** reuses existing `vendors.rating NUMERIC(3,2)` and `vendors.review_count`. `VendorRatingService` sets them from `SUM(overall_rating) / COUNT(*)` of `PUBLISHED` rows with `deleted_at IS NULL`, `RoundingMode.HALF_UP` to two decimals.
3. MVP **recomputes from the database** on each publish/edit/hide/restore/remove. A denormalized aggregate table is a later optimization if profile traffic warrants it.
4. Public JSON exposes the cached average and a 1–5 `distribution` map. UI formats to one decimal and must not render `0.0` when `review_count = 0`.

## Consequences

- Integer stars cannot drift. Aggregates are still decimal because an average of integers is not an integer.
- Hidden or removed reviews drop out of the cache on the same transaction as the status change.
