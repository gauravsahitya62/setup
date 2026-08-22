import type { Metadata } from "next";
import { Suspense } from "react";

import { RootShell } from "@/components/layout";
import { MarketplaceExperience } from "@/components/marketplace";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Marketplace",
  description:
    "Search and compare verified event rental inventory by category, city, price, and availability.",
  path: "/marketplace",
});

function MarketplaceFallback() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <SkeletonLoader variant="text" count={2} className="mb-8 max-w-xl" />
      <SkeletonLoader
        variant="card"
        count={8}
        className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4"
      />
    </div>
  );
}

export default function MarketplacePage() {
  return (
    <RootShell>
      <Suspense fallback={<MarketplaceFallback />}>
        <MarketplaceExperience />
      </Suspense>
    </RootShell>
  );
}
