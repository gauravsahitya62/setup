"use client";

import Link from "next/link";

import { Button } from "@/components/ui/button";
import { MarketplaceProductGrid } from "@/components/marketplace";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { useFavorites } from "@/hooks/use-favorites";

export default function PlannerFavoritesPage() {
  const { favorites, favoriteIds, loading, toggleFavorite } = useFavorites();

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1.5">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Favorites</h1>
          <p className="text-muted-foreground text-sm">
            Products you&apos;ve saved from the marketplace for quick access.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.marketplace}>Browse marketplace</Link>
        </Button>
      </div>

      {loading ? (
        <SkeletonLoader
          variant="card"
          count={8}
          className="grid gap-5 space-y-0 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
        />
      ) : (
        <MarketplaceProductGrid
          listings={favorites}
          favoriteIds={favoriteIds}
          onToggleFavorite={toggleFavorite}
          emptyTitle="No favorites yet"
          emptyDescription="Tap the heart icon on any product in the marketplace to save it here."
        />
      )}
    </div>
  );
}
