import { SkeletonLoader } from "@/components/shared/skeleton-loader";

export default function AdminLoading() {
  return (
    <div className="space-y-6 p-4 sm:p-6">
      <div className="bg-muted h-8 w-40 animate-pulse rounded-lg" />
      <SkeletonLoader variant="list" count={6} />
    </div>
  );
}
