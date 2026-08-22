"use client";

import { distributionCounts } from "@/lib/reviews/labels";
import { cn } from "@/lib/utils";
import type { VendorRatingSummaryDto } from "@/services/review-api";

interface RatingDistributionProps {
  summary: VendorRatingSummaryDto;
  className?: string;
}

export function RatingDistribution({ summary, className }: RatingDistributionProps) {
  const counts = distributionCounts(summary);
  const total = summary.reviewCount || 1;
  return (
    <div className={cn("space-y-1.5", className)} aria-label="Rating distribution">
      {([5, 4, 3, 2, 1] as const).map((stars) => {
        const count = counts[stars] ?? 0;
        const width = Math.round((count / total) * 100);
        return (
          <div key={stars} className="flex items-center gap-2 text-xs">
            <span className="text-muted-foreground w-6 tabular-nums">{stars} ★</span>
            <div className="bg-muted h-2 flex-1 overflow-hidden rounded-full">
              <div className="h-full rounded-full bg-amber-400" style={{ width: `${width}%` }} />
            </div>
            <span className="text-muted-foreground w-8 text-right tabular-nums">{count}</span>
          </div>
        );
      })}
    </div>
  );
}
