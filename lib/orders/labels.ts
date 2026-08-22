import {
  BadgeCheck,
  CalendarClock,
  CheckCircle2,
  CircleDashed,
  Flag,
  PackageCheck,
  Truck,
  Undo2,
  XCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

export const ORDER_STATUSES = [
  "PENDING",
  "CONFIRMED",
  "DELIVERY_SCHEDULED",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "EVENT_COMPLETED",
  "PICKUP_SCHEDULED",
  "PICKED_UP",
  "COMPLETED",
  "CANCELLED",
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  DELIVERY_SCHEDULED: "Delivery scheduled",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  EVENT_COMPLETED: "Event completed",
  PICKUP_SCHEDULED: "Pickup scheduled",
  PICKED_UP: "Picked up",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export const ORDER_STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING: "bg-muted text-muted-foreground",
  CONFIRMED: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
  DELIVERY_SCHEDULED: "bg-sky-500/10 text-sky-700 dark:text-sky-300",
  OUT_FOR_DELIVERY: "bg-amber-500/10 text-amber-800 dark:text-amber-300",
  DELIVERED: "bg-indigo-500/10 text-indigo-700 dark:text-indigo-300",
  EVENT_COMPLETED: "bg-violet-500/10 text-violet-700 dark:text-violet-300",
  PICKUP_SCHEDULED: "bg-sky-500/10 text-sky-700 dark:text-sky-300",
  PICKED_UP: "bg-teal-500/10 text-teal-700 dark:text-teal-300",
  COMPLETED: "bg-muted text-muted-foreground",
  CANCELLED: "bg-destructive/10 text-destructive",
};

export const ORDER_STATUS_ICONS: Record<OrderStatus, LucideIcon> = {
  PENDING: CircleDashed,
  CONFIRMED: BadgeCheck,
  DELIVERY_SCHEDULED: CalendarClock,
  OUT_FOR_DELIVERY: Truck,
  DELIVERED: PackageCheck,
  EVENT_COMPLETED: Flag,
  PICKUP_SCHEDULED: Undo2,
  PICKED_UP: CheckCircle2,
  COMPLETED: CheckCircle2,
  CANCELLED: XCircle,
};

export const DELIVERY_STATUSES = [
  "SCHEDULED",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "FAILED",
  "CANCELLED",
] as const;
export type DeliveryStatus = (typeof DELIVERY_STATUSES)[number];

export const DELIVERY_STATUS_LABELS: Record<DeliveryStatus, string> = {
  SCHEDULED: "Scheduled",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
};

export const PICKUP_STATUSES = ["SCHEDULED", "PICKED_UP", "CANCELLED"] as const;
export type PickupStatus = (typeof PICKUP_STATUSES)[number];

export const PICKUP_STATUS_LABELS: Record<PickupStatus, string> = {
  SCHEDULED: "Scheduled",
  PICKED_UP: "Picked up",
  CANCELLED: "Cancelled",
};

export const ORDER_TIMELINE_LABELS: Record<string, string> = {
  ORDER_CREATED: "Booking confirmed",
  ORDER_CONFIRMED: "Order confirmed",
  DELIVERY_SCHEDULED: "Delivery scheduled",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  EVENT_COMPLETED: "Event completed",
  PICKUP_SCHEDULED: "Pickup scheduled",
  PICKED_UP: "Picked up",
  ORDER_COMPLETED: "Completed",
  ORDER_CANCELLED: "Order cancelled",
  PROOF_UPLOADED: "Proof uploaded",
  NOTE_ADDED: "Note added",
  INVENTORY_RELEASED: "Inventory released",
  STATUS_CHANGED: "Status changed",
};

export const PLANNER_ORDER_FILTERS: { id: "" | OrderStatus; label: string }[] = [
  { id: "", label: "All" },
  { id: "PENDING", label: "Pending" },
  { id: "CONFIRMED", label: "Confirmed" },
  { id: "DELIVERY_SCHEDULED", label: "Delivery scheduled" },
  { id: "OUT_FOR_DELIVERY", label: "Out for delivery" },
  { id: "DELIVERED", label: "Delivered" },
  { id: "COMPLETED", label: "Completed" },
  { id: "CANCELLED", label: "Cancelled" },
];

export function orderErrorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (error && typeof error === "object" && "status" in error) {
    const status = (error as { status?: number }).status;
    const message = "message" in error ? String((error as { message?: string }).message) : "";
    if (status === 404) return "Order not found";
    if (status === 401) return "Please sign in to continue";
    if (status === 403) return "You do not have access to this order";
    if (message) return message;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

export function formatOrderDate(value?: string | null): string {
  if (!value) return "—";
  const date = value.includes("T") ? new Date(value) : new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export function formatOrderDateTime(value?: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

export function formatTime(value?: string | null): string {
  if (!value) return "";
  return value.slice(0, 5);
}
