# Marketplace search

`GET /api/v1/search` — public endpoint for discovering published products.

## Query parameters

| Param | Type | Notes |
| --- | --- | --- |
| `keyword` | string | Matches product `name`, `shortDescription`, `description` (case-insensitive `LIKE`) |
| `categoryId` | UUID | Exact category match |
| `subcategoryId` | UUID | Exact subcategory match |
| `vendorId` | UUID | Restrict to one vendor |
| `city` | string | Matches a vendor service area city |
| `minPrice` / `maxPrice` | decimal | `pricePerDay` range |
| `availableFrom` / `availableTo` | ISO date | See "Availability heuristic" below |
| `verifiedVendor` | boolean | Vendor `verified` flag |
| `minRating` | decimal | Vendor `rating` floor |
| `sort` | `NEWEST` \| `POPULARITY` \| `PRICE_ASC` \| `PRICE_DESC` \| `RATING` | Defaults to `NEWEST` |
| `page` / `size` | int | Defaults `0` / `12`, capped at `50` |

Response: `PageResponse<ProductSummaryResponse>` (same envelope as vendor search).

## Architecture — port/adapter so Elasticsearch can replace JPA later

```
SearchController
      │
      ▼
MarketplaceSearchService        (com.flowset.marketplace.search.service)
      │  builds SearchCriteria, resolves Sort, wraps result in PageResponse
      ▼
ProductSearchPort                (interface — com.flowset.marketplace.search)
      │
      ▼
JpaProductSearchAdapter          (today's implementation)
      │  combines ProductSpecifications, delegates to ProductRepository
      ▼
ProductRepository (JpaSpecificationExecutor<ProductEntity>)
```

- `ProductSpecifications` holds every filter as an independent, composable `Specification<ProductEntity>`
  (`keyword`, `categoryId`, `city`, `priceBetween`, `verifiedVendor`, `minRating`, `availableBetween`, …),
  combined with `ProductSpecifications.allOf(...)`.
- `ProductSearchPort` is the seam: swapping to Elasticsearch/OpenSearch later means writing an
  `EsProductSearchAdapter implements ProductSearchPort` and wiring it in as the `@Primary`/only bean —
  `MarketplaceSearchService` and `SearchController` do not change.
- Always filters to `deletedAt IS NULL` and `status = PUBLISHED`; drafts/archived products never appear in
  public search results regardless of other filters.

## Sort resolution (MVP heuristics)

| `sort` | Behavior |
| --- | --- |
| `NEWEST` | `createdAt DESC` |
| `PRICE_ASC` / `PRICE_DESC` | `pricePerDay` |
| `RATING` | `vendor.rating DESC` (nested Spring Data sort path) |
| `POPULARITY` | `featured DESC, createdAt DESC` — placeholder until a dedicated popularity/booking-count metric exists |

## Availability heuristic

`availableFrom`/`availableTo` are treated as a pragmatic MVP filter rather than full booking-engine logic:
a product matches when it owns **at least one `ACTIVE` variant with no `BLOCKED`/`MAINTENANCE`
`availability_calendar` entry inside the requested date range** (`ProductSpecifications.availableBetween`,
implemented as a correlated `NOT EXISTS` subquery). It does not currently cross-check `RESERVED` day-level
quantities against `inventory.available` — that belongs to the future booking/checkout module and should
tighten this specification without changing the controller or `MarketplaceSearchService` contract.

## Testing

`ProductSpecificationsTest` asserts the individual specifications (`keyword` + `priceBetween` + `categoryId`,
`verifiedVendor` + `minRating`) filter correctly when combined, and that `DRAFT` products are excluded.
`MarketplaceIntegrationTest` exercises the full HTTP path: publish a product, then confirm `GET /search`
returns it.
