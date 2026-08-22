"use client";

import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_STYLES,
  formatBookingDate,
} from "@/lib/booking/labels";
import { formatEstimatedRental } from "@/lib/booking/pricing";
import type { BookingRequest } from "@/types";
import { cn } from "@/lib/utils";

interface ProjectBookingsSectionProps {
  projectId: string;
  requests: BookingRequest[];
}

export function ProjectBookingsSection({ projectId, requests }: ProjectBookingsSectionProps) {
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Booking Requests</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Send the same requirement to multiple vendors. Inventory is reserved only when a vendor
            accepts.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" className="rounded-xl" asChild>
            <Link href={ROUTES.planner.bookingRequests}>Request history</Link>
          </Button>
          <Button className="rounded-xl" asChild>
            <Link href={ROUTES.planner.projectBookingNew(projectId)}>Request booking</Link>
          </Button>
        </div>
      </div>

      {requests.length === 0 ? (
        <EmptyState
          title="No booking requests yet"
          description="Select products from this project and send a request to one or more vendors."
          actionLabel="Request booking"
          onAction={() => {
            window.location.href = ROUTES.planner.projectBookingNew(projectId);
          }}
        />
      ) : (
        <div className="divide-border/70 border-border/70 divide-y rounded-2xl border">
          {requests.map((request) => (
            <Link
              key={request.id}
              href={ROUTES.planner.bookingRequest(request.id)}
              className="hover:bg-muted/30 flex flex-col gap-2 px-4 py-3.5 transition-colors sm:flex-row sm:items-center sm:justify-between"
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
                <p className="text-muted-foreground mt-1 line-clamp-1 text-xs">
                  {request.itemsSummary}
                </p>
                <p className="text-muted-foreground mt-1 text-xs">
                  Event {formatBookingDate(request.eventDate)} · Sent{" "}
                  {formatBookingDate(request.requestedAt.slice(0, 10))}
                </p>
              </div>
              <p className="shrink-0 text-sm font-semibold tabular-nums">
                {formatEstimatedRental(request.amount ?? 0, request.currency ?? "INR")}
              </p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
