"use client";

import * as React from "react";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_STYLES,
  bookingErrorMessage,
  formatBookingDate,
} from "@/lib/booking/labels";
import { formatEstimatedRental } from "@/lib/booking/pricing";
import { bookingApi, mapBookingSummary, type VendorBookingStatsDto } from "@/services";
import type { BookingRequest } from "@/types";
import { cn } from "@/lib/utils";

const BUCKETS: {
  key: keyof VendorBookingStatsDto;
  status: BookingRequest["status"] | "";
  title: string;
}[] = [
  { key: "pending", status: "PENDING", title: "New / pending requests" },
  { key: "accepted", status: "ACCEPTED", title: "Accepted requests" },
  { key: "confirmed", status: "CONFIRMED", title: "Upcoming confirmed requests" },
  { key: "rejected", status: "REJECTED", title: "Rejected requests" },
];

export function VendorBookingRequestInbox() {
  const [stats, setStats] = React.useState<VendorBookingStatsDto | null>(null);
  const [requests, setRequests] = React.useState<BookingRequest[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const [statsDto, page] = await Promise.all([
          bookingApi.vendor.stats(),
          bookingApi.vendor.list({ size: 50 }),
        ]);
        if (!cancelled) {
          setStats(statsDto);
          setRequests(page.content.map(mapBookingSummary));
        }
      } catch (err) {
        if (!cancelled) {
          setError(bookingErrorMessage(err));
          setRequests([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) return <SkeletonLoader variant="text" count={6} />;
  if (error) return <EmptyState title="Could not load requests" description={error} />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Booking requests</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Accepting a request reserves inventory for the requested dates.
        </p>
      </div>
      {stats ? (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {BUCKETS.map((bucket) => (
            <Card key={bucket.key} className="rounded-2xl">
              <CardHeader className="pb-2">
                <CardTitle className="text-muted-foreground text-sm">{bucket.title}</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-semibold tabular-nums">
                {stats[bucket.key]}
              </CardContent>
            </Card>
          ))}
        </div>
      ) : null}
      {requests.length === 0 ? (
        <EmptyState
          title="No booking requests"
          description="When planners send requests, they will appear here."
        />
      ) : (
        <div className="space-y-6">
          {BUCKETS.map((bucket) => {
            const rows = requests.filter((request) => request.status === bucket.status);
            if (rows.length === 0) return null;
            return (
              <section key={bucket.key} className="space-y-3">
                <h2 className="text-lg font-semibold">{bucket.title}</h2>
                <div className="divide-y rounded-2xl border">
                  {rows.map((request) => (
                    <div
                      key={request.id}
                      className="flex flex-col gap-3 px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between"
                    >
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="truncate text-sm font-medium">{request.projectName}</p>
                          <Badge
                            className={cn(
                              "rounded-md border-0",
                              BOOKING_STATUS_STYLES[request.status]
                            )}
                          >
                            {BOOKING_STATUS_LABELS[request.status]}
                          </Badge>
                        </div>
                        <p className="text-muted-foreground mt-1 text-xs">
                          {request.plannerName} · {request.itemsSummary}
                        </p>
                        <p className="text-muted-foreground text-xs">
                          Event {formatBookingDate(request.eventDate)} ·{" "}
                          {formatBookingDate(request.requestedAt)}
                        </p>
                      </div>
                      <div className="flex items-center gap-3">
                        <p className="text-sm font-semibold">
                          {formatEstimatedRental(request.amount ?? 0, request.currency ?? "INR")}
                        </p>
                        <Button size="sm" className="rounded-lg" asChild>
                          <Link href={ROUTES.vendor.request(request.id)}>Open</Link>
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
