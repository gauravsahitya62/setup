"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { RatingInput } from "@/components/reviews/rating-input";
import {
  MAX_REVIEW_TEXT_LENGTH,
  reviewFormSchema,
  type ReviewFormValues,
} from "@/lib/reviews/schema";
import { reviewErrorMessage } from "@/lib/reviews/labels";
import { ApiClientError, reviewApi, type ReviewDto } from "@/services";

interface ReviewFormProps {
  orderId: string;
  vendorName?: string;
  existing?: ReviewDto | null;
  onSubmitted?: (review: ReviewDto) => void;
  readOnly?: boolean;
}

export function ReviewForm({
  orderId,
  vendorName,
  existing,
  onSubmitted,
  readOnly,
}: ReviewFormProps) {
  const [submitting, setSubmitting] = React.useState(false);
  const form = useForm<ReviewFormValues>({
    resolver: zodResolver(reviewFormSchema),
    defaultValues: {
      overallRating: existing?.overallRating ?? 0,
      communicationRating: existing?.communicationRating ?? undefined,
      productQualityRating: existing?.productQualityRating ?? undefined,
      deliveryRating: existing?.deliveryRating ?? undefined,
      valueRating: existing?.valueRating ?? undefined,
      reviewText: existing?.reviewText ?? "",
    },
  });

  const disabled = Boolean(readOnly || submitting || (existing && !existing.canEdit));

  const submit = form.handleSubmit(async (values) => {
    if (submitting) return;
    setSubmitting(true);
    try {
      const payload = {
        overallRating: values.overallRating,
        communicationRating: values.communicationRating || undefined,
        productQualityRating: values.productQualityRating || undefined,
        deliveryRating: values.deliveryRating || undefined,
        valueRating: values.valueRating || undefined,
        reviewText: values.reviewText?.trim() || undefined,
      };
      const dto = existing
        ? await reviewApi.update(existing.id, payload)
        : await reviewApi.create({ orderId, ...payload });
      toast.success(existing ? "Review updated" : "Review published");
      onSubmitted?.(dto);
    } catch (err) {
      toast.error(
        err instanceof ApiClientError
          ? err.message
          : reviewErrorMessage(err, "Could not submit review")
      );
    } finally {
      setSubmitting(false);
    }
  });

  return (
    <form onSubmit={submit} className="space-y-5">
      <div>
        <h2 className="text-lg font-semibold">How was your experience?</h2>
        {vendorName ? <p className="text-muted-foreground mt-1 text-sm">{vendorName}</p> : null}
      </div>
      <RatingInput
        label="Overall"
        required
        value={form.watch("overallRating")}
        disabled={disabled}
        onChange={(value) => form.setValue("overallRating", value, { shouldValidate: true })}
      />
      {form.formState.errors.overallRating ? (
        <p className="text-destructive text-sm">{form.formState.errors.overallRating.message}</p>
      ) : null}
      <div className="grid gap-4 sm:grid-cols-2">
        <RatingInput
          label="Communication"
          value={form.watch("communicationRating")}
          disabled={disabled}
          onChange={(value) => form.setValue("communicationRating", value)}
        />
        <RatingInput
          label="Product quality"
          value={form.watch("productQualityRating")}
          disabled={disabled}
          onChange={(value) => form.setValue("productQualityRating", value)}
        />
        <RatingInput
          label="Delivery"
          value={form.watch("deliveryRating")}
          disabled={disabled}
          onChange={(value) => form.setValue("deliveryRating", value)}
        />
        <RatingInput
          label="Value for money"
          value={form.watch("valueRating")}
          disabled={disabled}
          onChange={(value) => form.setValue("valueRating", value)}
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor={`review-text-${orderId}`}>Review (optional)</Label>
        <Textarea
          id={`review-text-${orderId}`}
          rows={4}
          maxLength={MAX_REVIEW_TEXT_LENGTH}
          disabled={disabled}
          placeholder="What went well? What could be better?"
          {...form.register("reviewText")}
        />
        <p className="text-muted-foreground text-xs">
          {(form.watch("reviewText") ?? "").length}/{MAX_REVIEW_TEXT_LENGTH}
        </p>
      </div>
      {disabled && existing && !existing.canEdit ? (
        <p className="text-muted-foreground text-sm">
          This review is read-only. The edit window has expired.
        </p>
      ) : (
        <Button type="submit" className="rounded-xl" disabled={disabled}>
          {submitting ? <Loader2 className="size-4 animate-spin" /> : null}
          {existing ? "Update review" : "Submit review"}
        </Button>
      )}
    </form>
  );
}
