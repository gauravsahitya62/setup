"use client";

import * as React from "react";
import Link from "next/link";
import { toast } from "sonner";
import { AlertTriangle, Boxes, CalendarDays, Package, Search, Wrench } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { InventoryStatusBadge } from "@/components/vendor-dashboard/inventory/inventory-status-badge";
import { ROUTES } from "@/constants";
import { useMyVendor } from "@/hooks/use-my-vendor";
import {
  ApiClientError,
  marketplaceApi,
  type VendorInventoryItemDto,
  type VendorInventoryOverviewDto,
} from "@/services";
import { cn } from "@/lib/utils";

function Kpi({
  label,
  value,
  hint,
  tone,
}: {
  label: string;
  value: number;
  hint?: string;
  tone?: "warn";
}) {
  return (
    <div className="border-border/70 bg-card rounded-2xl border p-4 shadow-sm">
      <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">{label}</p>
      <p
        className={cn(
          "mt-1 text-2xl font-semibold tracking-tight tabular-nums",
          tone === "warn" && "text-amber-700 dark:text-amber-300"
        )}
      >
        {value}
      </p>
      {hint ? <p className="text-muted-foreground mt-1 text-xs">{hint}</p> : null}
    </div>
  );
}

export function VendorStockOverviewPage() {
  const { vendor, loading: vendorLoading, missing } = useMyVendor();
  const [data, setData] = React.useState<VendorInventoryOverviewDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [query, setQuery] = React.useState("");

  const reload = React.useCallback(async () => {
    if (!vendor) {
      setData(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setData(await marketplaceApi.vendorInventory.overview());
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load inventory");
    } finally {
      setLoading(false);
    }
  }, [vendor]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const filtered = React.useMemo(() => {
    const q = query.trim().toLowerCase();
    const items = data?.items ?? [];
    if (!q) return items;
    return items.filter(
      (item) =>
        item.productName.toLowerCase().includes(q) ||
        item.variantName.toLowerCase().includes(q) ||
        item.sku.toLowerCase().includes(q)
    );
  }, [data, query]);

  if (!vendorLoading && (missing || !vendor)) {
    return (
      <EmptyState
        title="Complete your vendor profile first"
        description="You need a storefront before you can manage rental inventory."
        actionLabel="Set up storefront"
        onAction={() => {
          window.location.href = ROUTES.vendor.profileWizard;
        }}
      />
    );
  }

  const busy = vendorLoading || loading;

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Inventory</h1>
          <p className="text-muted-foreground text-sm">
            Quantity and date-based availability for every variant. Available units are calculated,
            not typed in.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.availability}>
            <CalendarDays className="size-4" />
            Availability calendar
          </Link>
        </Button>
      </section>

      {busy || !data ? (
        <SkeletonLoader variant="list" count={6} />
      ) : (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
            <Kpi label="Total products" value={data.totalProducts} />
            <Kpi label="Total variants" value={data.totalVariants} />
            <Kpi label="Total units" value={data.totalUnits} />
            <Kpi label="Reserved" value={data.reservedUnits} />
            <Kpi label="Available" value={data.availableUnits} />
            <Kpi label="Maintenance" value={data.maintenanceUnits} />
            <Kpi
              label="Low availability"
              value={data.lowAvailabilityCount}
              tone={data.lowAvailabilityCount > 0 ? "warn" : undefined}
              hint="At or below 20% of total"
            />
          </div>

          <div className="relative">
            <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search product, variant, or SKU…"
              className="rounded-xl pl-9"
            />
          </div>

          <div className="border-border/70 bg-card overflow-hidden rounded-2xl border shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[960px] text-left text-sm">
                <thead className="border-border/70 bg-muted/30 text-muted-foreground border-b text-xs font-medium tracking-wide uppercase">
                  <tr>
                    <th className="px-4 py-3">Product</th>
                    <th className="px-4 py-3">Variant</th>
                    <th className="px-4 py-3">SKU</th>
                    <th className="px-4 py-3 text-right">Total</th>
                    <th className="px-4 py-3 text-right">Reserved</th>
                    <th className="px-4 py-3 text-right">Available</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((item) => (
                    <InventoryRow key={item.variantId} item={item} />
                  ))}
                  {filtered.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-muted-foreground px-4 py-16 text-center">
                        {data.items.length === 0
                          ? "No variants yet. Add products in Catalog to stock inventory."
                          : "No variants match your search."}
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function InventoryRow({ item }: { item: VendorInventoryItemDto }) {
  return (
    <tr className="border-border/50 hover:bg-muted/25 border-b last:border-0">
      <td className="px-4 py-3 align-middle">
        <div className="flex items-center gap-3">
          <div className="bg-muted ring-border/60 size-12 shrink-0 overflow-hidden rounded-lg ring-1">
            {item.featuredImage ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={item.featuredImage} alt="" className="size-full object-cover" />
            ) : (
              <div className="text-muted-foreground flex size-full items-center justify-center">
                <Package className="size-4" />
              </div>
            )}
          </div>
          <div className="min-w-0">
            <p className="truncate font-medium">{item.productName}</p>
            {item.lowStock ? (
              <p className="flex items-center gap-1 text-xs text-amber-700 dark:text-amber-300">
                <AlertTriangle className="size-3" />
                Low availability
              </p>
            ) : (
              <p className="text-muted-foreground truncate text-xs">{item.productSlug}</p>
            )}
          </div>
        </div>
      </td>
      <td className="px-4 py-3 align-middle">
        {item.variantName}
        {item.color ? (
          <span className="text-muted-foreground block text-xs">{item.color}</span>
        ) : null}
      </td>
      <td className="px-4 py-3 align-middle font-mono text-xs">{item.sku}</td>
      <td className="px-4 py-3 text-right align-middle tabular-nums">{item.totalQuantity}</td>
      <td className="px-4 py-3 text-right align-middle tabular-nums">{item.reservedQuantity}</td>
      <td className="px-4 py-3 text-right align-middle font-medium tabular-nums">
        {item.availableQuantity}
      </td>
      <td className="px-4 py-3 align-middle">
        <InventoryStatusBadge status={item.status} />
      </td>
      <td className="px-4 py-3 align-middle">
        <div className="flex justify-end gap-2">
          <Button variant="outline" size="sm" className="rounded-lg" asChild>
            <Link href={ROUTES.vendor.inventoryDetail(item.variantId)}>
              <Boxes className="size-3.5" />
              Manage
            </Link>
          </Button>
          <Button variant="ghost" size="sm" className="rounded-lg" asChild>
            <Link href={`${ROUTES.vendor.availability}?variant=${item.variantId}`}>
              <Wrench className="size-3.5" />
              Calendar
            </Link>
          </Button>
        </div>
      </td>
    </tr>
  );
}
