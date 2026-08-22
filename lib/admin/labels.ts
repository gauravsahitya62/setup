export const ADMIN_DATE_RANGES = [
  { id: "TODAY", label: "Today" },
  { id: "DAYS_7", label: "7 days" },
  { id: "DAYS_30", label: "30 days" },
  { id: "DAYS_90", label: "90 days" },
  { id: "CUSTOM", label: "Custom" },
] as const;

export const USER_STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Active",
  PENDING_VERIFICATION: "Pending",
  SUSPENDED: "Suspended",
};

export const VERIFICATION_LABEL: Record<string, string> = {
  PENDING: "Pending",
  UNDER_REVIEW: "Under review",
  VERIFIED: "Verified",
  REJECTED: "Rejected",
  SUSPENDED: "Suspended",
};

export const PRODUCT_STATUS_LABEL: Record<string, string> = {
  DRAFT: "Draft",
  PUBLISHED: "Published",
  PENDING_REVIEW: "Pending review",
  REJECTED: "Rejected",
  SUSPENDED: "Suspended",
  ARCHIVED: "Archived",
};

export const TICKET_STATUS_LABEL: Record<string, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  WAITING_FOR_USER: "Waiting for user",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

export const TICKET_PRIORITY_LABEL: Record<string, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  URGENT: "Urgent",
};

export const CATEGORY_STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Active",
  HIDDEN: "Hidden",
  ARCHIVED: "Archived",
};

export function formatEstimatedValue(value: number | string | null | undefined) {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(amount) ? amount : 0);
}

export function adminErrorMessage(error: unknown, fallback = "Something went wrong") {
  if (
    error &&
    typeof error === "object" &&
    "message" in error &&
    typeof error.message === "string"
  ) {
    return error.message;
  }
  return fallback;
}

export function requiresReason(action: string) {
  return ["reject", "suspend", "remove", "archive"].includes(action);
}
