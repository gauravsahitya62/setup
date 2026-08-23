"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/shared/modal";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { track } from "@/lib/analytics";
import {
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_STYLES,
  REJECTION_REASON_LABELS,
  REJECTION_REASONS,
  TIMELINE_LABELS,
  bookingErrorMessage,
  formatBookingDate,
  type BookingRejectionReason,
} from "@/lib/booking/labels";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import { EVENT_TYPE_LABELS } from "@/lib/projects/labels";
import { ApiClientError, bookingApi, type BookingRequestDto } from "@/services";
import type { EventType } from "@/types";
import { cn } from "@/lib/utils";

interface BookingRequestDetailProps {
  requestId: string;
  role: "planner" | "vendor";
}

export function BookingRequestDetail({ requestId, role }: BookingRequestDetailProps) {
  const router = useRouter();
  const [request, setRequest] = React.useState<BookingRequestDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [acceptOpen, setAcceptOpen] = React.useState(false);
  const [rejectOpen, setRejectOpen] = React.useState(false);
  const [rejectReason, setRejectReason] =
    React.useState<BookingRejectionReason>("INVENTORY_UNAVAILABLE");
  const [rejectNote, setRejectNote] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const dto =
        role === "vendor"
          ? await bookingApi.vendor.get(requestId)
          : await bookingApi.get(requestId);
      setRequest(dto);
    } catch (err) {
      setError(bookingErrorMessage(err));
      setRequest(null);
    } finally {
      setLoading(false);
    }
  }, [requestId, role]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const accept = async () => {
    setBusy(true);
    try {
      const dto = await bookingApi.vendor.accept(requestId);
      setRequest(dto);
      setAcceptOpen(false);
      track("booking_request_accepted");
      if (dto.orderId) track("order_created");
      toast.success("Booking accepted and inventory reserved");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : bookingErrorMessage(err));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const reject = async () => {
    setBusy(true);
    try {
      const dto = await bookingApi.vendor.reject(requestId, {
        reason: rejectReason,
        note: rejectNote || undefined,
      });
      setRequest(dto);
      setRejectOpen(false);
      track("booking_request_rejected");
      toast.success("Request rejected");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : bookingErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    setBusy(true);
    try {
      const dto = await bookingApi.cancel(requestId);
      setRequest(dto);
      toast.success("Request cancelled");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : bookingErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <SkeletonLoader variant="text" count={8} />;
  if (error || !request) {
    return (
      <EmptyState
        title="Booking request not found"
        description={error ?? "This request is unavailable."}
        actionLabel="Back"
        onAction={() =>
          router.push(role === "vendor" ? ROUTES.vendor.requests : ROUTES.planner.bookingRequests)
        }
      />
    );
  }

  const canCancel =
    role === "planner" && (request.status === "PENDING" || request.status === "CONFIRMED");
  const canRespond = role === "vendor" && request.status === "PENDING";
  const backHref = role === "vendor" ? ROUTES.vendor.requests : ROUTES.planner.bookingRequests;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Button variant="ghost" className="mb-2 -ml-2 rounded-xl" asChild>
            <Link href={backHref}>Back to requests</Link>
          </Button>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{request.projectName}</h1>
            <Badge className={cn("rounded-lg border-0", BOOKING_STATUS_STYLES[request.status])}>
              {BOOKING_STATUS_LABELS[request.status]}
            </Badge>
          </div>
          <p className="text-muted-foreground mt-1 text-sm">
            {role === "vendor" ? request.plannerName : request.vendorName} · Request {request.id}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canRespond ? (
            <>
              <Button variant="outline" className="rounded-xl" onClick={() => setRejectOpen(true)}>
                Reject request
              </Button>
              <Button className="rounded-xl" onClick={() => setAcceptOpen(true)}>
                Accept request
              </Button>
            </>
          ) : null}
          {canCancel ? (
            <Button
              variant="outline"
              className="rounded-xl"
              disabled={busy}
              onClick={() => void cancel()}
            >
              Cancel request
            </Button>
          ) : null}
          {request.orderId ? (
            <Button className="rounded-xl" asChild>
              <Link
                href={
                  role === "vendor"
                    ? ROUTES.vendor.order(request.orderId)
                    : ROUTES.planner.order(request.orderId)
                }
              >
                View order
              </Link>
            </Button>
          ) : null}
        </div>
      </div>

      <section className="border-border/70 grid gap-4 rounded-2xl border p-4 sm:grid-cols-2">
        <Detail label="Project" value={request.projectName} />
        <Detail
          label="Event"
          value={request.eventType ? EVENT_TYPE_LABELS[request.eventType as EventType] : "—"}
        />
        <Detail label="Event date" value={formatBookingDate(request.eventDate)} />
        <Detail
          label="Rental dates"
          value={`${formatBookingDate(request.rentalStartDate)} – ${formatBookingDate(request.rentalEndDate)} (${request.rentalDays} days)`}
        />
        <Detail label="Venue" value={request.venueName} />
        <Detail label="City" value={[request.city, request.state].filter(Boolean).join(", ")} />
        <Detail label="Vendor" value={request.vendorName} />
        <Detail label="Planner" value={request.plannerName} />
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Products</h2>
        <div className="divide-y rounded-2xl border">
          {request.items.map((item) => (
            <div
              key={item.id}
              className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="text-sm font-medium">
                  {item.productName} · {item.variantName}
                </p>
                <p className="text-muted-foreground text-xs">
                  {item.quantity} units · {formatEstimatedRental(toMoney(item.pricePerDay))}/day
                  {item.availabilityMessage ? ` · ${item.availabilityMessage}` : ""}
                </p>
              </div>
              <p className="text-sm font-semibold tabular-nums">
                {formatEstimatedRental(toMoney(item.lineEstimatedValue), request.currency)}
              </p>
            </div>
          ))}
        </div>
        <p className="text-sm font-semibold">
          Estimated rental value{" "}
          {formatEstimatedRental(toMoney(request.estimatedRentalValue), request.currency)}
        </p>
        <p className="text-muted-foreground text-xs">Estimated rental value, not a final amount.</p>
      </section>

      {request.delivery ? (
        <section className="border-border/70 space-y-2 rounded-2xl border p-4 text-sm">
          <h2 className="text-lg font-semibold">Delivery details</h2>
          <p>
            Deliver {formatBookingDate(request.delivery.deliveryDate)}
            {request.delivery.preferredDeliveryTime
              ? ` at ${request.delivery.preferredDeliveryTime}`
              : ""}
          </p>
          <p>
            Pickup {formatBookingDate(request.delivery.pickupDate)}
            {request.delivery.preferredPickupTime
              ? ` at ${request.delivery.preferredPickupTime}`
              : ""}
          </p>
          <p>{request.delivery.venueAddress}</p>
          <p>
            {request.delivery.contactPerson} {request.delivery.contactPhone}
          </p>
          {request.delivery.deliveryNotes ? <p>{request.delivery.deliveryNotes}</p> : null}
        </section>
      ) : null}

      {request.plannerNotes ? (
        <section className="border-border/70 rounded-2xl border p-4 text-sm">
          <h2 className="text-lg font-semibold">Planner notes</h2>
          <p className="text-muted-foreground mt-2 whitespace-pre-wrap">{request.plannerNotes}</p>
        </section>
      ) : null}

      {request.rejectionReason ? (
        <p className="text-destructive text-sm">
          Rejection: {REJECTION_REASON_LABELS[request.rejectionReason]}
          {request.vendorNotes ? ` — ${request.vendorNotes}` : ""}
        </p>
      ) : null}

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Request timeline</h2>
        <ol className="border-border/70 space-y-3 border-l pl-4">
          {request.timeline.map((event) => (
            <li key={event.id}>
              <p className="text-sm font-medium">
                {TIMELINE_LABELS[event.eventType] ?? event.eventType}
              </p>
              <p className="text-muted-foreground text-xs">{event.description}</p>
              <p className="text-muted-foreground text-xs">{formatBookingDate(event.createdAt)}</p>
            </li>
          ))}
        </ol>
      </section>

      <Modal
        open={acceptOpen}
        onOpenChange={setAcceptOpen}
        title="Accept this booking request?"
        description="Inventory will be reserved for the requested dates. This cannot be undone without planner cancellation."
        footer={
          <>
            <Button variant="outline" onClick={() => setAcceptOpen(false)}>
              Cancel
            </Button>
            <Button disabled={busy} onClick={() => void accept()}>
              Accept booking
            </Button>
          </>
        }
      >
        <div className="space-y-2 text-sm">
          {request.items.map((item) => (
            <p key={item.id}>
              {item.quantity} × {item.productName} ({item.variantName})
            </p>
          ))}
          <p>
            {formatBookingDate(request.rentalStartDate)} –{" "}
            {formatBookingDate(request.rentalEndDate)}
          </p>
          <p className="font-semibold">
            Estimated rental value{" "}
            {formatEstimatedRental(toMoney(request.estimatedRentalValue), request.currency)}
          </p>
          {request.items[0]?.availabilityMessage ? (
            <p className="text-muted-foreground">{request.items[0].availabilityMessage}</p>
          ) : (
            <p className="text-muted-foreground">Availability will be re-checked on accept.</p>
          )}
        </div>
      </Modal>

      <Modal
        open={rejectOpen}
        onOpenChange={setRejectOpen}
        title="Reject request"
        description="Choose a reason. The planner will see this on the request."
        footer={
          <>
            <Button variant="outline" onClick={() => setRejectOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" disabled={busy} onClick={() => void reject()}>
              Reject request
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="reason">Reason</Label>
            <select
              id="reason"
              className="border-input h-9 w-full rounded-md border bg-transparent px-3 text-sm"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value as BookingRejectionReason)}
            >
              {REJECTION_REASONS.map((reason) => (
                <option key={reason} value={reason}>
                  {REJECTION_REASON_LABELS[reason]}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="reject-note">Additional note</Label>
            <Textarea
              id="reject-note"
              rows={3}
              value={rejectNote}
              onChange={(event) => setRejectNote(event.target.value)}
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}

function Detail({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-muted-foreground text-xs">{label}</p>
      <p className="text-sm font-medium">{value || "—"}</p>
    </div>
  );
}
