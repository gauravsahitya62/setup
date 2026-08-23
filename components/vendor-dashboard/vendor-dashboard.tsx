"use client";

import * as React from "react";
import Link from "next/link";
import {
  ArrowDownRight,
  ArrowUpRight,
  CalendarDays,
  ClipboardList,
  Package,
  Plus,
  Settings,
  Warehouse,
} from "lucide-react";

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
import { Rating } from "@/components/shared/rating";
import { VendorAnalyticsCharts } from "@/components/vendor-dashboard/vendor-analytics-charts";
import { VendorAvailabilityCalendar } from "@/components/vendor-dashboard/vendor-availability-calendar";
import { VendorDeliveryCalendar } from "@/components/orders/vendor-delivery-calendar";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { ROUTES } from "@/constants";
import { useMyVendor } from "@/hooks/use-my-vendor";
import { useAuth } from "@/providers";
import {
  bookingApi,
  mapBookingSummary,
  marketplaceApi,
  orderApi,
  reviewApi,
  type OrderSummaryDto,
  type PublicReviewDto,
  type VendorInventoryOverviewDto,
} from "@/services";
import type { BookingRequest } from "@/types";
import { cn, formatNumber } from "@/lib/utils";
import { BOOKING_STATUS_LABELS } from "@/lib/booking/labels";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import { formatOrderDate } from "@/lib/orders/labels";

function KpiCard({
  label,
  value,
  hint,
  change,
  href,
}: {
  label: string;
  value: string;
  hint?: string;
  change?: number;
  href?: string;
}) {
  const content = (
    <Card className="rounded-2xl shadow-sm transition-shadow hover:shadow-md">
      <CardHeader className="pb-2">
        <CardDescription className="text-xs font-medium tracking-wide uppercase">
          {label}
        </CardDescription>
        <CardTitle className="text-2xl font-semibold tracking-tight tabular-nums sm:text-3xl">
          {value}
        </CardTitle>
      </CardHeader>
      <CardContent className="flex items-center gap-2 pt-0">
        {typeof change === "number" ? (
          <span
            className={cn(
              "inline-flex items-center gap-0.5 text-xs font-medium",
              change >= 0
                ? "text-emerald-600 dark:text-emerald-400"
                : "text-rose-600 dark:text-rose-400"
            )}
          >
            {change >= 0 ? (
              <ArrowUpRight className="size-3.5" />
            ) : (
              <ArrowDownRight className="size-3.5" />
            )}
            {Math.abs(change)}% vs last month
          </span>
        ) : hint ? (
          <span className="text-muted-foreground text-xs">{hint}</span>
        ) : null}
      </CardContent>
    </Card>
  );

  if (href) {
    return (
      <Link href={href} className="block focus-visible:outline-none">
        {content}
      </Link>
    );
  }
  return content;
}

function QuickActions() {
  const actions = [
    { href: ROUTES.vendor.catalog, label: "Add product", icon: Plus },
    { href: ROUTES.vendor.requests, label: "Review requests", icon: ClipboardList },
    { href: ROUTES.vendor.availability, label: "Update calendar", icon: CalendarDays },
    { href: ROUTES.vendor.settings, label: "Store settings", icon: Settings },
  ] as const;

  return (
    <Card className="rounded-2xl shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">Quick actions</CardTitle>
        <CardDescription>Common tasks for running your rental catalog.</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-2 sm:grid-cols-2">
        {actions.map((action) => {
          const Icon = action.icon;
          return (
            <Button
              key={action.href}
              variant="outline"
              className="h-auto justify-start gap-3 rounded-xl px-4 py-3"
              asChild
            >
              <Link href={action.href}>
                <span className="bg-muted flex size-8 items-center justify-center rounded-lg">
                  <Icon className="size-4" />
                </span>
                <span className="text-sm font-medium">{action.label}</span>
              </Link>
            </Button>
          );
        })}
      </CardContent>
    </Card>
  );
}

export function VendorDashboard() {
  const { user } = useAuth();
  const { vendor } = useMyVendor();
  const companyName = vendor?.companyName ?? "Your company";
  const firstName = user?.firstName ?? "there";
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  const publicHref = vendor?.slug
    ? `${ROUTES.vendors}/${vendor.slug}`
    : ROUTES.vendor.profileWizard;
  const [pending, setPending] = React.useState<BookingRequest[]>([]);
  const [pendingCount, setPendingCount] = React.useState(0);
  const [orders, setOrders] = React.useState<OrderSummaryDto[]>([]);
  const [reviews, setReviews] = React.useState<PublicReviewDto[]>([]);
  const [inventory, setInventory] = React.useState<VendorInventoryOverviewDto | null>(null);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [stats, page, orderPage, overview] = await Promise.all([
          bookingApi.vendor.stats(),
          bookingApi.vendor.list({ status: "PENDING", size: 5 }),
          orderApi.vendor.list({ size: 50 }),
          marketplaceApi.vendorInventory.overview(),
        ]);
        let latestReviews: PublicReviewDto[] = [];
        if (vendor?.id) {
          const reviewPage = await reviewApi.public.list(vendor.id, { size: 3, sort: "NEWEST" });
          latestReviews = reviewPage.content;
        }
        if (!cancelled) {
          setPendingCount(stats.pending);
          setPending(page.content.map(mapBookingSummary));
          setOrders(orderPage.content);
          setReviews(latestReviews);
          setInventory(overview);
        }
      } catch {
        if (!cancelled) {
          setPending([]);
          setPendingCount(0);
          setOrders([]);
          setReviews([]);
          setInventory(null);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [vendor?.id]);

  const activeOrders = orders.filter((order) => order.status !== "CANCELLED");
  const estimatedRentalValue = activeOrders.reduce(
    (sum, order) => sum + toMoney(order.totalAmount),
    0
  );
  const utilization =
    inventory && inventory.totalUnits > 0
      ? Math.round((inventory.reservedUnits / inventory.totalUnits) * 100)
      : 0;
  const popular = inventory?.items.slice(0, 4) ?? [];

  return (
    <div className="space-y-8 lg:space-y-10">
      <section className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1.5">
          <p className="text-muted-foreground text-sm">{companyName} · Owner</p>
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">
            {greeting}, {firstName}
          </h1>
          <p className="text-muted-foreground max-w-xl text-sm">
            You have {pendingCount} pending request{pendingCount === 1 ? "" : "s"}
            {inventory
              ? ` and ${inventory.lowAvailabilityCount} SKU${inventory.lowAvailabilityCount === 1 ? "" : "s"} running low.`
              : "."}{" "}
            Values below are estimated rental value, not collected revenue.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" className="rounded-xl" asChild>
            <Link href={publicHref}>
              {vendor?.status === "PUBLISHED" ? "View public profile" : "Complete profile"}
            </Link>
          </Button>
          <Button className="rounded-xl" asChild>
            <Link href={ROUTES.vendor.catalog}>
              <Warehouse className="size-4" />
              Manage inventory
            </Link>
          </Button>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          label="Estimated rental value"
          value={formatEstimatedRental(estimatedRentalValue, activeOrders[0]?.currency ?? "INR")}
          hint="From confirmed orders. Not collected revenue."
          href={ROUTES.vendor.analytics}
        />
        <KpiCard
          label="Pending requests"
          value={String(pendingCount)}
          hint="Awaiting your response"
          href={ROUTES.vendor.requests}
        />
        <KpiCard
          label="Active SKUs"
          value={String(inventory?.totalVariants ?? 0)}
          hint={`${inventory?.totalProducts ?? 0} products · ${inventory?.lowAvailabilityCount ?? 0} low stock`}
          href={ROUTES.vendor.catalog}
        />
        <KpiCard
          label="Units reserved"
          value={`${utilization}%`}
          hint={`${formatNumber(inventory?.reservedUnits ?? 0)} of ${formatNumber(inventory?.totalUnits ?? 0)} units reserved`}
        />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.45fr_0.9fr]">
        <VendorAnalyticsCharts />
        <div className="space-y-6">
          <QuickActions />
          <VendorAvailabilityCalendar compact />
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <Card className="rounded-2xl shadow-sm">
          <CardHeader>
            <CardTitle className="text-base">Pending requests</CardTitle>
            <CardDescription>Quotes and booking asks from planners.</CardDescription>
            <CardAction>
              <Button variant="ghost" size="sm" className="rounded-lg" asChild>
                <Link href={ROUTES.vendor.requests}>View all</Link>
              </Button>
            </CardAction>
          </CardHeader>
          <CardContent className="space-y-3">
            {pending.length === 0 ? (
              <p className="text-muted-foreground text-sm">No pending booking requests.</p>
            ) : (
              pending.map((request) => (
                <div
                  key={request.id}
                  className="border-border/70 bg-muted/20 flex flex-col gap-3 rounded-xl border p-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate text-sm font-medium">{request.projectName}</p>
                      <Badge variant="secondary" className="rounded-md">
                        {BOOKING_STATUS_LABELS[request.status]}
                      </Badge>
                    </div>
                    <p className="text-muted-foreground text-xs">{request.plannerName}</p>
                    <p className="text-muted-foreground text-xs">{request.itemsSummary}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3 sm:flex-col sm:items-end">
                    <p className="text-sm font-semibold tabular-nums">
                      {formatEstimatedRental(request.amount ?? 0, request.currency ?? "INR")}
                    </p>
                    <Button size="sm" className="rounded-lg" asChild>
                      <Link href={ROUTES.vendor.request(request.id)}>Respond</Link>
                    </Button>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="rounded-2xl shadow-sm">
          <CardHeader>
            <CardTitle className="text-base">Orders</CardTitle>
            <CardDescription>Confirmed and in-flight rental orders.</CardDescription>
            <CardAction>
              <Button variant="ghost" size="sm" className="rounded-lg" asChild>
                <Link href={ROUTES.vendor.bookings}>View all</Link>
              </Button>
            </CardAction>
          </CardHeader>
          <CardContent className="space-y-3">
            {orders.length === 0 ? (
              <p className="text-muted-foreground text-sm">
                No orders yet. Accepted requests become orders.
              </p>
            ) : (
              orders.map((order) => (
                <Link
                  key={order.id}
                  href={ROUTES.vendor.order(order.id)}
                  className="border-border/70 hover:bg-muted/30 flex items-start justify-between gap-4 rounded-xl border px-4 py-3.5"
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate text-sm font-medium">{order.orderNumber}</p>
                      <OrderStatusBadge status={order.status} />
                    </div>
                    <p className="text-muted-foreground text-xs">
                      {order.plannerName} · {order.projectName} · {formatOrderDate(order.eventDate)}
                    </p>
                    <p className="text-muted-foreground inline-flex items-center gap-1.5 text-xs">
                      <Package className="size-3.5" />
                      {order.itemCount} line items
                    </p>
                  </div>
                  <p className="shrink-0 text-sm font-semibold tabular-nums">
                    {formatEstimatedRental(toMoney(order.totalAmount), order.currency ?? "INR")}
                  </p>
                </Link>
              ))
            )}
          </CardContent>
        </Card>
      </section>

      <VendorDeliveryCalendar compact />

      <section className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="rounded-2xl shadow-sm">
          <CardHeader>
            <CardTitle className="text-base">Popular products</CardTitle>
            <CardDescription>Top performers by review volume.</CardDescription>
            <CardAction>
              <Button variant="ghost" size="sm" className="rounded-lg" asChild>
                <Link href={ROUTES.vendor.catalog}>Inventory</Link>
              </Button>
            </CardAction>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2">
            {popular.length === 0 ? (
              <p className="text-muted-foreground text-sm sm:col-span-2">
                No inventory listed yet. Add products to see stock here.
              </p>
            ) : (
              popular.map((item) => (
                <div
                  key={item.id}
                  className="border-border/70 hover:bg-muted/30 flex gap-3 rounded-xl border p-3 transition-colors"
                >
                  <div className="bg-muted size-16 shrink-0 overflow-hidden rounded-lg">
                    {item.featuredImage ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={item.featuredImage} alt="" className="size-full object-cover" />
                    ) : null}
                  </div>
                  <div className="min-w-0 space-y-1">
                    <p className="truncate text-sm font-medium">{item.productName}</p>
                    <p className="text-muted-foreground text-xs">{item.sku}</p>
                    <p className="text-muted-foreground text-xs tabular-nums">
                      {item.availableQuantity} available · {item.reservedQuantity} reserved
                    </p>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="rounded-2xl shadow-sm">
          <CardHeader>
            <CardTitle className="text-base">Recent reviews</CardTitle>
            <CardDescription>What planners are saying about your catalog.</CardDescription>
            <CardAction>
              <Button variant="ghost" size="sm" className="rounded-lg" asChild>
                <Link href={ROUTES.vendor.reviews}>All reviews</Link>
              </Button>
            </CardAction>
          </CardHeader>
          <CardContent className="space-y-4">
            {reviews.length === 0 ? (
              <p className="text-muted-foreground text-sm">
                No reviews yet. Completed bookings will appear here.
              </p>
            ) : (
              reviews.map((review) => (
                <div
                  key={review.id}
                  className="border-border/60 space-y-2 border-b pb-4 last:border-0 last:pb-0"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-medium">{review.plannerDisplayName}</p>
                      {review.verified ? (
                        <p className="text-muted-foreground text-xs">Verified Booking</p>
                      ) : null}
                    </div>
                    <Rating value={review.overallRating} />
                  </div>
                  {review.reviewText ? (
                    <p className="text-muted-foreground line-clamp-2 text-sm">
                      {review.reviewText}
                    </p>
                  ) : null}
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
