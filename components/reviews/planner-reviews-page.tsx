"use client";

import * as React from "react";
import Link from "next/link";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ReviewCard } from "@/components/reviews/review-card";
import { ReviewForm } from "@/components/reviews/review-form";
import { ROUTES } from "@/constants";
import { reviewErrorMessage } from "@/lib/reviews/labels";
import { ApiClientError, reviewApi, type EligibleReviewDto, type ReviewDto } from "@/services";

export function PlannerReviewsPage() {
  const [eligible, setEligible] = React.useState<EligibleReviewDto[]>([]);
  const [published, setPublished] = React.useState<ReviewDto[]>([]);
  const [history, setHistory] = React.useState<ReviewDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [activeOrderId, setActiveOrderId] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [toComplete, mine] = await Promise.all([
        reviewApi.eligible(),
        reviewApi.listMine({ size: 50 }),
      ]);
      setEligible(toComplete);
      setPublished(mine.content.filter((row) => row.status === "PUBLISHED" && !row.deletedAt));
      setHistory(mine.content);
    } catch (err) {
      setError(reviewErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void load();
  }, [load]);

  const requestDelete = async (id: string) => {
    const reason = window.prompt("Why are you removing this review?");
    if (!reason?.trim()) return;
    try {
      await reviewApi.remove(id, reason.trim());
      toast.success("Review removed");
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : reviewErrorMessage(err));
    }
  };

  if (loading) return <SkeletonLoader variant="text" count={8} />;
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
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Reviews</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Share feedback after a rental is completed. One review per order.
        </p>
      </div>
      <Tabs defaultValue="complete">
        <TabsList variant="line">
          <TabsTrigger value="complete">Reviews to complete ({eligible.length})</TabsTrigger>
          <TabsTrigger value="published">Published ({published.length})</TabsTrigger>
          <TabsTrigger value="history">History ({history.length})</TabsTrigger>
        </TabsList>
        <TabsContent value="complete" className="mt-6 space-y-4">
          {eligible.length === 0 ? (
            <EmptyState
              title="You're all caught up"
              description="Completed orders without a review will appear here."
              actionLabel="View orders"
              onAction={() => {
                window.location.href = ROUTES.planner.orders;
              }}
            />
          ) : (
            eligible.map((row) => (
              <div key={row.orderId} className="rounded-2xl border p-5">
                <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <p className="font-medium">{row.vendorName}</p>
                    <p className="text-muted-foreground text-xs">Order {row.orderNumber}</p>
                  </div>
                  <Button variant="outline" className="rounded-xl" asChild>
                    <Link href={ROUTES.planner.order(row.orderId)}>Order details</Link>
                  </Button>
                </div>
                {activeOrderId === row.orderId ? (
                  <ReviewForm
                    orderId={row.orderId}
                    vendorName={row.vendorName ?? undefined}
                    onSubmitted={() => void load()}
                  />
                ) : (
                  <Button className="rounded-xl" onClick={() => setActiveOrderId(row.orderId)}>
                    Leave a review
                  </Button>
                )}
              </div>
            ))
          )}
        </TabsContent>
        <TabsContent value="published" className="mt-6 space-y-4">
          {published.length === 0 ? (
            <EmptyState
              title="No published reviews"
              description="Reviews you submit will show up here."
            />
          ) : (
            published.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                actions={
                  <div className="flex flex-wrap gap-2">
                    {review.canEdit ? (
                      <Button size="sm" variant="outline" className="rounded-xl" asChild>
                        <Link href={ROUTES.planner.order(review.orderId)}>Edit</Link>
                      </Button>
                    ) : null}
                    <Button
                      size="sm"
                      variant="ghost"
                      className="rounded-xl"
                      onClick={() => void requestDelete(review.id)}
                    >
                      Remove
                    </Button>
                  </div>
                }
              />
            ))
          )}
        </TabsContent>
        <TabsContent value="history" className="mt-6 space-y-4">
          {history.length === 0 ? (
            <EmptyState
              title="No review history"
              description="Past reviews remain here even after removal."
            />
          ) : (
            history.map((review) => (
              <div key={review.id} className="space-y-2">
                <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">
                  {review.status}
                  {review.vendorName ? ` · ${review.vendorName}` : ""}
                </p>
                <ReviewCard review={review} />
              </div>
            ))
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
