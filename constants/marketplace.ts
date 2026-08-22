import type { MarketplaceFilters, MarketplaceListing } from "@/types";

/**
 * Static city options used to seed the marketplace location filter.
 * Vendor-derived city facets can replace this list once the search API
 * exposes a city aggregation endpoint.
 */
export const MARKETPLACE_CITIES = [
  "San Francisco, CA",
  "Los Angeles, CA",
  "Austin, TX",
  "Miami, FL",
  "New York, NY",
  "Chicago, IL",
] as const;

export const MARKETPLACE_PRICE_BOUNDS = { min: 0, max: 500 } as const;

export const DEFAULT_MARKETPLACE_FILTERS: MarketplaceFilters = {
  query: "",
  categorySlug: "all",
  city: "all",
  priceMin: MARKETPLACE_PRICE_BOUNDS.min,
  priceMax: MARKETPLACE_PRICE_BOUNDS.max,
  availableOnly: false,
  minRating: 0,
  verifiedOnly: false,
};

export const MARKETPLACE_PAGE_SIZE = 12;

/**
 * @deprecated Mock marketplace inventory has been removed. Live listings are
 * fetched from `marketplaceApi.search` (see `services/marketplace-api.ts`)
 * and mapped with `mapProductSummaryToListing`. Kept as an empty, typed
 * export so any lingering references fail closed instead of crashing.
 */
export const MARKETPLACE_LISTINGS: MarketplaceListing[] = [];

/**
 * @deprecated Category chips are populated from `GET /categories` at
 * runtime. This is only a safe fallback for the "All" chip.
 */
export const MARKETPLACE_CATEGORIES: { slug: string; label: string }[] = [
  { slug: "all", label: "All" },
];

export function filterMarketplaceListings(
  listings: MarketplaceListing[],
  filters: MarketplaceFilters
): MarketplaceListing[] {
  const query = filters.query.trim().toLowerCase();

  return listings.filter((listing) => {
    if (filters.categorySlug !== "all" && listing.categorySlug !== filters.categorySlug) {
      return false;
    }
    if (filters.city !== "all" && listing.city !== filters.city) {
      return false;
    }
    if (listing.dailyRate < filters.priceMin || listing.dailyRate > filters.priceMax) {
      return false;
    }
    if (filters.availableOnly && !listing.available) {
      return false;
    }
    if (filters.minRating > 0 && listing.rating < filters.minRating) {
      return false;
    }
    if (filters.verifiedOnly && !listing.vendorVerified) {
      return false;
    }
    if (query) {
      const haystack = [
        listing.name,
        listing.description,
        listing.vendorName,
        listing.city,
        listing.categoryName,
      ]
        .join(" ")
        .toLowerCase();
      if (!haystack.includes(query)) return false;
    }
    return true;
  });
}
