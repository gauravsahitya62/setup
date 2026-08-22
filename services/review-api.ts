import { apiClient } from "@/services/api-client";
import type { PageResponse } from "@/services/marketplace-api";

export const REVIEW_STATUSES = ["PENDING", "PUBLISHED", "HIDDEN", "REMOVED", "REPORTED"] as const;
export type ReviewStatus = (typeof REVIEW_STATUSES)[number];

export const REVIEW_SORTS = ["NEWEST", "HIGHEST", "LOWEST"] as const;
export type ReviewSort = (typeof REVIEW_SORTS)[number];

export const REVIEW_REPORT_REASONS = [
  "SPAM",
  "ABUSIVE",
  "FAKE",
  "INAPPROPRIATE",
  "PERSONAL_INFORMATION",
  "OTHER",
] as const;
export type ReviewReportReason = (typeof REVIEW_REPORT_REASONS)[number];

export const REVIEW_MODERATION_ACTIONS = ["HIDE", "RESTORE", "REMOVE", "NOTE"] as const;
export type ReviewModerationAction = (typeof REVIEW_MODERATION_ACTIONS)[number];

export interface PublicReviewDto {
  id: string;
  vendorId: string;
  plannerDisplayName: string;
  plannerInitials: string;
  overallRating: number;
  communicationRating?: number | null;
  productQualityRating?: number | null;
  deliveryRating?: number | null;
  valueRating?: number | null;
  reviewText?: string | null;
  verified: boolean;
  createdAt: string;
  publishedAt?: string | null;
}

export interface VendorRatingSummaryDto {
  averageRating: number | string;
  reviewCount: number;
  verifiedCount: number;
  distribution: Record<string, number>;
}

export interface VendorReviewPageDto {
  content: PublicReviewDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  summary: VendorRatingSummaryDto;
}

export interface ReviewDto {
  id: string;
  orderId: string;
  vendorId: string;
  vendorName?: string | null;
  plannerId: string;
  plannerDisplayName: string;
  plannerInitials: string;
  overallRating: number;
  communicationRating?: number | null;
  productQualityRating?: number | null;
  deliveryRating?: number | null;
  valueRating?: number | null;
  reviewText?: string | null;
  status: ReviewStatus;
  verified: boolean;
  canEdit: boolean;
  editExpiresAt?: string | null;
  createdAt: string;
  updatedAt: string;
  publishedAt?: string | null;
  deletedAt?: string | null;
}

export interface EligibleReviewDto {
  orderId: string;
  orderNumber: string;
  vendorId: string;
  vendorName?: string | null;
  vendorSlug?: string | null;
  completedAt?: string | null;
  eligibleUntil?: string | null;
  reviewPeriodExpired: boolean;
}

export interface CreateReviewPayload {
  orderId: string;
  overallRating: number;
  communicationRating?: number;
  productQualityRating?: number;
  deliveryRating?: number;
  valueRating?: number;
  reviewText?: string;
}

export interface UpdateReviewPayload {
  overallRating?: number;
  communicationRating?: number;
  productQualityRating?: number;
  deliveryRating?: number;
  valueRating?: number;
  reviewText?: string;
}

export interface ReviewReportDto {
  id: string;
  reviewId: string;
  reportedBy: string;
  reason: ReviewReportReason;
  description?: string | null;
  status: string;
  createdAt: string;
  resolvedAt?: string | null;
}

export interface ReviewModerationActionDto {
  id: string;
  reviewId: string;
  actorId: string;
  action: ReviewModerationAction;
  note?: string | null;
  previousStatus?: ReviewStatus | null;
  newStatus?: ReviewStatus | null;
  createdAt: string;
}

export const reviewApi = {
  public: {
    list: (
      vendorId: string,
      params?: {
        rating?: number;
        verified?: boolean;
        sort?: ReviewSort;
        page?: number;
        size?: number;
      }
    ) => apiClient.get<VendorReviewPageDto>(`/vendors/${vendorId}/reviews`, { params }),
  },
  eligible: () => apiClient.get<EligibleReviewDto[]>("/reviews/eligible"),
  eligibility: (orderId: string) =>
    apiClient.get<EligibleReviewDto>(`/reviews/eligible/${orderId}`),
  listMine: (params?: { status?: ReviewStatus; orderId?: string; page?: number; size?: number }) =>
    apiClient.get<PageResponse<ReviewDto>>("/reviews", { params }),
  get: (id: string) => apiClient.get<ReviewDto>(`/reviews/${id}`),
  create: (payload: CreateReviewPayload) => apiClient.post<ReviewDto>("/reviews", payload),
  update: (id: string, payload: UpdateReviewPayload) =>
    apiClient.patch<ReviewDto>(`/reviews/${id}`, payload),
  remove: (id: string, reason: string) => apiClient.delete<ReviewDto>(`/reviews/${id}`, { reason }),
  report: (id: string, payload: { reason: ReviewReportReason; description?: string }) =>
    apiClient.post<ReviewReportDto>(`/reviews/${id}/report`, payload),
  admin: {
    list: (params?: {
      status?: ReviewStatus;
      vendorId?: string;
      plannerId?: string;
      page?: number;
      size?: number;
    }) => apiClient.get<PageResponse<ReviewDto>>("/admin/reviews", { params }),
    reported: (params?: { page?: number; size?: number }) =>
      apiClient.get<PageResponse<ReviewDto>>("/admin/reviews/reported", { params }),
    moderate: (id: string, payload: { action: ReviewModerationAction; note?: string }) =>
      apiClient.patch<ReviewDto>(`/admin/reviews/${id}/moderate`, payload),
    history: (id: string) =>
      apiClient.get<ReviewModerationActionDto[]>(`/admin/reviews/${id}/history`),
    reports: (id: string) => apiClient.get<ReviewReportDto[]>(`/admin/reviews/${id}/reports`),
  },
};
