"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { AuthenticatedFileGallery } from "@/components/shared/authenticated-file-gallery";
import { EmptyState } from "@/components/shared/empty-state";
import { Modal } from "@/components/shared/modal";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { OrderStatusBadge } from "@/components/orders/order-status-badge";
import { OrderTimeline } from "@/components/orders/order-timeline";
import { OrderReviewPrompt } from "@/components/reviews/order-review-prompt";
import { ROUTES } from "@/constants";
import { track } from "@/lib/analytics";
import {
  DELIVERY_STATUS_LABELS,
  PICKUP_STATUS_LABELS,
  formatOrderDate,
  formatOrderDateTime,
  formatTime,
  orderErrorMessage,
} from "@/lib/orders/labels";
import { formatEstimatedRental, toMoney } from "@/lib/booking/pricing";
import { EVENT_TYPE_LABELS } from "@/lib/projects/labels";
import { ApiClientError, orderApi, type OrderDto } from "@/services";
import type { EventType } from "@/types";

interface OrderDetailProps {
  orderId: string;
  role: "planner" | "vendor";
}

export function OrderDetail({ orderId, role }: OrderDetailProps) {
  const router = useRouter();
  const [order, setOrder] = React.useState<OrderDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [note, setNote] = React.useState("");
  const [deliveryOpen, setDeliveryOpen] = React.useState(false);
  const [pickupOpen, setPickupOpen] = React.useState(false);
  const [cancelOpen, setCancelOpen] = React.useState(false);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const dto =
        role === "vendor" ? await orderApi.vendor.get(orderId) : await orderApi.get(orderId);
      setOrder(dto);
    } catch (err) {
      setError(orderErrorMessage(err));
      setOrder(null);
    } finally {
      setLoading(false);
    }
  }, [orderId, role]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const run = async (
    action: () => Promise<OrderDto>,
    success: string,
    event?: "delivery_completed" | "order_completed"
  ) => {
    setBusy(true);
    try {
      const dto = await action();
      setOrder(dto);
      toast.success(success);
      if (event) track(event);
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : orderErrorMessage(err));
      await load();
    } finally {
      setBusy(false);
    }
  };

  const addNote = async () => {
    if (!note.trim()) return;
    setBusy(true);
    try {
      await orderApi.addNote(orderId, note.trim());
      setNote("");
      toast.success("Note added");
      await load();
    } catch (err) {
      toast.error(orderErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const uploadProof = async (file: File | undefined) => {
    if (!file) return;
    setBusy(true);
    try {
      await orderApi.delivery.uploadProof(orderId, file);
      toast.success("Proof of delivery uploaded");
      await load();
    } catch (err) {
      toast.error(orderErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <SkeletonLoader variant="text" count={8} />;
  if (error || !order) {
    return (
      <EmptyState
        title="Order not found"
        description={error ?? "This order is unavailable."}
        actionLabel="Back"
        onAction={() =>
          router.push(role === "vendor" ? ROUTES.vendor.bookings : ROUTES.planner.orders)
        }
      />
    );
  }

  const actions = order.actions;
  const backHref = role === "vendor" ? ROUTES.vendor.bookings : ROUTES.planner.orders;
  const currency = order.currency ?? "INR";

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Button variant="ghost" className="mb-2 -ml-2 rounded-xl" asChild>
            <Link href={backHref}>Back to orders</Link>
          </Button>
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{order.orderNumber}</h1>
            <OrderStatusBadge status={order.status} />
          </div>
          <p className="text-muted-foreground mt-1 text-sm">
            {role === "vendor" ? order.plannerName : order.vendorName} · {order.projectName}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {actions.canScheduleDelivery ? (
            <Button className="rounded-xl" onClick={() => setDeliveryOpen(true)}>
              Schedule delivery
            </Button>
          ) : null}
          {actions.canMarkOutForDelivery ? (
            <Button
              className="rounded-xl"
              disabled={busy}
              onClick={() =>
                void run(() => orderApi.vendor.outForDelivery(orderId), "Marked out for delivery")
              }
            >
              Mark out for delivery
            </Button>
          ) : null}
          {actions.canMarkDelivered ? (
            <Button
              className="rounded-xl"
              disabled={busy}
              onClick={() =>
                void run(
                  () => orderApi.vendor.markDelivered(orderId),
                  "Marked delivered",
                  "delivery_completed"
                )
              }
            >
              Mark delivered
            </Button>
          ) : null}
          {actions.canMarkEventCompleted ? (
            <Button
              className="rounded-xl"
              disabled={busy}
              onClick={() =>
                void run(() => orderApi.vendor.markEventCompleted(orderId), "Event marked complete")
              }
            >
              Mark event completed
            </Button>
          ) : null}
          {actions.canSchedulePickup ? (
            <Button className="rounded-xl" onClick={() => setPickupOpen(true)}>
              Schedule pickup
            </Button>
          ) : null}
          {actions.canMarkPickedUp ? (
            <Button
              className="rounded-xl"
              disabled={busy}
              onClick={() =>
                void run(
                  () => orderApi.vendor.markPickedUp(orderId),
                  "Picked up — order completed",
                  "order_completed"
                )
              }
            >
              Mark picked up
            </Button>
          ) : null}
          {actions.canCancel ? (
            <Button variant="outline" className="rounded-xl" onClick={() => setCancelOpen(true)}>
              Cancel order
            </Button>
          ) : null}
        </div>
      </div>

      {role === "planner" && order.status === "COMPLETED" ? (
        <OrderReviewPrompt orderId={order.id} vendorName={order.vendorName ?? ""} />
      ) : null}

      <section className="border-border/70 grid gap-4 rounded-2xl border p-4 sm:grid-cols-2">
        <Detail label="Order number" value={order.orderNumber} />
        <Detail label="Project" value={order.projectName} />
        <Detail
          label="Event"
          value={order.eventType ? EVENT_TYPE_LABELS[order.eventType as EventType] : "—"}
        />
        <Detail label="Event date" value={formatOrderDate(order.eventDate)} />
        <Detail
          label="Rental dates"
          value={`${formatOrderDate(order.rentalStartDate)} – ${formatOrderDate(order.rentalEndDate)} (${order.rentalDays} days)`}
        />
        <Detail
          label={role === "vendor" ? "Planner" : "Vendor"}
          value={role === "vendor" ? order.plannerName : order.vendorName}
        />
        <Detail label="Delivery date" value={formatOrderDate(order.deliveryDate)} />
        <Detail label="Pickup date" value={formatOrderDate(order.pickupDate)} />
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Products</h2>
        <div className="divide-y rounded-2xl border">
          {order.items.map((item) => (
            <div
              key={item.id}
              className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="text-sm font-medium">
                  {item.productName} · {item.variantName}
                </p>
                <p className="text-muted-foreground text-xs">
                  {item.quantity} units ·{" "}
                  {formatEstimatedRental(toMoney(item.pricePerDay), currency)}/day ·{" "}
                  {item.rentalDays} days
                </p>
              </div>
              <p className="text-sm font-semibold tabular-nums">
                {formatEstimatedRental(toMoney(item.subtotal), currency)}
              </p>
            </div>
          ))}
        </div>
        <div className="space-y-1 text-sm">
          <p>Rental subtotal {formatEstimatedRental(toMoney(order.estimatedSubtotal), currency)}</p>
          <p>Delivery fee {formatEstimatedRental(toMoney(order.deliveryFee), currency)}</p>
          <p>
            Security deposit {formatEstimatedRental(toMoney(order.securityDeposit), currency)}{" "}
            (informational)
          </p>
          <p className="font-semibold">
            Total rental value {formatEstimatedRental(toMoney(order.totalAmount), currency)}
          </p>
          <p className="text-muted-foreground text-xs">
            Snapshots from confirmation. Security deposit is not included in the total. Payment has
            not been collected.
          </p>
        </div>
      </section>

      <section className="border-border/70 space-y-2 rounded-2xl border p-4 text-sm">
        <h2 className="text-lg font-semibold">Delivery</h2>
        {order.delivery ? (
          <>
            <p>
              Status: {DELIVERY_STATUS_LABELS[order.delivery.status]}
              {order.delivery.preferredTime ? ` · ${formatTime(order.delivery.preferredTime)}` : ""}
            </p>
            <p>
              {order.delivery.venueName}
              {order.delivery.venueAddress ? ` · ${order.delivery.venueAddress}` : ""}
            </p>
            <p>{[order.delivery.city, order.delivery.state].filter(Boolean).join(", ")}</p>
            <p>
              {order.delivery.contactPerson} {order.delivery.contactPhone}
            </p>
            {order.delivery.actualDeliveryTime ? (
              <p>Delivered {formatOrderDateTime(order.delivery.actualDeliveryTime)}</p>
            ) : null}
            {order.delivery.proofs.length > 0 ? (
              <AuthenticatedFileGallery
                files={order.delivery.proofs}
                meta={(proof) => (
                  <span className="text-muted-foreground text-xs">
                    {" "}
                    · {formatOrderDateTime(proof.uploadedAt)}
                  </span>
                )}
              />
            ) : (
              <p className="text-muted-foreground">No proof of delivery uploaded.</p>
            )}
          </>
        ) : (
          <p className="text-muted-foreground">Delivery has not been scheduled yet.</p>
        )}
        {actions.canUploadProof ? (
          <div className="pt-2">
            <Label htmlFor="pod">Upload proof (photo or PDF, max 10 MB)</Label>
            <Input
              id="pod"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif,application/pdf"
              className="mt-1 rounded-xl"
              disabled={busy}
              onChange={(event) => void uploadProof(event.target.files?.[0])}
            />
          </div>
        ) : null}
      </section>

      <section className="border-border/70 space-y-2 rounded-2xl border p-4 text-sm">
        <h2 className="text-lg font-semibold">Pickup</h2>
        {order.pickup ? (
          <>
            <p>Status: {PICKUP_STATUS_LABELS[order.pickup.status]}</p>
            <p>
              {formatOrderDate(order.pickup.pickupDate)}
              {order.pickup.preferredTime ? ` · ${formatTime(order.pickup.preferredTime)}` : ""}
            </p>
            {order.pickup.actualPickupTime ? (
              <p>Picked up {formatOrderDateTime(order.pickup.actualPickupTime)}</p>
            ) : null}
          </>
        ) : (
          <p className="text-muted-foreground">Pickup has not been scheduled yet.</p>
        )}
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Order timeline</h2>
        <OrderTimeline events={order.timeline} />
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Notes</h2>
        {order.notes.length === 0 ? (
          <p className="text-muted-foreground text-sm">No notes yet.</p>
        ) : null}
        <div className="space-y-2">
          {order.notes.map((row) => (
            <div key={row.id} className="rounded-xl border px-3 py-2 text-sm">
              <p className="whitespace-pre-wrap">{row.content}</p>
              <p className="text-muted-foreground mt-1 text-xs">
                {formatOrderDateTime(row.createdAt)}
              </p>
            </div>
          ))}
        </div>
        {actions.canAddNote ? (
          <div className="space-y-2">
            <Textarea
              className="rounded-xl"
              rows={3}
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Add a note"
            />
            <Button
              className="rounded-xl"
              disabled={busy || !note.trim()}
              onClick={() => void addNote()}
            >
              Add note
            </Button>
          </div>
        ) : null}
      </section>

      <ScheduleDeliveryModal
        open={deliveryOpen}
        onOpenChange={setDeliveryOpen}
        order={order}
        busy={busy}
        onSubmit={async (payload) => {
          await run(() => orderApi.vendor.scheduleDelivery(orderId, payload), "Delivery scheduled");
          setDeliveryOpen(false);
        }}
      />
      <SchedulePickupModal
        open={pickupOpen}
        onOpenChange={setPickupOpen}
        order={order}
        busy={busy}
        onSubmit={async (payload) => {
          await run(() => orderApi.vendor.schedulePickup(orderId, payload), "Pickup scheduled");
          setPickupOpen(false);
        }}
      />
      <Modal
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        title="Cancel this order?"
        description="Inventory reserved for this booking will be released. This cannot be undone."
        footer={
          <>
            <Button variant="outline" onClick={() => setCancelOpen(false)}>
              Keep order
            </Button>
            <Button
              variant="destructive"
              disabled={busy}
              onClick={() => {
                void run(() => orderApi.cancel(orderId), "Order cancelled");
                setCancelOpen(false);
              }}
            >
              Cancel order
            </Button>
          </>
        }
      >
        <p className="text-muted-foreground text-sm">
          {order.orderNumber} will be marked cancelled.
        </p>
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

function ScheduleDeliveryModal({
  open,
  onOpenChange,
  order,
  busy,
  onSubmit,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  order: OrderDto;
  busy: boolean;
  onSubmit: (payload: {
    deliveryDate: string;
    preferredTime?: string;
    venueName: string;
    venueAddress: string;
    city?: string;
    state?: string;
    contactPerson: string;
    contactPhone: string;
    deliveryNotes?: string;
    deliveryFee?: number;
    securityDeposit?: number;
  }) => Promise<void>;
}) {
  const [deliveryDate, setDeliveryDate] = React.useState(
    order.deliveryDate ?? order.rentalStartDate
  );
  const [preferredTime, setPreferredTime] = React.useState(
    order.delivery?.preferredTime?.slice(0, 5) ?? ""
  );
  const [venueName, setVenueName] = React.useState(order.delivery?.venueName ?? "");
  const [venueAddress, setVenueAddress] = React.useState(order.delivery?.venueAddress ?? "");
  const [city, setCity] = React.useState(order.delivery?.city ?? "");
  const [state, setState] = React.useState(order.delivery?.state ?? "");
  const [contactPerson, setContactPerson] = React.useState(order.delivery?.contactPerson ?? "");
  const [contactPhone, setContactPhone] = React.useState(order.delivery?.contactPhone ?? "");
  const [deliveryNotes, setDeliveryNotes] = React.useState(order.delivery?.deliveryNotes ?? "");
  const [deliveryFee, setDeliveryFee] = React.useState(String(order.deliveryFee ?? "0"));
  const [securityDeposit, setSecurityDeposit] = React.useState(
    String(order.securityDeposit ?? "0")
  );

  React.useEffect(() => {
    if (!open) return;
    setDeliveryDate(order.deliveryDate ?? order.rentalStartDate);
    setPreferredTime(order.delivery?.preferredTime?.slice(0, 5) ?? "");
    setVenueName(order.delivery?.venueName ?? "");
    setVenueAddress(order.delivery?.venueAddress ?? "");
    setCity(order.delivery?.city ?? "");
    setState(order.delivery?.state ?? "");
    setContactPerson(order.delivery?.contactPerson ?? "");
    setContactPhone(order.delivery?.contactPhone ?? "");
    setDeliveryNotes(order.delivery?.deliveryNotes ?? "");
    setDeliveryFee(String(order.deliveryFee ?? "0"));
    setSecurityDeposit(String(order.securityDeposit ?? "0"));
  }, [open, order]);

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Schedule delivery"
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            disabled={busy}
            onClick={() =>
              void onSubmit({
                deliveryDate,
                preferredTime: preferredTime ? `${preferredTime}:00` : undefined,
                venueName,
                venueAddress,
                city: city || undefined,
                state: state || undefined,
                contactPerson,
                contactPhone,
                deliveryNotes: deliveryNotes || undefined,
                deliveryFee: deliveryFee ? Number(deliveryFee) : undefined,
                securityDeposit: securityDeposit ? Number(securityDeposit) : undefined,
              })
            }
          >
            Schedule
          </Button>
        </>
      }
    >
      <div className="grid max-h-[60vh] gap-3 overflow-y-auto pr-1">
        <Field label="Delivery date">
          <Input
            type="date"
            className="rounded-xl"
            value={deliveryDate}
            onChange={(event) => setDeliveryDate(event.target.value)}
          />
        </Field>
        <Field label="Preferred time">
          <Input
            type="time"
            className="rounded-xl"
            value={preferredTime}
            onChange={(event) => setPreferredTime(event.target.value)}
          />
        </Field>
        <Field label="Venue">
          <Input
            className="rounded-xl"
            value={venueName}
            onChange={(event) => setVenueName(event.target.value)}
          />
        </Field>
        <Field label="Address">
          <Input
            className="rounded-xl"
            value={venueAddress}
            onChange={(event) => setVenueAddress(event.target.value)}
          />
        </Field>
        <Field label="City">
          <Input
            className="rounded-xl"
            value={city}
            onChange={(event) => setCity(event.target.value)}
          />
        </Field>
        <Field label="State">
          <Input
            className="rounded-xl"
            value={state}
            onChange={(event) => setState(event.target.value)}
          />
        </Field>
        <Field label="Contact person">
          <Input
            className="rounded-xl"
            value={contactPerson}
            onChange={(event) => setContactPerson(event.target.value)}
          />
        </Field>
        <Field label="Contact phone">
          <Input
            className="rounded-xl"
            value={contactPhone}
            onChange={(event) => setContactPhone(event.target.value)}
          />
        </Field>
        <Field label="Delivery fee (optional)">
          <Input
            type="number"
            min="0"
            className="rounded-xl"
            value={deliveryFee}
            onChange={(event) => setDeliveryFee(event.target.value)}
          />
        </Field>
        <Field label="Security deposit (informational)">
          <Input
            type="number"
            min="0"
            className="rounded-xl"
            value={securityDeposit}
            onChange={(event) => setSecurityDeposit(event.target.value)}
          />
        </Field>
        <Field label="Notes">
          <Textarea
            className="rounded-xl"
            value={deliveryNotes}
            onChange={(event) => setDeliveryNotes(event.target.value)}
          />
        </Field>
      </div>
    </Modal>
  );
}

function SchedulePickupModal({
  open,
  onOpenChange,
  order,
  busy,
  onSubmit,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  order: OrderDto;
  busy: boolean;
  onSubmit: (payload: {
    pickupDate: string;
    preferredTime?: string;
    notes?: string;
  }) => Promise<void>;
}) {
  const [pickupDate, setPickupDate] = React.useState(order.pickupDate ?? order.rentalEndDate);
  const [preferredTime, setPreferredTime] = React.useState(
    order.pickup?.preferredTime?.slice(0, 5) ?? ""
  );
  const [notes, setNotes] = React.useState(order.pickup?.notes ?? "");

  React.useEffect(() => {
    if (!open) return;
    setPickupDate(order.pickupDate ?? order.rentalEndDate);
    setPreferredTime(order.pickup?.preferredTime?.slice(0, 5) ?? "");
    setNotes(order.pickup?.notes ?? "");
  }, [open, order]);

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Schedule pickup"
      footer={
        <>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            disabled={busy}
            onClick={() =>
              void onSubmit({
                pickupDate,
                preferredTime: preferredTime ? `${preferredTime}:00` : undefined,
                notes: notes || undefined,
              })
            }
          >
            Schedule
          </Button>
        </>
      }
    >
      <div className="grid gap-3">
        <Field label="Pickup date">
          <Input
            type="date"
            className="rounded-xl"
            value={pickupDate}
            onChange={(event) => setPickupDate(event.target.value)}
          />
        </Field>
        <Field label="Preferred time">
          <Input
            type="time"
            className="rounded-xl"
            value={preferredTime}
            onChange={(event) => setPreferredTime(event.target.value)}
          />
        </Field>
        <Field label="Notes">
          <Textarea
            className="rounded-xl"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
          />
        </Field>
      </div>
    </Modal>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
