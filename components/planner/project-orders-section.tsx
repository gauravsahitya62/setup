"use client";

import Link from "next/link";

import { EmptyState } from "@/components/shared/empty-state";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { ROUTES } from "@/constants";
import { formatOrderDate } from "@/lib/orders/labels";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import type { OrderSummaryDto } from "@/services";

interface ProjectOrdersSectionProps {
  projectId: string;
  orders: OrderSummaryDto[];
}

export function ProjectOrdersSection({ projectId, orders }: ProjectOrdersSectionProps) {
  const upcoming = orders.filter(
    (order) =>
      order.status === "DELIVERY_SCHEDULED" ||
      order.status === "OUT_FOR_DELIVERY" ||
      order.status === "CONFIRMED"
  );

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Orders</h2>
        <p className="text-muted-foreground mt-1 text-sm">
          Confirmed rentals for this project. Open an order for delivery and pickup status.
        </p>
      </div>
      {upcoming.length > 0 ? (
        <p className="text-muted-foreground text-sm">
          {upcoming.length} upcoming {upcoming.length === 1 ? "delivery" : "deliveries"}
        </p>
      ) : null}
      {orders.length === 0 ? (
        <EmptyState
          title="No orders yet"
          description="Orders appear after a vendor accepts a booking request for this project."
          actionLabel="Booking requests"
          onAction={() => {
            window.location.href = ROUTES.planner.projectBookingNew(projectId);
          }}
        />
      ) : (
        <div className="divide-border/70 border-border/70 divide-y rounded-2xl border">
          {orders.map((order) => (
            <Link
              key={order.id}
              href={ROUTES.planner.order(order.id)}
              className="hover:bg-muted/30 flex flex-col gap-2 px-4 py-3.5 transition-colors sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="truncate text-sm font-medium">{order.orderNumber}</p>
                  <OrderStatusBadge status={order.status} />
                </div>
                <p className="text-muted-foreground mt-1 text-xs">
                  {order.vendorName} · {order.itemsSummary}
                </p>
                <p className="text-muted-foreground text-xs">
                  Delivery {formatOrderDate(order.deliveryDate)} · Event{" "}
                  {formatOrderDate(order.eventDate)}
                </p>
              </div>
              <p className="shrink-0 text-sm font-semibold tabular-nums">
                {formatEstimatedRental(toMoney(order.totalAmount), order.currency ?? "INR")}
              </p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
