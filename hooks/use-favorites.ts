"use client";

import * as React from "react";
import { toast } from "sonner";

import { useAuth } from "@/providers";
import { ApiClientError, mapProductSummaryToListing, marketplaceApi } from "@/services";
import type { MarketplaceListing } from "@/types";

/**
 * Wires planner favorites for any marketplace surface. Only planners can
 * favorite products (matches backend `@PreAuthorize("hasAnyRole('PLANNER','ADMIN')")`),
 * so `onToggleFavorite` is `undefined` for guests/vendors and product cards
 * simply omit the heart button in that case.
 */
export function useFavorites() {
  const { isAuthenticated, role } = useAuth();
  const canFavorite = isAuthenticated && role === "planner";

  const [favoriteIds, setFavoriteIds] = React.useState<Set<string>>(new Set());
  const [favorites, setFavorites] = React.useState<MarketplaceListing[]>([]);
  const [loading, setLoading] = React.useState(canFavorite);

  const reload = React.useCallback(async () => {
    if (!canFavorite) {
      setFavoriteIds(new Set());
      setFavorites([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const page = await marketplaceApi.favorites.list(0, 100);
      const listings = page.content.map(mapProductSummaryToListing);
      setFavorites(listings);
      setFavoriteIds(new Set(listings.map((item) => item.id)));
    } catch {
      setFavorites([]);
      setFavoriteIds(new Set());
    } finally {
      setLoading(false);
    }
  }, [canFavorite]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const toggleFavorite = React.useCallback(
    async (listing: MarketplaceListing) => {
      if (!canFavorite) return;
      const alreadyFavorite = favoriteIds.has(listing.id);

      setFavoriteIds((prev) => {
        const next = new Set(prev);
        if (alreadyFavorite) next.delete(listing.id);
        else next.add(listing.id);
        return next;
      });

      try {
        if (alreadyFavorite) {
          await marketplaceApi.favorites.remove(listing.id);
          setFavorites((prev) => prev.filter((item) => item.id !== listing.id));
          toast.success("Removed from favorites");
        } else {
          await marketplaceApi.favorites.add(listing.id);
          setFavorites((prev) => [listing, ...prev.filter((item) => item.id !== listing.id)]);
          toast.success("Saved to favorites");
        }
      } catch (error) {
        // revert optimistic update
        setFavoriteIds((prev) => {
          const next = new Set(prev);
          if (alreadyFavorite) next.add(listing.id);
          else next.delete(listing.id);
          return next;
        });
        toast.error(error instanceof ApiClientError ? error.message : "Could not update favorites");
      }
    },
    [canFavorite, favoriteIds]
  );

  return {
    canFavorite,
    favoriteIds,
    favorites,
    loading,
    toggleFavorite: canFavorite ? toggleFavorite : undefined,
    reload,
  };
}
