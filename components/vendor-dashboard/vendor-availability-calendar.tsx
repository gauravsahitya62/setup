"use client";

import * as React from "react";
import Link from "next/link";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ROUTES } from "@/constants";
import { cn } from "@/lib/utils";
import {
  ApiClientError,
  marketplaceApi,
  type CalendarDayDto,
  type VendorInventoryItemDto,
} from "@/services";

const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function iso(date: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function availabilityLabel(day: CalendarDayDto | undefined) {
  if (!day) return "No data";
  if (day.totalQuantity <= 0 || day.availableQuantity <= 0) return "Unavailable";
  if (day.availableQuantity < day.totalQuantity) {
    return `${day.availableQuantity} of ${day.totalQuantity} available`;
  }
  return `${day.availableQuantity} available`;
}

export function VendorAvailabilityCalendar({
  compact = false,
  initialVariant = "all",
}: {
  compact?: boolean;
  initialVariant?: string;
}) {
  const [view, setView] = React.useState<"month" | "week">(compact ? "month" : "month");
  const [cursor, setCursor] = React.useState(() => new Date());
  const [variantId, setVariantId] = React.useState(initialVariant);
  const [productId, setProductId] = React.useState("all");
  const [items, setItems] = React.useState<VendorInventoryItemDto[]>([]);
  const [days, setDays] = React.useState<CalendarDayDto[]>([]);
  const [selected, setSelected] = React.useState<CalendarDayDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [customStart, setCustomStart] = React.useState("");
  const [customEnd, setCustomEnd] = React.useState("");

  React.useEffect(() => {
    marketplaceApi.vendorInventory
      .overview()
      .then((data) => setItems(data.items))
      .catch(() => setItems([]));
  }, []);

  const range = React.useMemo(() => {
    if (customStart && customEnd) {
      return { start: customStart, end: customEnd };
    }
    if (view === "week") {
      const start = new Date(cursor);
      start.setDate(cursor.getDate() - cursor.getDay());
      const end = new Date(start);
      end.setDate(start.getDate() + 6);
      return { start: iso(start), end: iso(end) };
    }
    const start = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
    const end = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0);
    return { start: iso(start), end: iso(end) };
  }, [cursor, view, customStart, customEnd]);

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);
    marketplaceApi.vendorInventory
      .calendar({
        variant: variantId === "all" ? undefined : variantId,
        product: productId === "all" ? undefined : productId,
        startDate: range.start,
        endDate: range.end,
      })
      .then((data) => {
        if (!cancelled) {
          setDays(data);
          setSelected(null);
        }
      })
      .catch((error) => {
        toast.error(error instanceof ApiClientError ? error.message : "Could not load calendar");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [variantId, productId, range.start, range.end]);

  const byDate = React.useMemo(() => new Map(days.map((d) => [d.date, d])), [days]);
  const products = React.useMemo(() => {
    const map = new Map<string, string>();
    items.forEach((item) => map.set(item.productId, item.productName));
    return Array.from(map.entries());
  }, [items]);
  const variants = React.useMemo(
    () => items.filter((item) => productId === "all" || item.productId === productId),
    [items, productId]
  );

  const startDate = new Date(`${range.start}T12:00:00`);
  const blanks = view === "month" ? Array.from({ length: startDate.getDay() }) : [];
  const dayCount =
    Math.round((new Date(`${range.end}T12:00:00`).getTime() - startDate.getTime()) / 86400000) + 1;

  return (
    <Card className="rounded-2xl shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">Availability</CardTitle>
        <CardDescription>
          Inclusive rental dates. Numbers are available units, not just color.
        </CardDescription>
        {!compact ? (
          <CardAction>
            <Button variant="ghost" size="sm" className="rounded-lg" asChild>
              <Link href={ROUTES.vendor.inventory}>Inventory</Link>
            </Button>
          </CardAction>
        ) : (
          <CardAction>
            <Button variant="ghost" size="sm" className="rounded-lg" asChild>
              <Link href={ROUTES.vendor.availability}>Manage</Link>
            </Button>
          </CardAction>
        )}
      </CardHeader>
      <CardContent className="space-y-4">
        {!compact ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-1.5">
              <Label>View</Label>
              <Select value={view} onValueChange={(value) => setView(value as "month" | "week")}>
                <SelectTrigger className="rounded-xl">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="month">Month</SelectItem>
                  <SelectItem value="week">Week</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Product</Label>
              <Select
                value={productId}
                onValueChange={(value) => {
                  setProductId(value);
                  setVariantId("all");
                }}
              >
                <SelectTrigger className="rounded-xl">
                  <SelectValue placeholder="All products" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All products</SelectItem>
                  {products.map(([id, name]) => (
                    <SelectItem key={id} value={id}>
                      {name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Variant</Label>
              <Select value={variantId} onValueChange={setVariantId}>
                <SelectTrigger className="rounded-xl">
                  <SelectValue placeholder="All variants" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All variants</SelectItem>
                  {variants.map((item) => (
                    <SelectItem key={item.variantId} value={item.variantId}>
                      {item.variantName} · {item.sku}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div className="space-y-1.5">
                <Label>From</Label>
                <Input
                  type="date"
                  value={customStart}
                  onChange={(e) => setCustomStart(e.target.value)}
                  className="rounded-xl"
                />
              </div>
              <div className="space-y-1.5">
                <Label>To</Label>
                <Input
                  type="date"
                  value={customEnd}
                  onChange={(e) => setCustomEnd(e.target.value)}
                  className="rounded-xl"
                />
              </div>
            </div>
          </div>
        ) : null}

        <div className="flex items-center justify-between">
          <p className="text-sm font-medium">
            {startDate.toLocaleDateString(undefined, {
              month: "long",
              year: "numeric",
              day: "numeric",
            })}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              className="rounded-lg"
              onClick={() => {
                setCustomStart("");
                setCustomEnd("");
                setCursor(
                  view === "week"
                    ? new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() - 7)
                    : new Date(cursor.getFullYear(), cursor.getMonth() - 1, 1)
                );
              }}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="rounded-lg"
              onClick={() => {
                setCustomStart("");
                setCustomEnd("");
                setCursor(
                  view === "week"
                    ? new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() + 7)
                    : new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1)
                );
              }}
            >
              Next
            </Button>
          </div>
        </div>

        <div className="text-muted-foreground grid grid-cols-7 gap-1 text-center text-[11px] font-medium">
          {WEEKDAYS.map((day) => (
            <span key={day}>{day}</span>
          ))}
        </div>
        <div className={cn("grid grid-cols-7 gap-1", loading && "opacity-60")}>
          {blanks.map((_, i) => (
            <div key={`blank-${i}`} className="aspect-square" />
          ))}
          {Array.from({ length: dayCount }, (_, i) => {
            const date = new Date(startDate);
            date.setDate(startDate.getDate() + i);
            const key = iso(date);
            const day = byDate.get(key);
            const available = day?.availableQuantity ?? 0;
            const total = day?.totalQuantity ?? 0;
            const state =
              !day || total === 0
                ? "empty"
                : available <= 0
                  ? "unavailable"
                  : available < total
                    ? "limited"
                    : "open";
            return (
              <button
                key={key}
                type="button"
                title={availabilityLabel(day)}
                onClick={() => setSelected(day ?? null)}
                className={cn(
                  "flex aspect-square flex-col items-center justify-center rounded-lg text-xs ring-1 transition-transform ring-inset hover:scale-[1.04]",
                  state === "open" &&
                    "bg-emerald-500/15 text-emerald-800 ring-emerald-500/20 dark:text-emerald-300",
                  state === "limited" &&
                    "bg-amber-500/15 text-amber-900 ring-amber-500/25 dark:text-amber-200",
                  state === "unavailable" && "bg-muted text-muted-foreground ring-border",
                  state === "empty" && "bg-background text-muted-foreground ring-border/60"
                )}
              >
                <span className="font-medium tabular-nums">{date.getDate()}</span>
                <span className="text-[10px] tabular-nums">{available}</span>
              </button>
            );
          })}
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge variant="outline" className="rounded-md bg-emerald-500/15 text-emerald-800">
            Open — units free
          </Badge>
          <Badge variant="outline" className="rounded-md bg-amber-500/15 text-amber-900">
            Limited — some reserved
          </Badge>
          <Badge variant="outline" className="bg-muted text-muted-foreground rounded-md">
            Unavailable — none left
          </Badge>
        </div>
        {selected ? (
          <div className="border-border/70 bg-muted/20 rounded-xl border p-3 text-sm">
            <p className="font-medium">{selected.date}</p>
            <p className="text-muted-foreground mt-1">
              Total {selected.totalQuantity} · Reserved {selected.reservedQuantity} · Maintenance{" "}
              {selected.maintenanceQuantity} · Damaged {selected.damagedQuantity} · Blocked{" "}
              {selected.blockedQuantity} ·{" "}
              <span className="text-foreground">Available {selected.availableQuantity}</span>
            </p>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
