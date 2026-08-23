import { SkeletonLoader } from "@/components/shared/skeleton-loader";

export default function PlannerLoading() {
  return (
    <div className="space-y-6 p-4 sm:p-6">
      <div className="space-y-2">
        <div className="bg-muted h-8 w-48 animate-pulse rounded-lg" />
        <div className="bg-muted h-4 w-72 animate-pulse rounded-md" />
      </div>
      <SkeletonLoader variant="card" count={3} />
    </div>
  );
}
