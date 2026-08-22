"use client";

import * as React from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { ROUTES } from "@/constants";
import {
  PLANNER_ORDER_FILTERS,
  formatOrderDate,
  orderErrorMessage,
  type OrderStatus,
} from "@/lib/orders/labels";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import { orderApi, type OrderSummaryDto } from "@/services";

export function VendorOrderList() {
  const [status, setStatus] = React.useState<"" | OrderStatus>("");
  const [rows, setRows] = React.useState<OrderSummaryDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await orderApi.vendor.list({ status, size: 50 });
      setRows(page.content);
    } catch (err) {
      setError(orderErrorMessage(err));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [status]);

  React.useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {PLANNER_ORDER_FILTERS.map((filter) => (
          <Button
            key={filter.id || "all"}
            size="sm"
            variant={status === filter.id ? "default" : "outline"}
            className="rounded-xl"
            onClick={() => setStatus(filter.id)}
          >
            {filter.label}
          </Button>
        ))}
      </div>
      {loading ? (
        <SkeletonLoader variant="text" count={5} />
      ) : error ? (
        <EmptyState
          title="Could not load orders"
          description={error}
          actionLabel="Retry"
          onAction={() => void load()}
        />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No orders yet"
          description="Accepted booking requests become orders here."
          actionLabel="Requests"
          onAction={() => {
            window.location.href = ROUTES.vendor.requests;
          }}
        />
      ) : (
        <div className="divide-y rounded-2xl border">
          {rows.map((order) => (
            <Link
              key={order.id}
              href={ROUTES.vendor.order(order.id)}
              className="hover:bg-muted/30 flex flex-col gap-2 px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="truncate text-sm font-medium">{order.orderNumber}</p>
                  <OrderStatusBadge status={order.status} />
                </div>
                <p className="text-muted-foreground mt-1 text-xs">
                  {order.plannerName} · {order.projectName} · Event{" "}
                  {formatOrderDate(order.eventDate)}
                </p>
                <p className="text-muted-foreground text-xs">
                  Delivery {formatOrderDate(order.deliveryDate)} · {order.itemCount} items ·{" "}
                  {order.itemsSummary}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">
                  {formatEstimatedRental(toMoney(order.totalAmount), order.currency ?? "INR")}
                </p>
                <p className="text-muted-foreground text-xs">Order value</p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
