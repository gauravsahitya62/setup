"use client";

import * as React from "react";
import Link from "next/link";
import { ExternalLink, Loader2, Plus, Send, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import { ApiClientError, marketplaceApi, projectApi, type ProjectProductDto } from "@/services";
import { formatCurrency } from "@/lib/utils";

interface ProjectProductsSectionProps {
  projectId: string;
  eventDate?: string;
  marketplaceHref: string;
}

export function ProjectProductsSection({
  projectId,
  eventDate,
  marketplaceHref,
}: ProjectProductsSectionProps) {
  const [items, setItems] = React.useState<ProjectProductDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [checkingId, setCheckingId] = React.useState<string | null>(null);
  const [availability, setAvailability] = React.useState<Record<string, string>>({});

  const reload = React.useCallback(async () => {
    setLoading(true);
    try {
      setItems(await projectApi.products.list(projectId));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load products");
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const updateQty = async (item: ProjectProductDto, quantity: number) => {
    if (quantity < 1) return;
    try {
      const updated = await projectApi.products.update(projectId, item.id, { quantity });
      setItems((prev) => prev.map((row) => (row.id === item.id ? updated : row)));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not update quantity");
    }
  };

  const remove = async (item: ProjectProductDto) => {
    try {
      await projectApi.products.remove(projectId, item.id);
      setItems((prev) => prev.filter((row) => row.id !== item.id));
      toast.success(`Removed ${item.productName}`);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove product");
    }
  };

  const check = async (item: ProjectProductDto) => {
    if (!item.productVariantId) {
      toast.error("This product is no longer listed");
      return;
    }
    const date = eventDate ?? new Date().toISOString().slice(0, 10);
    setCheckingId(item.id);
    try {
      const result = await marketplaceApi.inventory.check(
        item.productVariantId,
        date,
        date,
        item.quantity
      );
      setAvailability((prev) => ({
        ...prev,
        [item.id]: result.available
          ? `${result.availableQuantity} available`
          : result.message || "Unavailable",
      }));
    } catch (error) {
      setAvailability((prev) => ({
        ...prev,
        [item.id]: error instanceof ApiClientError ? error.message : "Could not check availability",
      }));
    } finally {
      setCheckingId(null);
    }
  };

  if (loading) {
    return <p className="text-muted-foreground text-sm">Loading products…</p>;
  }

  if (items.length === 0) {
    return (
      <EmptyState
        title="No products yet"
        description="Browse the marketplace and add variants. This does not reserve inventory."
        actionLabel="Add products"
        onAction={() => {
          window.location.href = marketplaceHref;
        }}
      />
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Products</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            This is a shortlist. To notify a vendor, send a booking request — inventory is reserved
            only after they accept.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" className="rounded-xl" asChild>
            <Link href={marketplaceHref}>
              <Plus className="size-3.5" />
              Add more
            </Link>
          </Button>
          <Button size="sm" className="rounded-xl" asChild>
            <Link href={ROUTES.planner.projectBookingNew(projectId)}>
              <Send className="size-3.5" />
              Request booking
            </Link>
          </Button>
        </div>
      </div>

      <div className="border-primary/20 bg-primary/5 rounded-2xl border px-4 py-3 text-sm">
        <p className="font-medium">Next step: send a booking request</p>
        <p className="text-muted-foreground mt-1">
          Choose the vendor and items, then send. They get a notification and can accept or decline.
          Nothing is reserved until they accept.
        </p>
      </div>

      <div className="border-border/70 overflow-x-auto rounded-2xl border">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-xs">
            <tr>
              <th className="px-4 py-3 font-medium">Product</th>
              <th className="px-4 py-3 font-medium">Vendor</th>
              <th className="px-4 py-3 font-medium">Qty</th>
              <th className="px-4 py-3 font-medium">Price/day</th>
              <th className="px-4 py-3 font-medium">Availability</th>
              <th className="px-4 py-3 font-medium" />
            </tr>
          </thead>
          <tbody className="divide-border/60 divide-y">
            {items.map((item) => (
              <tr key={item.id} className="align-top">
                <td className="px-4 py-3">
                  <div className="flex gap-3">
                    <div className="bg-muted size-14 overflow-hidden rounded-lg">
                      {item.imageUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img src={item.imageUrl} alt="" className="size-full object-cover" />
                      ) : null}
                    </div>
                    <div>
                      <p className="font-medium">{item.productName}</p>
                      <p className="text-muted-foreground text-xs">{item.variantName}</p>
                      {item.deletedFromCatalog ? (
                        <Badge variant="secondary" className="mt-1 rounded-md">
                          No longer listed
                        </Badge>
                      ) : null}
                      {item.notes ? (
                        <p className="text-muted-foreground mt-1 text-xs">{item.notes}</p>
                      ) : null}
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <Link href={`/vendors/${item.vendorSlug}`} className="hover:underline">
                    {item.vendorName}
                    {item.vendorVerified ? " · Verified" : ""}
                  </Link>
                  {item.listed && item.productVariantId ? (
                    <p className="mt-1">
                      <Link
                        href={`${ROUTES.planner.projectBookingNew(projectId)}?vendor=${item.vendorId}`}
                        className="text-foreground text-xs font-medium underline-offset-4 hover:underline"
                      >
                        Request booking
                      </Link>
                    </p>
                  ) : null}
                </td>
                <td className="px-4 py-3">
                  <Input
                    type="number"
                    min={1}
                    className="h-9 w-20 rounded-lg"
                    defaultValue={item.quantity}
                    onBlur={(event) => void updateQty(item, Number(event.target.value))}
                  />
                </td>
                <td className="px-4 py-3">
                  <p>{formatCurrency(Number(item.snapshotPricePerDay), item.currency)}</p>
                  {item.currentPricePerDay != null &&
                  Number(item.currentPricePerDay) !== Number(item.snapshotPricePerDay) ? (
                    <p className="text-muted-foreground text-xs">
                      Now {formatCurrency(Number(item.currentPricePerDay), item.currency)}
                    </p>
                  ) : null}
                </td>
                <td className="px-4 py-3">
                  <p className="text-muted-foreground text-xs">
                    {availability[item.id] ?? "Not checked"}
                  </p>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="mt-1 h-7 rounded-lg px-2"
                    disabled={checkingId === item.id}
                    onClick={() => void check(item)}
                  >
                    {checkingId === item.id ? <Loader2 className="size-3 animate-spin" /> : null}
                    Check
                  </Button>
                </td>
                <td className="px-4 py-3 text-right">
                  {item.productSlug ? (
                    <Button variant="ghost" size="icon-sm" className="rounded-lg" asChild>
                      <Link href={ROUTES.product(item.productSlug)}>
                        <ExternalLink className="size-3.5" />
                      </Link>
                    </Button>
                  ) : null}
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    className="text-destructive rounded-lg"
                    onClick={() => void remove(item)}
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
