import type { EventType, ProjectStatus } from "@/types";

export const EVENT_TYPES: EventType[] = [
  "WEDDING",
  "RECEPTION",
  "ENGAGEMENT",
  "HALDI",
  "MEHENDI",
  "CORPORATE",
  "CONFERENCE",
  "EXHIBITION",
  "CONCERT",
  "FILM_SHOOT",
  "PRIVATE_EVENT",
  "OTHER",
];

export const EVENT_TYPE_LABELS: Record<EventType, string> = {
  WEDDING: "Wedding",
  RECEPTION: "Reception",
  ENGAGEMENT: "Engagement",
  HALDI: "Haldi",
  MEHENDI: "Mehendi",
  CORPORATE: "Corporate",
  CONFERENCE: "Conference",
  EXHIBITION: "Exhibition",
  CONCERT: "Concert",
  FILM_SHOOT: "Film shoot",
  PRIVATE_EVENT: "Private event",
  OTHER: "Other",
};

export const PROJECT_STATUSES: ProjectStatus[] = [
  "DRAFT",
  "PLANNING",
  "ACTIVE",
  "COMPLETED",
  "CANCELLED",
];

export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  DRAFT: "Draft",
  PLANNING: "Planning",
  ACTIVE: "Active",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

export const PROJECT_STATUS_STYLES: Record<ProjectStatus, string> = {
  DRAFT: "bg-muted text-muted-foreground",
  PLANNING: "bg-sky-500/10 text-sky-700 dark:text-sky-300",
  ACTIVE: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
  COMPLETED: "bg-muted text-muted-foreground",
  CANCELLED: "bg-destructive/10 text-destructive",
};

export function formatEventDate(value?: string | null): string | undefined {
  if (!value) return undefined;
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export function formatProjectLocation(input: {
  venueName?: string | null;
  city?: string | null;
  state?: string | null;
}): string | undefined {
  const parts = [input.venueName, input.city, input.state].filter(Boolean);
  return parts.length ? parts.join(", ") : undefined;
}

export function daysUntil(eventDate?: string | null): number | null {
  if (!eventDate) return null;
  const date = new Date(`${eventDate}T00:00:00`);
  if (Number.isNaN(date.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((date.getTime() - today.getTime()) / 86_400_000);
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function fileKind(fileType: string): "pdf" | "image" | "spreadsheet" | "document" | "other" {
  if (fileType.startsWith("image/")) return "image";
  if (fileType.includes("pdf")) return "pdf";
  if (fileType.includes("sheet") || fileType.includes("excel") || fileType.includes("csv")) {
    return "spreadsheet";
  }
  if (fileType.includes("word") || fileType.includes("msword") || fileType.includes("text")) {
    return "document";
  }
  return "other";
}
