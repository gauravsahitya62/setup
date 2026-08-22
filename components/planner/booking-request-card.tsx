"use client";

import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import type { BookingRequest } from "@/types";
import { ROUTES } from "@/constants";
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_STYLES,
  formatBookingDate,
} from "@/lib/booking/labels";
import { formatEstimatedRental } from "@/lib/booking/pricing";
import { cn } from "@/lib/utils";

interface BookingRequestCardProps {
  request: BookingRequest;
  href?: string;
  className?: string;
}

export function BookingRequestCard({ request, href, className }: BookingRequestCardProps) {
  const initials = request.vendorName
    .split(" ")
    .map((part) => part.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();
  const target = href ?? ROUTES.planner.bookingRequest(request.id);

  return (
    <Link href={target} className="block">
      <Card
        className={cn(
          "transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md",
          className
        )}
      >
        <CardHeader className="flex-row items-start justify-between gap-3 space-y-0">
          <div className="flex items-center gap-3">
            <Avatar>
              <AvatarImage src={request.vendorLogoUrl} alt={request.vendorName} />
              <AvatarFallback>{initials}</AvatarFallback>
            </Avatar>
            <div>
              <CardTitle className="text-base">{request.vendorName}</CardTitle>
              <p className="text-muted-foreground mt-0.5 text-sm">{request.projectName}</p>
            </div>
          </div>
          <Badge className={cn("rounded-lg border-0", BOOKING_STATUS_STYLES[request.status])}>
            {BOOKING_STATUS_LABELS[request.status]}
          </Badge>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-muted-foreground text-sm leading-relaxed">{request.itemsSummary}</p>
          <div className="flex items-center justify-between gap-3 text-sm">
            <span className="text-muted-foreground">
              {formatBookingDate(request.requestedAt.slice(0, 10))}
            </span>
            {typeof request.amount === "number" ? (
              <span className="font-semibold tracking-tight">
                {formatEstimatedRental(request.amount, request.currency ?? "INR")}
              </span>
            ) : null}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

interface RecentBookingRequestsProps {
  requests: BookingRequest[];
}

export function RecentBookingRequests({ requests }: RecentBookingRequestsProps) {
  return (
    <section>
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">Recent booking requests</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Track pending, accepted, and confirmed requests across your projects.
          </p>
        </div>
        <Button variant="outline" size="sm" className="rounded-xl" asChild>
          <Link href={ROUTES.planner.bookingRequests}>View all</Link>
        </Button>
      </div>

      {requests.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          No booking requests yet. Open a project to send one.
        </p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {requests.map((request, index) => (
            <div
              key={request.id}
              className="animate-in fade-in slide-in-from-bottom-3 fill-mode-both duration-500"
              style={{ animationDelay: `${index * 60}ms` }}
            >
              <BookingRequestCard request={request} />
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
