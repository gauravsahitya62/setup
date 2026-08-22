"use client";

import { Star } from "lucide-react";

import { cn } from "@/lib/utils";

interface RatingProps {
  value: number;
  max?: number;
  reviewCount?: number;
  size?: "sm" | "md";
  className?: string;
  emptyLabel?: string;
}

export function Rating({
  value,
  max = 5,
  reviewCount,
  size = "sm",
  className,
  emptyLabel = "No reviews yet",
}: RatingProps) {
  if (reviewCount === 0) {
    return <span className={cn("text-muted-foreground text-xs", className)}>{emptyLabel}</span>;
  }

  const clamped = Math.max(0, Math.min(value, max));
  const iconSize = size === "sm" ? "size-3.5" : "size-4";

  return (
    <div className={cn("inline-flex items-center gap-1.5", className)}>
      <div className="flex items-center gap-0.5" aria-label={`${clamped} out of ${max} stars`}>
        {Array.from({ length: max }).map((_, index) => {
          const filled = index + 1 <= Math.round(clamped);
          return (
            <Star
              key={index}
              className={cn(
                iconSize,
                filled ? "fill-amber-400 text-amber-400" : "fill-muted text-muted"
              )}
            />
          );
        })}
      </div>
      <span className="text-foreground text-xs font-medium tabular-nums">{clamped.toFixed(1)}</span>
      {typeof reviewCount === "number" ? (
        <span className="text-muted-foreground text-xs">({reviewCount})</span>
      ) : null}
    </div>
  );
}
