import type { VendorRatingSummaryDto } from "@/services/review-api";

export const REVIEW_STATUS_LABELS: Record<string, string> = {
  PENDING: "Pending",
  PUBLISHED: "Published",
  HIDDEN: "Hidden",
  REMOVED: "Removed",
  REPORTED: "Reported",
};

export const REVIEW_SORT_LABELS: Record<string, string> = {
  NEWEST: "Newest",
  HIGHEST: "Highest rated",
  LOWEST: "Lowest rated",
};

export const REVIEW_REPORT_REASON_LABELS: Record<string, string> = {
  SPAM: "Spam",
  ABUSIVE: "Abusive",
  FAKE: "Fake",
  INAPPROPRIATE: "Inappropriate",
  PERSONAL_INFORMATION: "Personal information",
  OTHER: "Other",
};

export function formatReviewDate(iso?: string | null): string {
  if (!iso) return "";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export function reviewErrorMessage(error: unknown, fallback = "Could not load reviews"): string {
  if (error && typeof error === "object" && "status" in error) {
    const status = (error as { status?: number }).status;
    const message = "message" in error ? String((error as { message?: string }).message) : "";
    if (status === 401) return "Please sign in to continue";
    if (status === 404) return "Review not found";
    if (status === 409 && message) return message;
    if (message) return message;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

export function emptyRatingCopy(reviewCount: number): string | null {
  if (reviewCount > 0) return null;
  return "No reviews yet";
}

export function distributionCounts(
  summary?: VendorRatingSummaryDto | null
): Record<number, number> {
  const result: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  if (!summary?.distribution) return result;
  for (const [key, value] of Object.entries(summary.distribution)) {
    const stars = Number(key);
    if (stars >= 1 && stars <= 5) {
      result[stars] = Number(value) || 0;
    }
  }
  return result;
}

export function averageRatingValue(summary?: VendorRatingSummaryDto | null, fallback = 0): number {
  if (!summary || summary.reviewCount <= 0) return fallback;
  return Number(summary.averageRating ?? 0);
}
