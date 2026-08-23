import { SkeletonLoader } from "@/components/shared/skeleton-loader";

export default function VendorLoading() {
  return (
    <div className="space-y-6 p-4 sm:p-6">
      <div className="bg-muted h-8 w-56 animate-pulse rounded-lg" />
      <SkeletonLoader variant="card" count={3} />
    </div>
  );
}
