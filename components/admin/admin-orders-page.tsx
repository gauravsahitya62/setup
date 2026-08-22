"use client";

import * as React from "react";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { adminErrorMessage, formatEstimatedValue } from "@/lib/admin/labels";
import { ApiClientError, adminApi } from "@/services";
import type { OrderSummaryDto } from "@/services/order-api";

export function AdminOrdersPage() {
  const [status, setStatus] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<OrderSummaryDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.orders({ status: status || undefined, page, size: 20 });
      setRows(result.content);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [status, page]);

  React.useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Orders"
        description="Support view of marketplace orders. Amounts are estimated rental value, not collected revenue."
      />
      <select
        className="bg-background w-fit rounded-xl border px-3 py-2 text-sm"
        value={status}
        onChange={(e) => {
          setStatus(e.target.value);
          setPage(0);
        }}
      >
        <option value="">All statuses</option>
        <option value="CONFIRMED">Confirmed</option>
        <option value="IN_PROGRESS">In progress</option>
        <option value="COMPLETED">Completed</option>
        <option value="CANCELLED">Cancelled</option>
      </select>
      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load orders" description={error} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No orders match" /> : null}
      <div className="overflow-x-auto rounded-2xl border">
        <table className="w-full min-w-[860px] text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-left text-xs tracking-wide uppercase">
            <tr>
              <th className="px-4 py-3">Order</th>
              <th className="px-4 py-3">Planner</th>
              <th className="px-4 py-3">Vendor</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Dates</th>
              <th className="px-4 py-3">Est. value</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((order) => (
              <tr key={order.id} className="border-t">
                <td className="px-4 py-3">
                  <p className="font-medium">{order.orderNumber}</p>
                  <p className="text-muted-foreground text-xs">{order.itemsSummary}</p>
                </td>
                <td className="px-4 py-3">{order.plannerName ?? "—"}</td>
                <td className="px-4 py-3">{order.vendorName ?? "—"}</td>
                <td className="px-4 py-3">{order.status}</td>
                <td className="text-muted-foreground px-4 py-3">
                  {order.rentalStartDate} → {order.rentalEndDate}
                </td>
                <td className="px-4 py-3">{formatEstimatedValue(order.totalAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex justify-end gap-2">
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
