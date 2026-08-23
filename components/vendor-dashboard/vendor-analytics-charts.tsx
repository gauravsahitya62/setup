"use client";

import * as React from "react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import { formatNumber } from "@/lib/utils";
import { orderApi, type OrderSummaryDto } from "@/services";

function isActiveOrder(order: OrderSummaryDto): boolean {
  return order.status !== "CANCELLED";
}

export function VendorAnalyticsCharts() {
  const [orders, setOrders] = React.useState<OrderSummaryDto[]>([]);
  const [loaded, setLoaded] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const page = await orderApi.vendor.list({ size: 50 });
        if (!cancelled) setOrders(page.content);
      } catch {
        if (!cancelled) setOrders([]);
      } finally {
        if (!cancelled) setLoaded(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const active = orders.filter(isActiveOrder);
  const completed = orders.filter((order) => order.status === "COMPLETED");
  const gbv = active.reduce((sum, order) => sum + toMoney(order.totalAmount), 0);
  const completedTotal = completed.reduce((sum, order) => sum + toMoney(order.totalAmount), 0);
  const currency = active[0]?.currency ?? completed[0]?.currency ?? "INR";

  return (
    <Card className="rounded-2xl shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">Estimated rental value</CardTitle>
        <CardDescription>
          Gross Booking Value from confirmed orders. This is not collected revenue.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {!loaded ? (
          <p className="text-muted-foreground text-sm">Loading order totals…</p>
        ) : active.length === 0 ? (
          <p className="text-muted-foreground text-sm">
            No confirmed orders yet. Estimated rental value appears after you accept a booking
            request.
          </p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <p className="text-muted-foreground text-xs">Gross Booking Value</p>
              <p className="text-lg font-semibold tabular-nums">
                {formatEstimatedRental(gbv, currency)}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs">Confirmed orders</p>
              <p className="text-lg font-semibold tabular-nums">{formatNumber(active.length)}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs">Completed / total</p>
              <p className="text-lg font-semibold tabular-nums">
                {formatNumber(completed.length)} · {formatEstimatedRental(completedTotal, currency)}
              </p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
