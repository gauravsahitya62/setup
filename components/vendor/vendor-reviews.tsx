"use client";

import * as React from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { Rating } from "@/components/shared/rating";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ReviewCard } from "@/components/reviews/review-card";
import { ReportReviewButton } from "@/components/reviews/report-review-button";
import { RatingDistribution } from "@/components/reviews/rating-distribution";
import { REVIEW_SORT_LABELS, reviewErrorMessage } from "@/lib/reviews/labels";
import {
  reviewApi,
  type PublicReviewDto,
  type ReviewSort,
  type VendorRatingSummaryDto,
} from "@/services";

interface VendorReviewsProps {
  vendorId: string;
  rating: number;
  reviewCount: number;
}

export function VendorReviews({ vendorId, rating, reviewCount }: VendorReviewsProps) {
  const [reviews, setReviews] = React.useState<PublicReviewDto[]>([]);
  const [summary, setSummary] = React.useState<VendorRatingSummaryDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [sort, setSort] = React.useState<ReviewSort>("NEWEST");
  const [ratingFilter, setRatingFilter] = React.useState<number | undefined>();
  const [verifiedOnly, setVerifiedOnly] = React.useState(false);
  const [page, setPage] = React.useState(0);
  const [totalPages, setTotalPages] = React.useState(0);
  const [total, setTotal] = React.useState(reviewCount);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await reviewApi.public.list(vendorId, {
        page,
        size: 8,
        sort,
        rating: ratingFilter,
        verified: verifiedOnly || undefined,
      });
      setReviews(result.content);
      setSummary(result.summary);
      setTotalPages(result.totalPages);
      setTotal(result.totalElements);
    } catch (err) {
      setError(reviewErrorMessage(err));
      setReviews([]);
    } finally {
      setLoading(false);
    }
  }, [vendorId, page, sort, ratingFilter, verifiedOnly]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const displayRating = summary ? Number(summary.averageRating) : rating;
  const displayCount = summary?.reviewCount ?? reviewCount;

  return (
    <Card>
      <CardHeader className="flex-row items-end justify-between gap-4 space-y-0">
        <div>
          <CardTitle className="text-lg">Reviews</CardTitle>
          <p className="text-muted-foreground mt-1 text-sm">
            What planners say after a completed booking.
          </p>
        </div>
        <Rating value={displayRating} reviewCount={displayCount} size="md" />
      </CardHeader>
      <CardContent className="space-y-6">
        {summary && displayCount > 0 ? (
          <div className="grid gap-6 sm:grid-cols-2">
            <div>
              <p className="text-muted-foreground text-sm">
                {summary.verifiedCount} verified bookings
              </p>
            </div>
            <RatingDistribution summary={summary} />
          </div>
        ) : null}
        <div className="flex flex-wrap gap-2">
          {(["NEWEST", "HIGHEST", "LOWEST"] as ReviewSort[]).map((option) => (
            <Button
              key={option}
              size="sm"
              variant={sort === option ? "default" : "outline"}
              className="rounded-xl"
              onClick={() => {
                setSort(option);
                setPage(0);
              }}
            >
              {REVIEW_SORT_LABELS[option]}
            </Button>
          ))}
          {[5, 4, 3, 2, 1].map((stars) => (
            <Button
              key={stars}
              size="sm"
              variant={ratingFilter === stars ? "default" : "outline"}
              className="rounded-xl"
              onClick={() => {
                setRatingFilter((current) => (current === stars ? undefined : stars));
                setPage(0);
              }}
            >
              {stars} ★
            </Button>
          ))}
          <Button
            size="sm"
            variant={verifiedOnly ? "default" : "outline"}
            className="rounded-xl"
            onClick={() => {
              setVerifiedOnly((value) => !value);
              setPage(0);
            }}
          >
            Verified only
          </Button>
        </div>
        {loading ? (
          <SkeletonLoader variant="text" count={4} />
        ) : error ? (
          <EmptyState
            title="Could not load reviews"
            description={error}
            actionLabel="Retry"
            onAction={() => void load()}
          />
        ) : reviews.length === 0 ? (
          <EmptyState
            title={displayCount === 0 ? "No reviews yet" : "No matching reviews"}
            description={
              displayCount === 0
                ? "This vendor has not received a verified review yet."
                : "Try a different filter."
            }
          />
        ) : (
          <div className="space-y-4">
            {reviews.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                actions={<ReportReviewButton reviewId={review.id} />}
              />
            ))}
          </div>
        )}
        {totalPages > 1 ? (
          <div className="text-muted-foreground flex items-center justify-between text-sm">
            <span>
              Page {page + 1} of {totalPages} · {total} reviews
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
