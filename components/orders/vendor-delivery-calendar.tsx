"use client";

import * as React from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { ROUTES } from "@/constants";
import { formatOrderDate, formatTime, orderErrorMessage } from "@/lib/orders/labels";
import { orderApi, type CalendarEntryDto, type VendorDeliveryCalendarDto } from "@/services";

function isoToday() {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function VendorDeliveryCalendar({ compact = false }: { compact?: boolean }) {
  const [date, setDate] = React.useState(isoToday);
  const [city, setCity] = React.useState("");
  const [data, setData] = React.useState<VendorDeliveryCalendarDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await orderApi.vendor.calendar({ date, city: city.trim() || undefined }));
    } catch (err) {
      setError(orderErrorMessage(err));
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [date, city]);

  React.useEffect(() => {
    void load();
  }, [load]);

  return (
    <Card className="rounded-2xl shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">Delivery calendar</CardTitle>
        <CardDescription>
          Today, upcoming deliveries, pending pickups, and completed drops.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2">
          <Input
            type="date"
            className="w-auto rounded-xl"
            value={date}
            onChange={(event) => setDate(event.target.value)}
          />
          <Input
            className="min-w-[8rem] flex-1 rounded-xl"
            placeholder="City"
            value={city}
            onChange={(event) => setCity(event.target.value)}
          />
          <Button variant="outline" className="rounded-xl" onClick={() => void load()}>
            Apply
          </Button>
        </div>
        {loading ? (
          <SkeletonLoader variant="text" count={3} />
        ) : error ? (
          <EmptyState
            title="Could not load calendar"
            description={error}
            actionLabel="Retry"
            onAction={() => void load()}
          />
        ) : data ? (
          <div className={compact ? "space-y-4" : "grid gap-4 md:grid-cols-2"}>
            <Lane
              title="Today's deliveries"
              rows={data.todayDeliveries}
              empty="No deliveries today."
            />
            <Lane
              title="Upcoming deliveries"
              rows={data.upcomingDeliveries}
              empty="No upcoming deliveries."
            />
            <Lane title="Pending pickups" rows={data.pendingPickups} empty="No pending pickups." />
            {!compact ? (
              <Lane
                title="Completed deliveries"
                rows={data.completedDeliveries}
                empty="No completed deliveries."
              />
            ) : null}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function Lane({ title, rows, empty }: { title: string; rows: CalendarEntryDto[]; empty: string }) {
  return (
    <div>
      <h3 className="mb-2 text-sm font-semibold">{title}</h3>
      {rows.length === 0 ? (
        <p className="text-muted-foreground text-sm">{empty}</p>
      ) : (
        <ul className="space-y-2">
          {rows.map((row) => (
            <li key={`${row.kind}-${row.orderId}-${row.date}`}>
              <Link
                href={ROUTES.vendor.order(row.orderId)}
                className="hover:bg-muted/30 block rounded-xl border px-3 py-2"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-medium">{row.orderNumber}</p>
                  <OrderStatusBadge status={row.orderStatus} />
                </div>
                <p className="text-muted-foreground mt-1 text-xs">
                  {row.kind === "PICKUP" ? "Pickup" : "Delivery"} {formatOrderDate(row.date)}
                  {row.preferredTime ? ` · ${formatTime(row.preferredTime)}` : ""} ·{" "}
                  {row.city || "—"}
                </p>
                <p className="text-muted-foreground text-xs">
                  {row.plannerName} · {row.projectName}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
