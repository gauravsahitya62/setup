import { apiClient } from "@/services/api-client";
import type { PageResponse } from "@/services/marketplace-api";
import type { BookingRejectionReason, BookingStatus } from "@/lib/booking/labels";
import { toMoney } from "@/lib/booking/pricing";
import type { BookingRequest } from "@/types";

export type BookingStatusDto = BookingStatus;
export type BookingRejectionReasonDto = BookingRejectionReason;

export interface BookingRequestItemDto {
  id: string;
  productVariantId?: string | null;
  productName: string;
  variantName: string;
  sku?: string | null;
  vendorId: string;
  quantity: number;
  pricePerDay: number | string;
  rentalStartDate: string;
  rentalEndDate: string;
  rentalDays: number;
  lineEstimatedValue: number | string;
  notes?: string | null;
  availabilityStatus?: "AVAILABLE" | "PARTIAL" | "UNAVAILABLE" | null;
  availableQuantity?: number | null;
  availabilityMessage?: string | null;
}

export interface BookingDeliveryDto {
  deliveryDate?: string | null;
  preferredDeliveryTime?: string | null;
  venueName?: string | null;
  venueAddress?: string | null;
  city?: string | null;
  state?: string | null;
  contactPerson?: string | null;
  contactPhone?: string | null;
  deliveryNotes?: string | null;
  pickupDate?: string | null;
  preferredPickupTime?: string | null;
}

export interface BookingTimelineEventDto {
  id: string;
  eventType: string;
  fromStatus?: BookingStatus | null;
  toStatus?: BookingStatus | null;
  description: string;
  note?: string | null;
  actorId?: string | null;
  createdAt: string;
}

export interface BookingNoteDto {
  id: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export interface BookingRequestSummaryDto {
  id: string;
  projectId: string;
  projectName?: string | null;
  vendorId: string;
  vendorName?: string | null;
  vendorLogoUrl?: string | null;
  plannerId: string;
  plannerName?: string | null;
  status: BookingStatus;
  eventDate?: string | null;
  rentalStartDate: string;
  rentalEndDate: string;
  itemsSummary: string;
  itemCount: number;
  estimatedRentalValue: number | string;
  currency: string;
  requestedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BookingRequestDto extends BookingRequestSummaryDto {
  eventType?: string | null;
  guestCount?: number | null;
  vendorSlug?: string | null;
  vendorVerified?: boolean;
  rentalDays: number;
  deliveryDate?: string | null;
  pickupDate?: string | null;
  venueName?: string | null;
  venueAddress?: string | null;
  city?: string | null;
  state?: string | null;
  plannerNotes?: string | null;
  vendorNotes?: string | null;
  rejectionReason?: BookingRejectionReason | null;
  respondedAt?: string | null;
  viewedAt?: string | null;
  cancelledAt?: string | null;
  orderId?: string | null;
  items: BookingRequestItemDto[];
  delivery?: BookingDeliveryDto | null;
  timeline: BookingTimelineEventDto[];
  notes: BookingNoteDto[];
}

export interface VendorBookingStatsDto {
  pending: number;
  accepted: number;
  rejected: number;
  cancelled: number;
  confirmed: number;
}

export interface CreateBookingRequestPayload {
  projectId: string;
  vendorId: string;
  items: { variantId: string; quantity: number; notes?: string }[];
  eventDate: string;
  rentalStartDate: string;
  rentalEndDate: string;
  venueName?: string;
  venueAddress?: string;
  city?: string;
  state?: string;
  plannerNotes?: string;
  delivery?: {
    deliveryDate?: string;
    preferredDeliveryTime?: string;
    venueName?: string;
    venueAddress?: string;
    city?: string;
    state?: string;
    contactPerson?: string;
    contactPhone?: string;
    deliveryNotes?: string;
    pickupDate?: string;
    preferredPickupTime?: string;
  };
}

export function mapBookingSummary(dto: BookingRequestSummaryDto): BookingRequest {
  return {
    id: dto.id,
    projectId: dto.projectId,
    projectName: dto.projectName ?? "Project",
    vendorId: dto.vendorId,
    vendorName: dto.vendorName ?? "Vendor",
    vendorLogoUrl: dto.vendorLogoUrl ?? undefined,
    plannerName: dto.plannerName ?? undefined,
    itemsSummary: dto.itemsSummary,
    status: dto.status,
    amount: toMoney(dto.estimatedRentalValue),
    currency: dto.currency,
    eventDate: dto.eventDate ?? undefined,
    requestedAt: dto.requestedAt ?? dto.createdAt,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

export const bookingApi = {
  create: (payload: CreateBookingRequestPayload) =>
    apiClient.post<BookingRequestDto>("/booking-requests", payload),
  list: (
    params: { projectId?: string; status?: BookingStatus | ""; page?: number; size?: number } = {}
  ) =>
    apiClient.get<PageResponse<BookingRequestSummaryDto>>("/booking-requests", {
      params: {
        projectId: params.projectId || undefined,
        status: params.status || undefined,
        page: params.page ?? 0,
        size: params.size ?? 20,
      },
    }),
  get: (id: string) => apiClient.get<BookingRequestDto>(`/booking-requests/${id}`),
  cancel: (id: string) => apiClient.post<BookingRequestDto>(`/booking-requests/${id}/cancel`),
  vendor: {
    list: (
      params: { status?: BookingStatus | ""; projectId?: string; page?: number; size?: number } = {}
    ) =>
      apiClient.get<PageResponse<BookingRequestSummaryDto>>("/vendor/booking-requests", {
        params: {
          status: params.status || undefined,
          projectId: params.projectId || undefined,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    stats: () => apiClient.get<VendorBookingStatsDto>("/vendor/booking-requests/stats"),
    get: (id: string) => apiClient.get<BookingRequestDto>(`/vendor/booking-requests/${id}`),
    accept: (id: string) =>
      apiClient.post<BookingRequestDto>(`/vendor/booking-requests/${id}/accept`),
    reject: (id: string, payload: { reason: BookingRejectionReason; note?: string }) =>
      apiClient.post<BookingRequestDto>(`/vendor/booking-requests/${id}/reject`, payload),
  },
};
