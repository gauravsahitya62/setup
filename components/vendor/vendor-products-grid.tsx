"use client";

import * as React from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MarketplaceProductCard } from "@/components/marketplace/marketplace-product-card";
import { ProductQuickView } from "@/components/marketplace/product-quick-view";
import { AddToProjectDialog } from "@/components/marketplace/add-to-project-dialog";
import { EmptyState } from "@/components/shared/empty-state";
import type { MarketplaceListing } from "@/types";
import { ROUTES } from "@/constants";

interface VendorProductsGridProps {
  listings: MarketplaceListing[];
  vendorName: string;
  vendorId?: string;
}

export function VendorProductsGrid({ listings, vendorName, vendorId }: VendorProductsGridProps) {
  const [quickView, setQuickView] = React.useState<MarketplaceListing | null>(null);
  const [addTarget, setAddTarget] = React.useState<MarketplaceListing | null>(null);

  return (
    <>
      <Card>
        <CardHeader className="flex-row items-end justify-between gap-4 space-y-0">
          <div>
            <CardTitle className="text-lg">Products</CardTitle>
            <p className="text-muted-foreground mt-1 text-sm">
              Rental inventory from {vendorName} available on FlowSet.
            </p>
          </div>
          <Button variant="outline" size="sm" className="rounded-xl" asChild>
            <Link
              href={
                vendorId
                  ? `${ROUTES.marketplace}?q=${encodeURIComponent(vendorName)}&vendorId=${encodeURIComponent(vendorId)}`
                  : `${ROUTES.marketplace}?q=${encodeURIComponent(vendorName)}`
              }
            >
              View in marketplace
            </Link>
          </Button>
        </CardHeader>
        <CardContent>
          {listings.length === 0 ? (
            <EmptyState
              title="No products listed yet"
              description="This vendor hasn’t published catalog items on FlowSet."
            />
          ) : (
            <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {listings.map((listing) => (
                <MarketplaceProductCard
                  key={listing.id}
                  listing={listing}
                  onQuickView={setQuickView}
                  onAddToProject={setAddTarget}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProductQuickView
        listing={quickView}
        open={Boolean(quickView)}
        onOpenChange={(open) => !open && setQuickView(null)}
        onAddToProject={setAddTarget}
      />
      <AddToProjectDialog
        listing={addTarget}
        open={Boolean(addTarget)}
        onOpenChange={(open) => !open && setAddTarget(null)}
      />
    </>
  );
}
