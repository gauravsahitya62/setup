"use client";

import * as React from "react";

import { AddToProjectDialog } from "@/components/marketplace/add-to-project-dialog";
import { MarketplaceProductCard } from "@/components/marketplace/marketplace-product-card";
import { ProductQuickView } from "@/components/marketplace/product-quick-view";
import { EmptyState } from "@/components/shared/empty-state";
import type { MarketplaceListing } from "@/types";
import { cn } from "@/lib/utils";

interface MarketplaceProductGridProps {
  listings: MarketplaceListing[];
  favoriteIds?: Set<string>;
  onToggleFavorite?: (listing: MarketplaceListing) => void;
  emptyTitle?: string;
  emptyDescription?: string;
  className?: string;
  projectId?: string;
}

/**
 * Shared grid of marketplace product cards with quick-view and
 * add-to-project dialogs wired in. Reused by the marketplace search
 * experience, category pages, product detail "related products", and the
 * planner favorites page so those surfaces stay in sync with the same
 * card design without re-implementing dialog state everywhere.
 */
export function MarketplaceProductGrid({
  listings,
  favoriteIds,
  onToggleFavorite,
  emptyTitle = "No rentals to show",
  emptyDescription = "Check back soon as vendors publish more inventory.",
  className,
  projectId,
}: MarketplaceProductGridProps) {
  const [quickView, setQuickView] = React.useState<MarketplaceListing | null>(null);
  const [addTarget, setAddTarget] = React.useState<MarketplaceListing | null>(null);

  if (listings.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <>
      <div className={cn("grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4", className)}>
        {listings.map((listing) => (
          <MarketplaceProductCard
            key={listing.id}
            listing={listing}
            onQuickView={setQuickView}
            onAddToProject={setAddTarget}
            isFavorite={favoriteIds?.has(listing.id)}
            onToggleFavorite={onToggleFavorite}
          />
        ))}
      </div>

      <ProductQuickView
        listing={quickView}
        open={Boolean(quickView)}
        onOpenChange={(open) => {
          if (!open) setQuickView(null);
        }}
        onAddToProject={setAddTarget}
      />

      <AddToProjectDialog
        listing={addTarget}
        open={Boolean(addTarget)}
        onOpenChange={(open) => {
          if (!open) setAddTarget(null);
        }}
        projectId={projectId}
      />
    </>
  );
}
