export const BOOKING_STATUSES = [
  "DRAFT",
  "PENDING",
  "ACCEPTED",
  "REJECTED",
  "CANCELLED",
  "CONFIRMED",
  "COMPLETED",
] as const;

export type BookingStatus = (typeof BOOKING_STATUSES)[number];

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  DRAFT: "Draft",
  PENDING: "Pending",
  ACCEPTED: "Accepted",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
  CONFIRMED: "Confirmed",
  COMPLETED: "Completed",
};

export const BOOKING_STATUS_STYLES: Record<BookingStatus, string> = {
  DRAFT: "bg-muted text-muted-foreground",
  PENDING: "bg-amber-500/10 text-amber-700 dark:text-amber-300",
  ACCEPTED: "bg-sky-500/10 text-sky-700 dark:text-sky-300",
  REJECTED: "bg-destructive/10 text-destructive",
  CANCELLED: "bg-muted text-muted-foreground",
  CONFIRMED: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
  COMPLETED: "bg-muted text-muted-foreground",
};

export const REJECTION_REASONS = [
  "INVENTORY_UNAVAILABLE",
  "DATES_UNAVAILABLE",
  "QUANTITY_UNAVAILABLE",
  "SERVICE_AREA",
  "OTHER",
] as const;

export type BookingRejectionReason = (typeof REJECTION_REASONS)[number];

export const REJECTION_REASON_LABELS: Record<BookingRejectionReason, string> = {
  INVENTORY_UNAVAILABLE: "Inventory unavailable",
  DATES_UNAVAILABLE: "Dates unavailable",
  QUANTITY_UNAVAILABLE: "Quantity unavailable",
  SERVICE_AREA: "Outside service area",
  OTHER: "Other",
};

export const TIMELINE_LABELS: Record<string, string> = {
  REQUEST_CREATED: "Request created",
  REQUEST_SENT: "Request sent",
  VENDOR_VIEWED: "Vendor viewed",
  VENDOR_ACCEPTED: "Vendor accepted",
  INVENTORY_RESERVED: "Inventory reserved",
  CONFIRMED: "Confirmed",
  VENDOR_REJECTED: "Vendor rejected",
  PLANNER_CANCELLED: "Cancelled",
  INVENTORY_RELEASED: "Inventory released",
  INVENTORY_UNAVAILABLE: "Inventory no longer available",
  STATUS_CHANGED: "Status changed",
  COMPLETED: "Completed",
};

export function bookingErrorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (error && typeof error === "object" && "status" in error) {
    const status = (error as { status?: number }).status;
    const message = "message" in error ? String((error as { message?: string }).message) : "";
    if (status === 404) return "Booking request not found";
    if (status === 401) return "Please sign in to continue";
    if (status === 403) return "You do not have access to this booking request";
    if (message) return message;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

export function formatBookingDate(value?: string | null): string {
  if (!value) return "—";
  const date = value.includes("T") ? new Date(value) : new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}
