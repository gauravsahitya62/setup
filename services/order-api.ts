import { apiClient } from "@/services/api-client";
import type { PageResponse } from "@/services/marketplace-api";
import type { DeliveryStatus, OrderStatus, PickupStatus } from "@/lib/orders/labels";
import { toMoney } from "@/lib/booking/pricing";

export interface OrderItemDto {
  id: string;
  productVariantId?: string | null;
  productName: string;
  variantName: string;
  sku?: string | null;
  quantity: number;
  pricePerDay: number | string;
  rentalDays: number;
  subtotal: number | string;
  createdAt: string;
}

export interface DeliveryProofDto {
  id: string;
  fileUrl: string;
  fileName: string;
  fileType: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface DeliveryDto {
  id: string;
  orderId: string;
  deliveryDate?: string | null;
  preferredTime?: string | null;
  actualDeliveryTime?: string | null;
  venueName?: string | null;
  venueAddress?: string | null;
  city?: string | null;
  state?: string | null;
  contactPerson?: string | null;
  contactPhone?: string | null;
  deliveryNotes?: string | null;
  status: DeliveryStatus;
  createdAt: string;
  updatedAt: string;
  proofs: DeliveryProofDto[];
}

export interface PickupDto {
  id: string;
  orderId: string;
  pickupDate?: string | null;
  preferredTime?: string | null;
  actualPickupTime?: string | null;
  notes?: string | null;
  status: PickupStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrderTimelineEventDto {
  id: string;
  eventType: string;
  fromStatus?: OrderStatus | null;
  toStatus?: OrderStatus | null;
  description: string;
  note?: string | null;
  actorId?: string | null;
  createdAt: string;
}

export interface OrderNoteDto {
  id: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export interface OrderActionsDto {
  canCancel: boolean;
  canScheduleDelivery: boolean;
  canMarkOutForDelivery: boolean;
  canMarkDelivered: boolean;
  canMarkEventCompleted: boolean;
  canSchedulePickup: boolean;
  canMarkPickedUp: boolean;
  canUploadProof: boolean;
  canUpdateDelivery: boolean;
  canAddNote: boolean;
}

export interface OrderSummaryDto {
  id: string;
  orderNumber: string;
  bookingRequestId: string;
  projectId: string;
  projectName?: string | null;
  vendorId: string;
  vendorName?: string | null;
  vendorLogoUrl?: string | null;
  plannerId: string;
  plannerName?: string | null;
  status: OrderStatus;
  eventDate?: string | null;
  rentalStartDate: string;
  rentalEndDate: string;
  deliveryDate?: string | null;
  pickupDate?: string | null;
  itemsSummary: string;
  itemCount: number;
  estimatedSubtotal: number | string;
  deliveryFee: number | string;
  securityDeposit: number | string;
  totalAmount: number | string;
  currency: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderDto extends OrderSummaryDto {
  eventType?: string | null;
  guestCount?: number | null;
  vendorSlug?: string | null;
  rentalDays: number;
  plannerNotes?: string | null;
  vendorNotes?: string | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  items: OrderItemDto[];
  delivery?: DeliveryDto | null;
  pickup?: PickupDto | null;
  timeline: OrderTimelineEventDto[];
  notes: OrderNoteDto[];
  actions: OrderActionsDto;
}

export interface DeliveryHistoryDto {
  id: string;
  fromStatus?: DeliveryStatus | null;
  toStatus?: DeliveryStatus | null;
  changedBy?: string | null;
  note?: string | null;
  createdAt: string;
}

export interface CalendarEntryDto {
  orderId: string;
  orderNumber: string;
  orderStatus: OrderStatus;
  kind: string;
  date: string;
  preferredTime?: string | null;
  city?: string | null;
  venueName?: string | null;
  plannerName?: string | null;
  projectName?: string | null;
  deliveryStatus?: DeliveryStatus | null;
  pickupStatus?: PickupStatus | null;
  totalAmount: number | string;
  currency: string;
}

export interface VendorDeliveryCalendarDto {
  date: string;
  todayDeliveries: CalendarEntryDto[];
  upcomingDeliveries: CalendarEntryDto[];
  pendingPickups: CalendarEntryDto[];
  completedDeliveries: CalendarEntryDto[];
}

export interface ScheduleDeliveryPayload {
  deliveryDate: string;
  preferredTime?: string;
  venueName?: string;
  venueAddress?: string;
  city?: string;
  state?: string;
  contactPerson?: string;
  contactPhone?: string;
  deliveryNotes?: string;
  deliveryFee?: number;
  securityDeposit?: number;
}

export interface SchedulePickupPayload {
  pickupDate: string;
  preferredTime?: string;
  notes?: string;
}

export function mapOrderSummary(dto: OrderSummaryDto) {
  return {
    ...dto,
    estimatedSubtotal: toMoney(dto.estimatedSubtotal),
    deliveryFee: toMoney(dto.deliveryFee),
    securityDeposit: toMoney(dto.securityDeposit),
    totalAmount: toMoney(dto.totalAmount),
  };
}

export const orderApi = {
  list: (
    params: { projectId?: string; status?: OrderStatus | ""; page?: number; size?: number } = {}
  ) =>
    apiClient.get<PageResponse<OrderSummaryDto>>("/orders", {
      params: {
        projectId: params.projectId || undefined,
        status: params.status || undefined,
        page: params.page ?? 0,
        size: params.size ?? 20,
      },
    }),
  get: (id: string) => apiClient.get<OrderDto>(`/orders/${id}`),
  cancel: (id: string) => apiClient.post<OrderDto>(`/orders/${id}/cancel`),
  addNote: (id: string, content: string) =>
    apiClient.post<OrderNoteDto>(`/orders/${id}/notes`, { content }),
  delivery: {
    get: (orderId: string) => apiClient.get<DeliveryDto | null>(`/orders/${orderId}/delivery`),
    history: (orderId: string) =>
      apiClient.get<DeliveryHistoryDto[]>(`/orders/${orderId}/delivery/history`),
    update: (orderId: string, payload: ScheduleDeliveryPayload) =>
      apiClient.put<DeliveryDto>(`/orders/${orderId}/delivery`, payload),
    uploadProof: (orderId: string, file: File) => {
      const form = new FormData();
      form.append("file", file);
      return apiClient.post<DeliveryProofDto>(`/orders/${orderId}/delivery/proof`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      });
    },
  },
  vendor: {
    list: (
      params: { status?: OrderStatus | ""; projectId?: string; page?: number; size?: number } = {}
    ) =>
      apiClient.get<PageResponse<OrderSummaryDto>>("/vendor/orders", {
        params: {
          status: params.status || undefined,
          projectId: params.projectId || undefined,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    get: (id: string) => apiClient.get<OrderDto>(`/vendor/orders/${id}`),
    calendar: (params: { date?: string; status?: OrderStatus | ""; city?: string } = {}) =>
      apiClient.get<VendorDeliveryCalendarDto>("/vendor/orders/calendar", {
        params: {
          date: params.date || undefined,
          status: params.status || undefined,
          city: params.city || undefined,
        },
      }),
    scheduleDelivery: (id: string, payload: ScheduleDeliveryPayload) =>
      apiClient.post<OrderDto>(`/vendor/orders/${id}/schedule-delivery`, payload),
    outForDelivery: (id: string) =>
      apiClient.post<OrderDto>(`/vendor/orders/${id}/out-for-delivery`),
    markDelivered: (id: string) => apiClient.post<OrderDto>(`/vendor/orders/${id}/mark-delivered`),
    markEventCompleted: (id: string) =>
      apiClient.post<OrderDto>(`/vendor/orders/${id}/mark-event-completed`),
    schedulePickup: (id: string, payload: SchedulePickupPayload) =>
      apiClient.post<OrderDto>(`/vendor/orders/${id}/schedule-pickup`, payload),
    markPickedUp: (id: string) => apiClient.post<OrderDto>(`/vendor/orders/${id}/mark-picked-up`),
  },
  admin: {
    list: (params: { status?: OrderStatus | ""; page?: number; size?: number } = {}) =>
      apiClient.get<PageResponse<OrderSummaryDto>>("/admin/orders", {
        params: {
          status: params.status || undefined,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    get: (id: string) => apiClient.get<OrderDto>(`/admin/orders/${id}`),
  },
};
