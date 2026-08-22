"use client";

import * as React from "react";
import { Star } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { Rating } from "@/components/shared/rating";
import { ReviewCard } from "@/components/reviews/review-card";
import { RatingDistribution } from "@/components/reviews/rating-distribution";
import { reviewErrorMessage } from "@/lib/reviews/labels";
import { reviewApi, type PublicReviewDto, type VendorRatingSummaryDto } from "@/services";
import { vendorApi } from "@/services/vendor-api";

export function VendorReviewsDashboard() {
  const [reviews, setReviews] = React.useState<PublicReviewDto[]>([]);
  const [summary, setSummary] = React.useState<VendorRatingSummaryDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [page, setPage] = React.useState(0);
  const [totalPages, setTotalPages] = React.useState(0);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const me = await vendorApi.getMine();
      const result = await reviewApi.public.list(me.id, { page, size: 12, sort: "NEWEST" });
      setReviews(result.content);
      setSummary(result.summary);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(reviewErrorMessage(err));
      setReviews([]);
    } finally {
      setLoading(false);
    }
  }, [page]);

  React.useEffect(() => {
    void load();
  }, [load]);

  if (loading) return <SkeletonLoader variant="text" count={6} />;
  if (error) {
    return (
      <EmptyState
        title="Could not load reviews"
        description={error}
        actionLabel="Retry"
        onAction={() => void load()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Reviews</h1>
        <p className="text-muted-foreground text-sm">
          Feedback from planners who completed a booking with you.
        </p>
      </div>
      {summary ? (
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-base">Rating summary</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-6 sm:grid-cols-2">
            <div>
              <Rating
                value={Number(summary.averageRating)}
                reviewCount={summary.reviewCount}
                size="md"
              />
              <p className="text-muted-foreground mt-2 text-sm">
                {summary.verifiedCount} verified bookings
              </p>
            </div>
            <RatingDistribution summary={summary} />
          </CardContent>
        </Card>
      ) : null}
      {reviews.length === 0 ? (
        <EmptyState
          title="No reviews yet"
          description="Reviews from completed bookings will appear here."
          icon={<Star className="size-5" />}
        />
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <ReviewCard key={review.id} review={review} />
          ))}
        </div>
      )}
      {totalPages > 1 ? (
        <div className="flex justify-center gap-2">
          <Button
            variant="outline"
            className="rounded-xl"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Previous
          </Button>
          <Button
            variant="outline"
            className="rounded-xl"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      ) : null}
    </div>
  );
}
