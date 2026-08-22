"use client";

import { MarketplaceProductGrid } from "@/components/marketplace/marketplace-product-grid";
import { useFavorites } from "@/hooks/use-favorites";
import type { MarketplaceListing } from "@/types";

interface ConnectedProductGridProps {
  listings: MarketplaceListing[];
  emptyTitle?: string;
  emptyDescription?: string;
}

/**
 * Client-side boundary that wires planner favorites for a list of listings
 * fetched by a server component (e.g. a category page). Keeps the parent
 * page a server component while still supporting the favorite toggle.
 */
export function ConnectedProductGrid({
  listings,
  emptyTitle,
  emptyDescription,
}: ConnectedProductGridProps) {
  const { favoriteIds, toggleFavorite } = useFavorites();

  return (
    <MarketplaceProductGrid
      listings={listings}
      favoriteIds={favoriteIds}
      onToggleFavorite={toggleFavorite}
      emptyTitle={emptyTitle}
      emptyDescription={emptyDescription}
    />
  );
}
