"use client";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { InventoryStockStatus } from "@/services";

const LABEL: Record<InventoryStockStatus, string> = {
  AVAILABLE: "Available",
  LOW_STOCK: "Low stock",
  UNAVAILABLE: "Unavailable",
  MAINTENANCE: "Maintenance",
};

export function InventoryStatusBadge({ status }: { status: InventoryStockStatus }) {
  return (
    <Badge
      variant="secondary"
      className={cn(
        "rounded-md",
        status === "AVAILABLE" && "bg-emerald-500/12 text-emerald-800 dark:text-emerald-300",
        status === "LOW_STOCK" && "bg-amber-500/12 text-amber-900 dark:text-amber-200",
        status === "MAINTENANCE" && "bg-sky-500/12 text-sky-900 dark:text-sky-300",
        status === "UNAVAILABLE" && "bg-muted text-muted-foreground"
      )}
    >
      {LABEL[status]}
    </Badge>
  );
}

export function isoDate(date: Date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

export function endOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

export function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}
