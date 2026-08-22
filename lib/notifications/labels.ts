import { ROUTES } from "@/constants";
import type { UserRole } from "@/types";
import type {
  NotificationDto,
  NotificationFilter,
  NotificationType,
} from "@/services/notification-api";

export const NOTIFICATION_FILTER_LABELS: Record<NotificationFilter, string> = {
  ALL: "All",
  UNREAD: "Unread",
  BOOKINGS: "Bookings",
  ORDERS: "Orders",
  DELIVERY: "Delivery",
  INVENTORY: "Inventory",
  SYSTEM: "System",
};

export function formatUnreadBadge(count: number): string | null {
  if (count <= 0) return null;
  if (count > 99) return "99+";
  return String(count);
}

export function notificationsInboxHref(role: UserRole): string {
  switch (role) {
    case "vendor":
      return ROUTES.vendor.notifications;
    case "admin":
      return ROUTES.admin.notifications;
    default:
      return ROUTES.planner.notifications;
  }
}

export function notificationActionHref(
  notification: Pick<NotificationDto, "actionUrl">
): string | null {
  if (!notification.actionUrl) return null;
  return notification.actionUrl.startsWith("/")
    ? notification.actionUrl
    : `/${notification.actionUrl}`;
}

export function formatNotificationTime(iso: string, now = new Date()): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  const delta = now.getTime() - date.getTime();
  const minutes = Math.floor(delta / 60_000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Yesterday";
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

export function notificationErrorMessage(
  error: unknown,
  fallback = "Could not load notifications"
): string {
  if (error && typeof error === "object" && "status" in error) {
    const status = (error as { status?: number }).status;
    const message = "message" in error ? String((error as { message?: string }).message) : "";
    if (status === 401) return "Please sign in to continue";
    if (status === 404) return "Notification not found";
    if (message) return message;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

export function notificationIconKind(
  type: NotificationType
): "booking" | "order" | "delivery" | "inventory" | "system" {
  if (type.includes("BOOKING")) return "booking";
  if (type === "LOW_INVENTORY" || type === "INVENTORY_UNAVAILABLE") return "inventory";
  if (
    type.includes("DELIVERY") ||
    type.includes("PICKUP") ||
    type === "OUT_FOR_DELIVERY" ||
    type === "ORDER_DELIVERED"
  ) {
    return "delivery";
  }
  if (type.includes("ORDER")) return "order";
  return "system";
}
