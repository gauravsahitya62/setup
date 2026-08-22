"use client";

import * as React from "react";
import Link from "next/link";
import { BadgeCheck, LifeBuoy, PackageSearch, ScrollText, Users } from "lucide-react";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ADMIN_DATE_RANGES, adminErrorMessage } from "@/lib/admin/labels";
import { ROUTES } from "@/constants";
import { useAuth } from "@/providers";
import { ApiClientError, adminApi, type AdminDashboardDto, type AdminDateRange } from "@/services";

const KPI = [
  { key: "totalPlanners", label: "Total Planners" },
  { key: "totalVendors", label: "Total Vendors" },
  { key: "verifiedVendors", label: "Verified Vendors" },
  { key: "pendingVendors", label: "Pending Vendors" },
  { key: "totalProducts", label: "Total Products" },
  { key: "activeProducts", label: "Active Products" },
  { key: "pendingModeration", label: "Pending Moderation" },
  { key: "activeProjects", label: "Active Projects" },
  { key: "bookingRequests", label: "Booking Requests" },
  { key: "confirmedOrders", label: "Confirmed Orders" },
  { key: "completedOrders", label: "Completed Orders" },
  { key: "reportedReviews", label: "Reported Reviews" },
  { key: "openSupportTickets", label: "Open Support Tickets" },
] as const;

export function AdminDashboard() {
  const { user } = useAuth();
  const [range, setRange] = React.useState<AdminDateRange>("DAYS_30");
  const [from, setFrom] = React.useState("");
  const [to, setTo] = React.useState("");
  const [data, setData] = React.useState<AdminDashboardDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(
        await adminApi.dashboard(
          range,
          range === "CUSTOM" && from ? new Date(`${from}T00:00:00.000Z`).toISOString() : undefined,
          range === "CUSTOM" && to ? new Date(`${to}T23:59:59.999Z`).toISOString() : undefined
        )
      );
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [range, from, to]);

  React.useEffect(() => {
    if (range === "CUSTOM" && (!from || !to)) return;
    void load();
  }, [load, range, from, to]);

  return (
    <div className="space-y-8">
      <AdminPageHeader
        title={`Welcome back, ${user?.firstName ?? "Admin"}`}
        description="Marketplace operations, verification queues, and support activity."
        actions={
          <div className="flex flex-wrap gap-2">
            {ADMIN_DATE_RANGES.map((item) => (
              <Button
                key={item.id}
                size="sm"
                variant={range === item.id ? "default" : "outline"}
                className="rounded-xl"
                onClick={() => setRange(item.id)}
              >
                {item.label}
              </Button>
            ))}
            {range === "CUSTOM" ? (
              <>
                <input
                  type="date"
                  value={from}
                  className="bg-background rounded-xl border px-3 py-1.5 text-sm"
                  onChange={(event) => setFrom(event.target.value)}
                />
                <input
                  type="date"
                  value={to}
                  className="bg-background rounded-xl border px-3 py-1.5 text-sm"
                  onChange={(event) => setTo(event.target.value)}
                />
              </>
            ) : null}
          </div>
        }
      />

      {loading ? <SkeletonLoader className="h-40 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load dashboard" description={error} /> : null}

      {data ? (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {KPI.map((item) => (
              <Card key={item.key} className="rounded-2xl">
                <CardHeader className="pb-2">
                  <CardDescription>{item.label}</CardDescription>
                  <CardTitle className="text-2xl">{data[item.key]}</CardTitle>
                </CardHeader>
              </Card>
            ))}
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card className="rounded-2xl">
              <CardHeader>
                <CardTitle>Trends</CardTitle>
                <CardDescription>
                  New users, vendors, products, and bookings in this window.
                </CardDescription>
              </CardHeader>
              <CardContent className="grid gap-3 sm:grid-cols-2">
                <Trend label="New users" points={data.newUsers} />
                <Trend label="New vendors" points={data.newVendors} />
                <Trend label="New products" points={data.newProducts} />
                <Trend label="Booking requests" points={data.bookingRequestTrend} />
                <Trend label="Confirmed orders" points={data.confirmedOrdersTrend} />
                <Trend label="Completed orders" points={data.completedOrdersTrend} />
              </CardContent>
            </Card>

            <Card className="rounded-2xl">
              <CardHeader className="flex-row items-center justify-between">
                <div>
                  <CardTitle>Recent activity</CardTitle>
                  <CardDescription>Immutable audit trail.</CardDescription>
                </div>
                <Button variant="outline" size="sm" className="rounded-xl" asChild>
                  <Link href={ROUTES.admin.audit}>View all</Link>
                </Button>
              </CardHeader>
              <CardContent className="space-y-3">
                {data.recentActivity.length === 0 ? (
                  <p className="text-muted-foreground text-sm">No audit events yet.</p>
                ) : (
                  data.recentActivity.map((row) => (
                    <div key={row.id} className="flex items-start justify-between gap-3 text-sm">
                      <div>
                        <p className="font-medium">{row.action.replaceAll("_", " ")}</p>
                        <p className="text-muted-foreground">
                          {row.actorName} · {row.resourceType ?? "—"}
                        </p>
                      </div>
                      <AdminStatusBadge label={new Date(row.createdAt).toLocaleString()} />
                    </div>
                  ))
                )}
              </CardContent>
            </Card>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button className="rounded-xl" asChild>
              <Link href={ROUTES.admin.vendors}>
                <BadgeCheck className="mr-2 size-4" />
                Review vendors
              </Link>
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <Link href={ROUTES.admin.products}>
                <PackageSearch className="mr-2 size-4" />
                Moderate products
              </Link>
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <Link href={ROUTES.admin.tickets}>
                <LifeBuoy className="mr-2 size-4" />
                Support
              </Link>
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <Link href={ROUTES.admin.users}>
                <Users className="mr-2 size-4" />
                Users
              </Link>
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <Link href={ROUTES.admin.audit}>
                <ScrollText className="mr-2 size-4" />
                Audit logs
              </Link>
            </Button>
          </div>
        </>
      ) : null}
    </div>
  );
}

function Trend({ label, points }: { label: string; points: { date: string; count: number }[] }) {
  const total = points.reduce((sum, point) => sum + point.count, 0);
  return (
    <div className="border-border/60 rounded-xl border p-3">
      <p className="text-muted-foreground text-xs">{label}</p>
      <p className="text-lg font-semibold">{total}</p>
    </div>
  );
}
