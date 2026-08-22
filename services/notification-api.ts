import { apiClient } from "@/services/api-client";
import type { PageResponse } from "@/services/marketplace-api";

export const NOTIFICATION_TYPES = [
  "BOOKING_REQUEST_CREATED",
  "NEW_BOOKING_REQUEST",
  "BOOKING_REQUEST_ACCEPTED",
  "BOOKING_REQUEST_REJECTED",
  "BOOKING_REQUEST_CANCELLED",
  "INVENTORY_UNAVAILABLE",
  "ORDER_CONFIRMED",
  "DELIVERY_SCHEDULED",
  "OUT_FOR_DELIVERY",
  "ORDER_DELIVERED",
  "PICKUP_SCHEDULED",
  "ORDER_COMPLETED",
  "UPCOMING_DELIVERY",
  "DELIVERY_REMINDER",
  "PICKUP_REMINDER",
  "LOW_INVENTORY",
  "VENDOR_VERIFIED",
  "REVIEW_REMINDER",
  "REVIEW_ELIGIBLE",
  "REVIEW_RECEIVED",
  "REVIEW_REPORTED",
  "REVIEW_MODERATED",
  "SYSTEM_ANNOUNCEMENT",
  "NEW_VENDOR_REGISTRATION",
  "VENDOR_DOCUMENT_SUBMITTED",
  "VENDOR_REPORT",
  "SYSTEM_ALERT",
] as const;

export type NotificationType = (typeof NOTIFICATION_TYPES)[number];

export const NOTIFICATION_FILTERS = [
  "ALL",
  "UNREAD",
  "BOOKINGS",
  "ORDERS",
  "DELIVERY",
  "INVENTORY",
  "SYSTEM",
] as const;

export type NotificationFilter = (typeof NOTIFICATION_FILTERS)[number];

export type NotificationResourceType =
  | "BOOKING_REQUEST"
  | "ORDER"
  | "PROJECT"
  | "VENDOR"
  | "PRODUCT"
  | "INVENTORY"
  | "REVIEW"
  | "SYSTEM";

export interface NotificationDto {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  resourceType?: NotificationResourceType | null;
  resourceId?: string | null;
  actionUrl?: string | null;
  metadata?: Record<string, unknown> | null;
  isRead: boolean;
  readAt?: string | null;
  createdAt: string;
  expiresAt?: string | null;
}

export interface UnreadCountDto {
  unreadCount: number;
}

export interface NotificationPreferencesDto {
  bookingUpdates: boolean;
  orderUpdates: boolean;
  deliveryUpdates: boolean;
  inventoryAlerts: boolean;
  systemAnnouncements: boolean;
}

export const notificationApi = {
  list: (params: { filter?: NotificationFilter; page?: number; size?: number } = {}) =>
    apiClient.get<PageResponse<NotificationDto>>("/notifications", {
      params: {
        filter: params.filter ?? "ALL",
        page: params.page ?? 0,
        size: params.size ?? 20,
      },
    }),
  unreadCount: () => apiClient.get<UnreadCountDto>("/notifications/unread-count"),
  markRead: (id: string) => apiClient.patch<NotificationDto>(`/notifications/${id}/read`),
  markAllRead: () => apiClient.patch<UnreadCountDto>("/notifications/read-all"),
  delete: (id: string) => apiClient.delete<void>(`/notifications/${id}`),
  getPreferences: () => apiClient.get<NotificationPreferencesDto>("/notification-preferences"),
  updatePreferences: (payload: NotificationPreferencesDto) =>
    apiClient.put<NotificationPreferencesDto>("/notification-preferences", payload),
};
