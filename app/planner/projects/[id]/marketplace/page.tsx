import { Suspense } from "react";

import { MarketplaceExperience } from "@/components/marketplace";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";

interface ProjectMarketplacePageProps {
  params: Promise<{ id: string }>;
}

function MarketplaceFallback() {
  return (
    <SkeletonLoader variant="card" count={8} className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4" />
  );
}

export default async function ProjectMarketplacePage({ params }: ProjectMarketplacePageProps) {
  const { id } = await params;
  return (
    <Suspense fallback={<MarketplaceFallback />}>
      <MarketplaceExperience projectId={id} />
    </Suspense>
  );
}
