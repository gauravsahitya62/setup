import { describe, expect, it } from "vitest";

import { reviewFormSchema } from "@/lib/reviews/schema";
import { averageRatingValue, distributionCounts, emptyRatingCopy } from "@/lib/reviews/labels";

describe("review form schema", () => {
  it("requires overall rating 1–5 and allows optional text", () => {
    expect(reviewFormSchema.safeParse({ overallRating: 5 }).success).toBe(true);
    expect(reviewFormSchema.safeParse({ overallRating: 0 }).success).toBe(false);
    expect(reviewFormSchema.safeParse({ overallRating: 6 }).success).toBe(false);
    expect(reviewFormSchema.safeParse({}).success).toBe(false);
    expect(
      reviewFormSchema.safeParse({ overallRating: 4, reviewText: "Great delivery." }).success
    ).toBe(true);
  });

  it("rejects review text over the maximum length", () => {
    const result = reviewFormSchema.safeParse({
      overallRating: 5,
      reviewText: "x".repeat(2001),
    });
    expect(result.success).toBe(false);
  });
});

describe("rating display", () => {
  it("hides 0.0 when there are no reviews", () => {
    expect(emptyRatingCopy(0)).toBe("No reviews yet");
    expect(emptyRatingCopy(3)).toBeNull();
  });

  it("fills missing distribution buckets from live summary data", () => {
    const counts = distributionCounts({
      averageRating: "4.70",
      reviewCount: 324,
      verifiedCount: 300,
      distribution: { "5": 250, "4": 50, "3": 10, "2": 8, "1": 6 },
    });
    expect(counts).toEqual({ 5: 250, 4: 50, 3: 10, 2: 8, 1: 6 });
    expect(
      averageRatingValue({
        averageRating: "4.70",
        reviewCount: 324,
        verifiedCount: 300,
        distribution: {},
      })
    ).toBe(4.7);
    expect(
      averageRatingValue({ averageRating: 0, reviewCount: 0, verifiedCount: 0, distribution: {} })
    ).toBe(0);
  });
});
