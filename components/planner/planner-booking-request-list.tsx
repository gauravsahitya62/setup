"use client";

import * as React from "react";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_STYLES,
  bookingErrorMessage,
  formatBookingDate,
} from "@/lib/booking/labels";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import { bookingApi, mapBookingSummary } from "@/services";
import type { BookingStatus } from "@/types";
import { cn } from "@/lib/utils";

const FILTERS: { id: "" | BookingStatus; label: string }[] = [
  { id: "", label: "All" },
  { id: "PENDING", label: "Pending" },
  { id: "ACCEPTED", label: "Accepted" },
  { id: "REJECTED", label: "Rejected" },
  { id: "CANCELLED", label: "Cancelled" },
  { id: "CONFIRMED", label: "Confirmed" },
];

export function PlannerBookingRequestList() {
  const [status, setStatus] = React.useState<"" | BookingStatus>("");
  const [rows, setRows] = React.useState<ReturnType<typeof mapBookingSummary>[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const page = await bookingApi.list({ status, size: 50 });
        if (!cancelled) setRows(page.content.map(mapBookingSummary));
      } catch (err) {
        if (!cancelled) {
          setError(bookingErrorMessage(err));
          setRows([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [status]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Booking requests</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Independent requests per vendor. Inventory is reserved only after a vendor accepts.
        </p>
      </div>
      <div className="flex flex-wrap gap-2">
        {FILTERS.map((filter) => (
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
        <EmptyState title="Could not load requests" description={error} />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No booking requests"
          description="Open a project workspace and choose Request booking."
          actionLabel="Projects"
          onAction={() => {
            window.location.href = ROUTES.planner.projects;
          }}
        />
      ) : (
        <div className="divide-y rounded-2xl border">
          {rows.map((request) => (
            <Link
              key={request.id}
              href={ROUTES.planner.bookingRequest(request.id)}
              className="hover:bg-muted/30 flex flex-col gap-2 px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="truncate text-sm font-medium">{request.vendorName}</p>
                  <Badge
                    className={cn("rounded-md border-0", BOOKING_STATUS_STYLES[request.status])}
                  >
                    {BOOKING_STATUS_LABELS[request.status]}
                  </Badge>
                </div>
                <p className="text-muted-foreground mt-1 text-xs">
                  {request.projectName} · Event {formatBookingDate(request.eventDate)} ·{" "}
                  {request.itemsSummary}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">
                  {formatEstimatedRental(toMoney(request.amount), request.currency ?? "INR")}
                </p>
                <p className="text-muted-foreground text-xs">
                  {formatBookingDate(request.requestedAt)}
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
