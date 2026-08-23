import { SkeletonLoader } from "@/components/shared/skeleton-loader";

export default function MarketplaceLoading() {
  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8">
      <div className="bg-muted h-10 w-64 animate-pulse rounded-lg" />
      <SkeletonLoader variant="card" count={3} />
    </div>
  );
}
