import { z } from "zod";

export const MAX_REVIEW_TEXT_LENGTH = 2000;

const rating = z
  .number()
  .int()
  .min(1, "Rating must be between 1 and 5")
  .max(5, "Rating must be between 1 and 5");

const optionalRating = z.number().int().min(1).max(5).optional().nullable();

export const reviewFormSchema = z.object({
  overallRating: rating,
  communicationRating: optionalRating,
  productQualityRating: optionalRating,
  deliveryRating: optionalRating,
  valueRating: optionalRating,
  reviewText: z
    .string()
    .trim()
    .max(MAX_REVIEW_TEXT_LENGTH, `Review must be at most ${MAX_REVIEW_TEXT_LENGTH} characters`)
    .optional()
    .or(z.literal("")),
});

export type ReviewFormValues = z.infer<typeof reviewFormSchema>;

export const reportReviewSchema = z.object({
  reason: z.enum(["SPAM", "ABUSIVE", "FAKE", "INAPPROPRIATE", "PERSONAL_INFORMATION", "OTHER"]),
  description: z.string().trim().max(2000).optional().or(z.literal("")),
});

export type ReportReviewFormValues = z.infer<typeof reportReviewSchema>;
