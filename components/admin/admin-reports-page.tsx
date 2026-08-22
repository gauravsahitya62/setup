"use client";

import * as React from "react";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ADMIN_DATE_RANGES, adminErrorMessage, formatEstimatedValue } from "@/lib/admin/labels";
import { ApiClientError, adminApi, type AdminDateRange, type AdminReportsDto } from "@/services";

export function AdminReportsPage() {
  const [range, setRange] = React.useState<AdminDateRange>("DAYS_30");
  const [from, setFrom] = React.useState("");
  const [to, setTo] = React.useState("");
  const [data, setData] = React.useState<AdminReportsDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(
        await adminApi.reports(
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
    <div className="space-y-6">
      <AdminPageHeader
        title="Reports"
        description="Operational metrics. Financial figures are estimated rental value — FlowSet does not collect payments in MVP."
      />

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
            <Input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="w-auto rounded-xl"
            />
            <Input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="w-auto rounded-xl"
            />
          </>
        ) : null}
      </div>

      {loading ? <SkeletonLoader className="h-40 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load reports" description={error} /> : null}

      {data ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Stat title="Booking requests" value={data.bookingRequests} />
            <Stat title="Confirmed orders" value={data.confirmedOrders} />
            <Stat title="Completed orders" value={data.completedOrders} />
            <Stat
              title={data.financialLabel || "Estimated rental value"}
              value={formatEstimatedValue(data.estimatedRentalValue)}
            />
            <Stat title="Published reviews" value={data.publishedReviews} />
            <Stat title="Reported reviews" value={data.reportedReviews} />
            <Stat title="New users" value={data.newUsers} />
            <Stat title="Cancelled orders" value={data.cancelledOrders} />
          </div>

          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle>Vendor performance</CardTitle>
              <CardDescription>
                Acceptance, completion, rating, and estimated booking value.
              </CardDescription>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              <table className="w-full min-w-[840px] text-sm">
                <thead className="text-muted-foreground text-left text-xs tracking-wide uppercase">
                  <tr>
                    <th className="py-2 pr-3">Vendor</th>
                    <th className="py-2 pr-3">Requests</th>
                    <th className="py-2 pr-3">Accept</th>
                    <th className="py-2 pr-3">Reject</th>
                    <th className="py-2 pr-3">Completed</th>
                    <th className="py-2 pr-3">Cancel</th>
                    <th className="py-2 pr-3">Rating</th>
                    <th className="py-2 pr-3">Est. booking value</th>
                  </tr>
                </thead>
                <tbody>
                  {data.vendorPerformance.map((row) => (
                    <tr key={row.vendorId} className="border-t">
                      <td className="py-2 pr-3 font-medium">{row.companyName}</td>
                      <td className="py-2 pr-3">{row.bookingRequests}</td>
                      <td className="py-2 pr-3">{pct(row.acceptanceRate)}</td>
                      <td className="py-2 pr-3">{pct(row.rejectionRate)}</td>
                      <td className="py-2 pr-3">{row.completedOrders}</td>
                      <td className="py-2 pr-3">{pct(row.cancellationRate)}</td>
                      <td className="py-2 pr-3">{Number(row.averageRating).toFixed(1)}</td>
                      <td className="py-2 pr-3">
                        {formatEstimatedValue(row.estimatedBookingValue)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>

          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle>Product performance</CardTitle>
              <CardDescription>
                Views, project adds, bookings, favorites, and estimated booking value.
              </CardDescription>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              <table className="w-full min-w-[840px] text-sm">
                <thead className="text-muted-foreground text-left text-xs tracking-wide uppercase">
                  <tr>
                    <th className="py-2 pr-3">Product</th>
                    <th className="py-2 pr-3">Views</th>
                    <th className="py-2 pr-3">Project adds</th>
                    <th className="py-2 pr-3">Requests</th>
                    <th className="py-2 pr-3">Confirmed</th>
                    <th className="py-2 pr-3">Completed</th>
                    <th className="py-2 pr-3">Favorites</th>
                    <th className="py-2 pr-3">Est. booking value</th>
                  </tr>
                </thead>
                <tbody>
                  {data.productPerformance.map((row) => (
                    <tr key={row.productId} className="border-t">
                      <td className="py-2 pr-3 font-medium">{row.productName}</td>
                      <td className="py-2 pr-3">{row.views}</td>
                      <td className="py-2 pr-3">{row.projectAdds}</td>
                      <td className="py-2 pr-3">{row.bookingRequests}</td>
                      <td className="py-2 pr-3">{row.confirmedBookings}</td>
                      <td className="py-2 pr-3">{row.completedOrders}</td>
                      <td className="py-2 pr-3">{row.favorites}</td>
                      <td className="py-2 pr-3">
                        {formatEstimatedValue(row.estimatedBookingValue)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  );
}

function Stat({ title, value }: { title: string; value: number | string }) {
  return (
    <Card className="rounded-2xl">
      <CardHeader className="pb-2">
        <CardDescription>{title}</CardDescription>
        <CardTitle className="text-2xl">{value}</CardTitle>
      </CardHeader>
    </Card>
  );
}

function pct(value: number) {
  return `${Number(value).toFixed(1)}%`;
}
