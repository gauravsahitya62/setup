"use client";

import * as React from "react";
import { useSearchParams } from "next/navigation";

import { CategoryChips } from "@/components/marketplace/category-chips";
import { MarketplaceFiltersBar } from "@/components/marketplace/marketplace-filters";
import { MarketplaceProductGrid } from "@/components/marketplace/marketplace-product-grid";
import { MarketplaceSearch } from "@/components/marketplace/marketplace-search";
import { EmptyState } from "@/components/shared/empty-state";
import { LoadingSpinner } from "@/components/shared/loading-spinner";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import {
  DEFAULT_MARKETPLACE_FILTERS,
  MARKETPLACE_CITIES,
  MARKETPLACE_PAGE_SIZE,
} from "@/constants/marketplace";
import { useDebounce } from "@/hooks/use-debounce";
import { useFavorites } from "@/hooks/use-favorites";
import { useInfiniteScroll } from "@/hooks/use-infinite-scroll";
import {
  ApiClientError,
  flattenCategorySlugMap,
  mapProductSummaryToListing,
  marketplaceApi,
  vendorApi,
  type CategoryDto,
} from "@/services";
import type { MarketplaceFilters, MarketplaceListing } from "@/types";

export function MarketplaceExperience({ projectId }: { projectId?: string }) {
  const searchParams = useSearchParams();
  const vendorIdFromUrl = searchParams.get("vendorId") ?? undefined;
  const [filters, setFilters] = React.useState<MarketplaceFilters>(() => {
    const q = searchParams.get("q") ?? "";
    const location = searchParams.get("location") ?? "";
    const matchedCity = MARKETPLACE_CITIES.find(
      (city) => city.toLowerCase() === location.toLowerCase()
    );

    return {
      ...DEFAULT_MARKETPLACE_FILTERS,
      query: q,
      city: matchedCity ?? "all",
    };
  });

  const [categories, setCategories] = React.useState<CategoryDto[]>([]);
  const categorySlugToId = React.useMemo(() => flattenCategorySlugMap(categories), [categories]);
  const categoryChips = React.useMemo(
    () => [
      { slug: "all", label: "All" },
      ...categories.map((category) => ({ slug: category.slug, label: category.name })),
    ],
    [categories]
  );

  const [listings, setListings] = React.useState<MarketplaceListing[]>([]);
  const [page, setPage] = React.useState(0);
  const [totalElements, setTotalElements] = React.useState(0);
  const [loadingInitial, setLoadingInitial] = React.useState(true);
  const [loadingMore, setLoadingMore] = React.useState(false);
  const [loadError, setLoadError] = React.useState<string | null>(null);

  const { favoriteIds, toggleFavorite } = useFavorites();

  const debouncedQuery = useDebounce(filters.query, 250);
  const activeFilters = React.useMemo(
    () => ({ ...filters, query: debouncedQuery }),
    [filters, debouncedQuery]
  );

  React.useEffect(() => {
    let cancelled = false;
    async function loadCategories() {
      try {
        const tree = await marketplaceApi.categories.tree();
        if (!cancelled) setCategories(tree);
      } catch {
        if (!cancelled) setCategories([]);
      }
    }
    void loadCategories();
    return () => {
      cancelled = true;
    };
  }, []);

  const fetchPage = React.useCallback(
    async (pageToLoad: number, signal?: AbortSignal) => {
      const categoryId =
        activeFilters.categorySlug !== "all"
          ? categorySlugToId.get(activeFilters.categorySlug)
          : undefined;

      const query = activeFilters.query.trim();
      let vendorId = vendorIdFromUrl;
      let keyword = query || undefined;

      if (!vendorId && query) {
        try {
          const vendors = await vendorApi.list({ companyName: query, page: 0, size: 20 });
          const needle = query.toLowerCase();
          const match = vendors.content.find(
            (vendor) =>
              vendor.companyName.toLowerCase() === needle ||
              vendor.companyName.toLowerCase().includes(needle) ||
              vendor.slug.toLowerCase().includes(needle.replace(/\s+/g, "-"))
          );
          if (match) {
            vendorId = match.id;
          }
        } catch {
          // Continue with product keyword search if vendor lookup fails.
        }
      }

      if (vendorId) {
        keyword = undefined;
      }

      return marketplaceApi.search(
        {
          keyword,
          vendorId,
          categoryId,
          city: activeFilters.city !== "all" ? activeFilters.city : undefined,
          minPrice:
            activeFilters.priceMin > DEFAULT_MARKETPLACE_FILTERS.priceMin
              ? activeFilters.priceMin
              : undefined,
          maxPrice:
            activeFilters.priceMax < DEFAULT_MARKETPLACE_FILTERS.priceMax
              ? activeFilters.priceMax
              : undefined,
          verifiedVendor: activeFilters.verifiedOnly || undefined,
          minRating: activeFilters.minRating > 0 ? activeFilters.minRating : undefined,
          page: pageToLoad,
          size: MARKETPLACE_PAGE_SIZE,
        },
        signal
      );
    },
    [activeFilters, categorySlugToId, vendorIdFromUrl]
  );

  // Reset + load page 0 whenever filters change (categories must be resolved first
  // so category-slug filters map to a real categoryId on first render).
  React.useEffect(() => {
    const controller = new AbortController();
    let cancelled = false;

    async function loadFirstPage() {
      setLoadingInitial(true);
      setLoadError(null);
      try {
        const result = await fetchPage(0, controller.signal);
        if (cancelled) return;
        const availableOnly = activeFilters.availableOnly
          ? result.content.filter((item) => item.available)
          : result.content;
        setListings(availableOnly.map(mapProductSummaryToListing));
        setTotalElements(result.totalElements);
        setPage(0);
      } catch (error) {
        if (cancelled || error instanceof DOMException) return;
        setListings([]);
        setTotalElements(0);
        setLoadError(error instanceof ApiClientError ? error.message : "Could not load listings");
      } finally {
        if (!cancelled) setLoadingInitial(false);
      }
    }

    void loadFirstPage();
    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [fetchPage, activeFilters.availableOnly]);

  const hasMore = listings.length < totalElements;

  const loadMore = React.useCallback(() => {
    if (!hasMore || loadingMore || loadingInitial) return;
    setLoadingMore(true);
    const nextPage = page + 1;
    void fetchPage(nextPage)
      .then((result) => {
        const availableOnly = activeFilters.availableOnly
          ? result.content.filter((item) => item.available)
          : result.content;
        setListings((prev) => [...prev, ...availableOnly.map(mapProductSummaryToListing)]);
        setTotalElements(result.totalElements);
        setPage(nextPage);
      })
      .catch(() => undefined)
      .finally(() => setLoadingMore(false));
  }, [hasMore, loadingMore, loadingInitial, page, fetchPage, activeFilters.availableOnly]);

  const sentinelRef = useInfiniteScroll(loadMore, hasMore);

  const updateFilters = (next: MarketplaceFilters) => {
    setFilters(next);
  };

  const resetFilters = () => {
    setFilters(DEFAULT_MARKETPLACE_FILTERS);
  };

  return (
    <div
      className={projectId ? "space-y-6" : "mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10 lg:px-8"}
    >
      <div className="animate-in fade-in slide-in-from-bottom-3 mb-8 max-w-2xl duration-500">
        <p className="text-muted-foreground text-sm font-medium">
          {projectId ? "Project marketplace" : "Marketplace"}
        </p>
        <h1 className="font-heading mt-2 text-3xl font-semibold tracking-tight text-balance sm:text-4xl">
          {projectId ? "Add products to this event" : "Find everything your event needs"}
        </h1>
        <p className="text-muted-foreground mt-3 text-base leading-relaxed text-pretty">
          {projectId
            ? "Selecting a product adds it to the current project. Inventory is not reserved."
            : "Browse verified rental inventory by category, city, price, and availability — then add pieces to a project."}
        </p>
      </div>

      <div className="border-border/50 bg-background/90 sticky top-16 z-30 -mx-4 space-y-4 border-b px-4 py-4 backdrop-blur-xl sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8">
        <MarketplaceSearch
          query={filters.query}
          city={filters.city}
          cities={MARKETPLACE_CITIES}
          onQueryChange={(query) => updateFilters({ ...filters, query })}
          onCityChange={(city) => updateFilters({ ...filters, city })}
        />
        <CategoryChips
          categories={categoryChips}
          value={filters.categorySlug}
          onChange={(categorySlug) => updateFilters({ ...filters, categorySlug })}
        />
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <MarketplaceFiltersBar
            filters={filters}
            onChange={updateFilters}
            onReset={resetFilters}
          />
          <p className="text-muted-foreground text-sm tabular-nums">
            {loadingInitial
              ? "Searching…"
              : `${totalElements} rental${totalElements === 1 ? "" : "s"}`}
          </p>
        </div>
      </div>

      <div className="mt-8">
        {loadingInitial ? (
          <SkeletonLoader
            variant="card"
            count={MARKETPLACE_PAGE_SIZE}
            className="grid gap-5 space-y-0 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
          />
        ) : loadError ? (
          <EmptyState
            title="Couldn't load the marketplace"
            description={loadError}
            actionLabel="Try again"
            onAction={resetFilters}
          />
        ) : listings.length === 0 ? (
          <EmptyState
            title="No rentals match your filters"
            description="Try a different city, clear advanced filters, or search a broader category."
            actionLabel="Clear filters"
            onAction={resetFilters}
          />
        ) : (
          <>
            <MarketplaceProductGrid
              listings={listings}
              favoriteIds={favoriteIds}
              onToggleFavorite={toggleFavorite}
              projectId={projectId}
            />

            {hasMore ? (
              <div ref={sentinelRef} className="flex justify-center py-10">
                {loadingMore ? <LoadingSpinner label="Loading more rentals" /> : null}
              </div>
            ) : (
              <p className="text-muted-foreground py-10 text-center text-sm">
                You&apos;ve reached the end of results
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}
