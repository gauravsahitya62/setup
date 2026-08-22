"use client";

import type { ReactNode } from "react";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Rating } from "@/components/shared/rating";
import { formatReviewDate } from "@/lib/reviews/labels";

export interface ReviewCardData {
  id: string;
  plannerDisplayName: string;
  plannerInitials: string;
  overallRating: number;
  reviewText?: string | null;
  verified?: boolean;
  createdAt?: string | null;
  publishedAt?: string | null;
}

interface ReviewCardProps {
  review: ReviewCardData;
  actions?: ReactNode;
}

export function ReviewCard({ review, actions }: ReviewCardProps) {
  return (
    <article className="border-border/60 bg-muted/20 rounded-2xl border p-5">
      <div className="flex items-start gap-3">
        <Avatar>
          <AvatarFallback>
            {review.plannerInitials || review.plannerDisplayName.charAt(0)}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-semibold tracking-tight">{review.plannerDisplayName}</p>
              {review.verified ? (
                <Badge className="rounded-md border-0 bg-emerald-500/15 text-emerald-700 dark:text-emerald-300">
                  Verified Booking
                </Badge>
              ) : null}
            </div>
            <time className="text-muted-foreground text-xs">
              {formatReviewDate(review.publishedAt ?? review.createdAt)}
            </time>
          </div>
          <div className="mt-3">
            <Rating value={review.overallRating} />
          </div>
          {review.reviewText ? (
            <p className="text-muted-foreground mt-3 text-sm leading-relaxed">
              {review.reviewText}
            </p>
          ) : null}
          {actions ? <div className="mt-4">{actions}</div> : null}
        </div>
      </div>
    </article>
  );
}
