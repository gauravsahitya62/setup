"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeft, Loader2, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { InventoryStatusBadge } from "@/components/vendor-dashboard/inventory/inventory-status-badge";
import { ROUTES } from "@/constants";
import {
  ApiClientError,
  marketplaceApi,
  type CalendarDayDto,
  type VendorInventoryDetailDto,
} from "@/services";

export function VendorStockDetailPage({ variantId }: { variantId: string }) {
  const router = useRouter();
  const [detail, setDetail] = React.useState<VendorInventoryDetailDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [tab, setTab] = React.useState("overview");

  const reload = React.useCallback(async () => {
    setLoading(true);
    try {
      setDetail(await marketplaceApi.vendorInventory.get(variantId));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load inventory");
      if (error instanceof ApiClientError && error.status === 404) {
        router.push(ROUTES.vendor.inventory);
      }
    } finally {
      setLoading(false);
    }
  }, [variantId, router]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  if (loading && !detail) {
    return <SkeletonLoader variant="list" count={6} />;
  }
  if (!detail) {
    return (
      <EmptyState
        title="Inventory not found"
        description="This variant has no inventory row."
        actionLabel="Back to inventory"
        onAction={() => router.push(ROUTES.vendor.inventory)}
      />
    );
  }

  const image = detail.imageUrls[0] ?? detail.featuredImage;

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" className="rounded-lg" asChild>
        <Link href={ROUTES.vendor.inventory}>
          <ArrowLeft className="size-4" />
          Inventory
        </Link>
      </Button>

      <section className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <div className="bg-muted ring-border/60 size-24 shrink-0 overflow-hidden rounded-2xl ring-1">
          {image ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={image} alt="" className="size-full object-cover" />
          ) : null}
        </div>
        <div className="min-w-0 flex-1 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{detail.productName}</h1>
            <InventoryStatusBadge status={detail.status} />
          </div>
          <p className="text-muted-foreground text-sm">
            {detail.variantName}
            {detail.color ? ` · ${detail.color}` : ""}
            {detail.material ? ` · ${detail.material}` : ""}
            {" · SKU "}
            <span className="font-mono">{detail.sku}</span>
          </p>
          {detail.lowStock ? (
            <p className="text-sm text-amber-700 dark:text-amber-300">Low availability</p>
          ) : null}
        </div>
      </section>

      <div className="grid gap-3 sm:grid-cols-5">
        <Stat label="Total" value={detail.totalQuantity} />
        <Stat label="Reserved today" value={detail.reservedQuantity} />
        <Stat label="Available today" value={detail.availableQuantity} />
        <Stat label="Maintenance" value={detail.maintenanceQuantity} />
        <Stat label="Damaged" value={detail.damagedQuantity} />
      </div>

      <Tabs value={tab} onValueChange={setTab} className="gap-4">
        <TabsList className="bg-muted/50 h-auto w-full flex-wrap justify-start gap-1 rounded-xl p-1 sm:w-auto">
          <TabsTrigger value="overview" className="rounded-lg">
            Overview
          </TabsTrigger>
          <TabsTrigger value="calendar" className="rounded-lg">
            Calendar
          </TabsTrigger>
          <TabsTrigger value="adjust" className="rounded-lg">
            Adjustments
          </TabsTrigger>
          <TabsTrigger value="maintenance" className="rounded-lg">
            Maintenance
          </TabsTrigger>
          <TabsTrigger value="history" className="rounded-lg">
            History
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <ReservationsList detail={detail} />
        </TabsContent>
        <TabsContent value="calendar">
          <VariantCalendar variantId={variantId} productName={detail.productName} />
        </TabsContent>
        <TabsContent value="adjust">
          <AdjustForm variantId={variantId} currentTotal={detail.totalQuantity} onDone={reload} />
        </TabsContent>
        <TabsContent value="maintenance">
          <MaintenancePanel variantId={variantId} detail={detail} onDone={reload} />
        </TabsContent>
        <TabsContent value="history">
          <HistoryList variantId={variantId} />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="border-border/70 bg-card rounded-2xl border p-4 shadow-sm">
      <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
    </div>
  );
}

function ReservationsList({ detail }: { detail: VendorInventoryDetailDto }) {
  if (detail.upcomingReservations.length === 0) {
    return (
      <EmptyState
        title="No upcoming reservations"
        description="Confirmed bookings for this variant will appear here."
      />
    );
  }
  return (
    <div className="border-border/70 bg-card overflow-hidden rounded-2xl border shadow-sm">
      <table className="w-full text-left text-sm">
        <thead className="border-border/70 bg-muted/30 text-muted-foreground border-b text-xs font-medium tracking-wide uppercase">
          <tr>
            <th className="px-4 py-3">Dates</th>
            <th className="px-4 py-3 text-right">Qty</th>
            <th className="px-4 py-3">Status</th>
          </tr>
        </thead>
        <tbody>
          {detail.upcomingReservations.map((row) => (
            <tr key={row.id} className="border-border/50 border-b last:border-0">
              <td className="px-4 py-3">
                {row.startDate} → {row.endDate}
              </td>
              <td className="px-4 py-3 text-right tabular-nums">{row.quantity}</td>
              <td className="px-4 py-3">{row.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AdjustForm({
  variantId,
  currentTotal,
  onDone,
}: {
  variantId: string;
  currentTotal: number;
  onDone: () => Promise<void>;
}) {
  const [delta, setDelta] = React.useState("20");
  const [reason, setReason] = React.useState("");
  const [saving, setSaving] = React.useState(false);
  const parsed = Number(delta);
  const nextTotal = Number.isFinite(parsed) ? currentTotal + parsed : currentTotal;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!reason.trim()) {
      toast.error("A reason is required for every adjustment");
      return;
    }
    if (!Number.isFinite(parsed) || parsed === 0) {
      toast.error("Enter a non-zero adjustment");
      return;
    }
    setSaving(true);
    try {
      await marketplaceApi.vendorInventory.adjust(variantId, {
        delta: parsed,
        reason: reason.trim(),
      });
      toast.success(`Stock updated to ${currentTotal + parsed}`);
      setReason("");
      await onDone();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not adjust inventory");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={(e) => void submit(e)}
      className="border-border/70 bg-card max-w-lg space-y-4 rounded-2xl border p-5 shadow-sm"
    >
      <p className="text-muted-foreground text-sm">
        Current total:{" "}
        <span className="text-foreground font-medium tabular-nums">{currentTotal}</span>. New total:{" "}
        <span className="text-foreground font-medium tabular-nums">{nextTotal}</span>
      </p>
      <div className="space-y-2">
        <Label htmlFor="delta">Adjustment</Label>
        <Input
          id="delta"
          type="number"
          value={delta}
          onChange={(e) => setDelta(e.target.value)}
          className="rounded-xl"
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="reason">Reason</Label>
        <Textarea
          id="reason"
          required
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Purchased additional chairs"
          className="rounded-xl"
        />
      </div>
      <Button type="submit" className="rounded-xl" disabled={saving}>
        {saving ? <Loader2 className="size-4 animate-spin" /> : null}
        Apply adjustment
      </Button>
    </form>
  );
}

function MaintenancePanel({
  variantId,
  detail,
  onDone,
}: {
  variantId: string;
  detail: VendorInventoryDetailDto;
  onDone: () => Promise<void>;
}) {
  const [quantity, setQuantity] = React.useState("1");
  const [startDate, setStartDate] = React.useState("");
  const [endDate, setEndDate] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [saving, setSaving] = React.useState(false);
  const [deleting, setDeleting] = React.useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await marketplaceApi.vendorInventory.createMaintenance(variantId, {
        quantity: Number(quantity),
        startDate,
        endDate,
        reason: reason.trim(),
      });
      toast.success("Maintenance block created");
      setReason("");
      await onDone();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not create maintenance");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string) {
    setDeleting(id);
    try {
      await marketplaceApi.vendorInventory.deleteMaintenance(variantId, id);
      toast.success("Maintenance removed");
      await onDone();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove maintenance");
    } finally {
      setDeleting(null);
    }
  }

  const active = detail.maintenanceRecords.filter((row) => row.status === "ACTIVE");
  const history = detail.maintenanceRecords.filter((row) => row.status !== "ACTIVE");

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <form
        onSubmit={(e) => void submit(e)}
        className="border-border/70 bg-card space-y-4 rounded-2xl border p-5 shadow-sm"
      >
        <h2 className="font-semibold">Create maintenance block</h2>
        <div className="space-y-2">
          <Label htmlFor="mqty">Quantity</Label>
          <Input
            id="mqty"
            type="number"
            min={1}
            required
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            className="rounded-xl"
          />
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="mstart">Start date</Label>
            <Input
              id="mstart"
              type="date"
              required
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="rounded-xl"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="mend">End date</Label>
            <Input
              id="mend"
              type="date"
              required
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="rounded-xl"
            />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="mreason">Reason</Label>
          <Textarea
            id="mreason"
            required
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="rounded-xl"
          />
        </div>
        <Button type="submit" className="rounded-xl" disabled={saving}>
          {saving ? <Loader2 className="size-4 animate-spin" /> : null}
          Block for maintenance
        </Button>
      </form>

      <div className="space-y-3">
        <h2 className="font-semibold">Active blocks</h2>
        {active.length === 0 ? (
          <p className="text-muted-foreground text-sm">No active maintenance windows.</p>
        ) : (
          active.map((row) => (
            <div
              key={row.id}
              className="border-border/70 bg-card flex items-start justify-between gap-3 rounded-2xl border p-4"
            >
              <div>
                <p className="font-medium tabular-nums">
                  {row.quantity} units · {row.startDate} → {row.endDate}
                </p>
                <p className="text-muted-foreground text-sm">{row.reason}</p>
              </div>
              <Button
                variant="ghost"
                size="icon-sm"
                className="rounded-lg"
                onClick={() => void remove(row.id)}
                disabled={deleting === row.id}
                aria-label="Remove maintenance"
              >
                {deleting === row.id ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Trash2 className="size-4" />
                )}
              </Button>
            </div>
          ))
        )}
        {history.length > 0 ? (
          <div className="pt-2">
            <h3 className="text-muted-foreground mb-2 text-sm font-medium">History</h3>
            <ul className="text-muted-foreground space-y-2 text-sm">
              {history.map((row) => (
                <li key={row.id}>
                  {row.startDate} → {row.endDate} · {row.quantity} · {row.reason} ({row.status})
                </li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>
    </div>
  );
}

function HistoryList({ variantId }: { variantId: string }) {
  const [rows, setRows] = React.useState<
    Awaited<ReturnType<typeof marketplaceApi.vendorInventory.movements>>
  >([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    let cancelled = false;
    marketplaceApi.vendorInventory
      .movements(variantId, 100)
      .then((data) => {
        if (!cancelled) setRows(data);
      })
      .catch((error) => {
        toast.error(error instanceof ApiClientError ? error.message : "Could not load history");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [variantId]);

  if (loading) return <SkeletonLoader variant="list" count={4} />;
  if (rows.length === 0) {
    return (
      <EmptyState
        title="No movements yet"
        description="Adjustments and maintenance will be recorded here."
      />
    );
  }

  return (
    <div className="border-border/70 bg-card overflow-hidden rounded-2xl border shadow-sm">
      <table className="w-full text-left text-sm">
        <thead className="border-border/70 bg-muted/30 text-muted-foreground border-b text-xs font-medium tracking-wide uppercase">
          <tr>
            <th className="px-4 py-3">When</th>
            <th className="px-4 py-3">Type</th>
            <th className="px-4 py-3 text-right">Qty</th>
            <th className="px-4 py-3">Reason</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className="border-border/50 border-b last:border-0">
              <td className="text-muted-foreground px-4 py-3">
                {new Date(row.createdAt).toLocaleString()}
              </td>
              <td className="px-4 py-3">{row.movementType.replaceAll("_", " ")}</td>
              <td className="px-4 py-3 text-right tabular-nums">{row.quantity}</td>
              <td className="px-4 py-3">{row.reason}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function VariantCalendar({ variantId, productName }: { variantId: string; productName: string }) {
  const [month, setMonth] = React.useState(() => new Date());
  const [days, setDays] = React.useState<CalendarDayDto[]>([]);
  const [selected, setSelected] = React.useState<CalendarDayDto | null>(null);
  const [loading, setLoading] = React.useState(true);

  const start = new Date(month.getFullYear(), month.getMonth(), 1);
  const end = new Date(month.getFullYear(), month.getMonth() + 1, 0);

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const pad = (n: number) => String(n).padStart(2, "0");
    const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    marketplaceApi.vendorInventory
      .calendar({ variant: variantId, startDate: iso(start), endDate: iso(end) })
      .then((data) => {
        if (!cancelled) setDays(data);
      })
      .catch((error) => {
        toast.error(error instanceof ApiClientError ? error.message : "Could not load calendar");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reload when month changes
  }, [variantId, month.getFullYear(), month.getMonth()]);

  const byDate = React.useMemo(() => new Map(days.map((d) => [d.date, d])), [days]);
  const blanks = Array.from({ length: start.getDay() });
  const weekday = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">
          {month.toLocaleString(undefined, { month: "long", year: "numeric" })} · {productName}
        </h2>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            className="rounded-lg"
            onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}
          >
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="rounded-lg"
            onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}
          >
            Next
          </Button>
        </div>
      </div>
      {loading ? (
        <SkeletonLoader variant="list" count={2} />
      ) : (
        <div className="border-border/70 bg-card rounded-2xl border p-4 shadow-sm">
          <div className="text-muted-foreground grid grid-cols-7 gap-1 text-center text-[11px] font-medium">
            {weekday.map((d) => (
              <span key={d}>{d}</span>
            ))}
          </div>
          <div className="mt-2 grid grid-cols-7 gap-1">
            {blanks.map((_, i) => (
              <div key={`b-${i}`} className="aspect-square" />
            ))}
            {Array.from({ length: end.getDate() }, (_, i) => i + 1).map((n) => {
              const pad = (v: number) => String(v).padStart(2, "0");
              const key = `${month.getFullYear()}-${pad(month.getMonth() + 1)}-${pad(n)}`;
              const day = byDate.get(key);
              const available = day?.availableQuantity ?? 0;
              const total = day?.totalQuantity ?? 0;
              const label =
                !day || total === 0
                  ? "No stock"
                  : available <= 0
                    ? "Unavailable"
                    : available < total
                      ? `${available} available`
                      : `${available} available`;
              return (
                <button
                  key={key}
                  type="button"
                  title={label}
                  onClick={() => setSelected(day ?? null)}
                  className="ring-border/70 hover:bg-muted/40 flex aspect-square flex-col items-center justify-center rounded-lg text-xs ring-1 ring-inset"
                >
                  <span className="font-medium tabular-nums">{n}</span>
                  <span className="text-muted-foreground text-[10px] tabular-nums">
                    {available}
                  </span>
                </button>
              );
            })}
          </div>
          <p className="text-muted-foreground mt-3 text-xs">
            Each cell shows the available quantity. Click a date to see reservations.
          </p>
        </div>
      )}
      {selected ? (
        <div className="border-border/70 bg-card rounded-2xl border p-4 text-sm shadow-sm">
          <p className="font-medium">{selected.date}</p>
          <p className="text-muted-foreground mt-1">
            Total {selected.totalQuantity} · Reserved {selected.reservedQuantity} · Maintenance{" "}
            {selected.maintenanceQuantity} · Damaged {selected.damagedQuantity} · Blocked{" "}
            {selected.blockedQuantity} · Available {selected.availableQuantity}
          </p>
        </div>
      ) : null}
    </div>
  );
}
