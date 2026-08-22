"use client";

import * as React from "react";
import Link from "next/link";
import { ReviewForm } from "@/components/reviews/review-form";
import { ROUTES } from "@/constants";
import { reviewApi, type EligibleReviewDto, type ReviewDto } from "@/services";

interface OrderReviewPromptProps {
  orderId: string;
  vendorName: string;
}

export function OrderReviewPrompt({ orderId, vendorName }: OrderReviewPromptProps) {
  const [eligible, setEligible] = React.useState<EligibleReviewDto | null>(null);
  const [existing, setExisting] = React.useState<ReviewDto | null>(null);
  const [loading, setLoading] = React.useState(true);

  const load = React.useCallback(async () => {
    setLoading(true);
    try {
      const [eligibility, mine] = await Promise.all([
        reviewApi.eligibility(orderId).catch(() => null),
        reviewApi.listMine({ orderId, size: 1 }),
      ]);
      setEligible(eligibility);
      setExisting(mine.content[0] ?? null);
    } catch {
      setEligible(null);
      setExisting(null);
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  React.useEffect(() => {
    void load();
  }, [load]);

  if (loading) return null;
  if (existing) {
    return (
      <section className="border-border/70 rounded-2xl border p-5">
        <ReviewForm
          orderId={orderId}
          vendorName={vendorName}
          existing={existing}
          readOnly={!existing.canEdit}
          onSubmitted={() => void load()}
        />
      </section>
    );
  }
  if (eligible?.reviewPeriodExpired) {
    return (
      <section className="border-border/70 rounded-2xl border p-5">
        <h2 className="text-lg font-semibold">Review period expired</h2>
        <p className="text-muted-foreground mt-1 text-sm">
          Reviews can be submitted for 30 days after the order is completed.
        </p>
      </section>
    );
  }
  if (!eligible) return null;

  return (
    <section className="border-border/70 rounded-2xl border p-5">
      <ReviewForm orderId={orderId} vendorName={vendorName} onSubmitted={() => void load()} />
      <p className="text-muted-foreground mt-3 text-xs">
        Or open{" "}
        <Link className="underline" href={ROUTES.planner.reviews}>
          all reviews
        </Link>
        .
      </p>
    </section>
  );
}
