"use client";

import * as React from "react";
import Link from "next/link";
import { toast } from "sonner";
import {
  Archive,
  CheckCircle2,
  ExternalLink,
  FileEdit,
  Loader2,
  MoreHorizontal,
  Plus,
  Search,
  SlidersHorizontal,
  Trash2,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { PRODUCT_STATUS_LABEL } from "@/lib/admin/labels";
import { useMyVendor } from "@/hooks/use-my-vendor";
import {
  ApiClientError,
  marketplaceApi,
  type ProductStatus,
  type ProductSummaryDto,
} from "@/services";
import { cn, formatCurrency } from "@/lib/utils";

type StatusFilter = "all" | ProductStatus;

function StatusBadge({ status }: { status: ProductStatus }) {
  return (
    <Badge
      variant="secondary"
      className={cn(
        "rounded-md capitalize",
        status === "PUBLISHED" && "bg-emerald-500/12 text-emerald-800 dark:text-emerald-300",
        status === "DRAFT" && "bg-amber-500/12 text-amber-900 dark:text-amber-200",
        (status === "ARCHIVED" || status === "SUSPENDED" || status === "REJECTED") &&
          "bg-muted text-muted-foreground",
        status === "PENDING_REVIEW" && "bg-amber-500/12 text-amber-900 dark:text-amber-200"
      )}
    >
      {PRODUCT_STATUS_LABEL[status] ?? status}
    </Badge>
  );
}

export function VendorInventoryPage() {
  const { vendor, loading: vendorLoading, missing } = useMyVendor();
  const [products, setProducts] = React.useState<ProductSummaryDto[]>([]);
  const [loadingProducts, setLoadingProducts] = React.useState(true);
  const [deletingId, setDeletingId] = React.useState<string | null>(null);
  const [query, setQuery] = React.useState("");
  const [status, setStatus] = React.useState<StatusFilter>("all");
  const [category, setCategory] = React.useState<string>("all");

  const reload = React.useCallback(async () => {
    if (!vendor) {
      setProducts([]);
      setLoadingProducts(false);
      return;
    }
    setLoadingProducts(true);
    try {
      const page = await marketplaceApi.vendorProducts.list(vendor.id, 0, 100);
      setProducts(page.content);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load inventory");
    } finally {
      setLoadingProducts(false);
    }
  }, [vendor]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const categories = React.useMemo(() => {
    const set = new Set(products.map((item) => item.categoryName));
    return Array.from(set).sort();
  }, [products]);

  const filtered = React.useMemo(() => {
    const q = query.trim().toLowerCase();
    return products.filter((item) => {
      if (status !== "all" && item.status !== status) return false;
      if (category !== "all" && item.categoryName !== category) return false;
      if (!q) return true;
      return (
        item.name.toLowerCase().includes(q) ||
        item.slug.toLowerCase().includes(q) ||
        item.categoryName.toLowerCase().includes(q)
      );
    });
  }, [products, query, status, category]);

  const summary = React.useMemo(
    () => ({
      published: products.filter((p) => p.status === "PUBLISHED").length,
      draft: products.filter((p) => p.status === "DRAFT").length,
      archived: products.filter((p) => p.status === "ARCHIVED").length,
      featured: products.filter((p) => p.featured).length,
    }),
    [products]
  );

  async function handleDelete(product: ProductSummaryDto) {
    if (
      typeof window !== "undefined" &&
      !window.confirm(`Delete "${product.name}"? This can't be undone.`)
    ) {
      return;
    }
    setDeletingId(product.id);
    try {
      await marketplaceApi.products.remove(product.id);
      toast.success("Product deleted");
      await reload();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not delete product");
    } finally {
      setDeletingId(null);
    }
  }

  const loading = vendorLoading || loadingProducts;

  if (!vendorLoading && (missing || !vendor)) {
    return (
      <EmptyState
        title="Complete your vendor profile first"
        description="You need a storefront before you can add products to your catalog."
        actionLabel="Set up storefront"
        onAction={() => {
          window.location.href = ROUTES.vendor.profileWizard;
        }}
      />
    );
  }

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Inventory</h1>
          <p className="text-muted-foreground text-sm">
            {summary.published} published · {summary.draft} drafts · {summary.archived} archived ·{" "}
            {summary.featured} featured
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button className="rounded-xl" asChild>
            <Link href={ROUTES.vendor.productWizard}>
              <Plus className="size-4" />
              Add product
            </Link>
          </Button>
        </div>
      </section>

      <Tabs
        value={status}
        onValueChange={(value) => setStatus(value as StatusFilter)}
        className="gap-4"
      >
        <TabsList className="bg-muted/50 h-auto w-full flex-wrap justify-start gap-1 rounded-xl p-1 sm:w-auto">
          <TabsTrigger value="all" className="rounded-lg">
            All
          </TabsTrigger>
          <TabsTrigger value="PUBLISHED" className="gap-1.5 rounded-lg">
            <CheckCircle2 className="size-3.5" />
            Published
          </TabsTrigger>
          <TabsTrigger value="DRAFT" className="gap-1.5 rounded-lg">
            <FileEdit className="size-3.5" />
            Draft
          </TabsTrigger>
          <TabsTrigger value="ARCHIVED" className="gap-1.5 rounded-lg">
            <Archive className="size-3.5" />
            Archived
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="border-border/70 bg-card flex flex-col gap-3 rounded-2xl border p-3 shadow-sm sm:flex-row sm:items-center">
        <div className="relative min-w-0 flex-1">
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search products, slugs, categories…"
            className="rounded-xl pl-9"
          />
        </div>
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger className="w-full rounded-xl sm:w-48">
            <SlidersHorizontal className="text-muted-foreground size-4" />
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            {categories.map((name) => (
              <SelectItem key={name} value={name}>
                {name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {loading ? (
        <SkeletonLoader variant="list" count={5} />
      ) : (
        <div className="border-border/70 bg-card overflow-hidden rounded-2xl border shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[880px] text-left text-sm">
              <thead className="border-border/70 bg-muted/30 text-muted-foreground border-b text-xs font-medium tracking-wide uppercase">
                <tr>
                  <th className="px-4 py-3">Product</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Price</th>
                  <th className="px-4 py-3">Featured</th>
                  <th className="w-12 px-2 py-3" />
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr
                    key={item.id}
                    className="border-border/50 hover:bg-muted/25 border-b last:border-0"
                  >
                    <td className="px-4 py-3 align-middle">
                      <div className="flex items-center gap-3">
                        <div className="bg-muted ring-border/60 size-12 shrink-0 overflow-hidden rounded-lg ring-1">
                          {item.featuredImage ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img
                              src={item.featuredImage}
                              alt=""
                              className="size-full object-cover"
                            />
                          ) : null}
                        </div>
                        <div className="min-w-0">
                          <Link
                            href={ROUTES.vendor.productEdit(item.id)}
                            className="hover:text-foreground truncate font-medium hover:underline"
                          >
                            {item.name}
                          </Link>
                          <p className="text-muted-foreground truncate text-xs">{item.slug}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 align-middle">
                      <StatusBadge status={item.status} />
                    </td>
                    <td className="text-muted-foreground px-4 py-3 align-middle">
                      {item.categoryName}
                    </td>
                    <td className="px-4 py-3 align-middle">
                      <p className="font-medium tabular-nums">
                        {formatCurrency(Number(item.pricePerDay))}
                      </p>
                      <p className="text-muted-foreground text-xs">per day</p>
                    </td>
                    <td className="px-4 py-3 align-middle">
                      {item.featured ? (
                        <Badge variant="outline" className="rounded-md">
                          Featured
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </td>
                    <td className="px-2 py-3 align-middle">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="rounded-lg"
                            aria-label="Actions"
                            disabled={deletingId === item.id}
                          >
                            {deletingId === item.id ? (
                              <Loader2 className="size-4 animate-spin" />
                            ) : (
                              <MoreHorizontal className="size-4" />
                            )}
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem asChild>
                            <Link href={ROUTES.vendor.productEdit(item.id)}>
                              <FileEdit className="size-3.5" />
                              Edit
                            </Link>
                          </DropdownMenuItem>
                          {item.status === "PUBLISHED" ? (
                            <DropdownMenuItem asChild>
                              <Link href={ROUTES.product(item.slug)} target="_blank">
                                <ExternalLink className="size-3.5" />
                                View listing
                              </Link>
                            </DropdownMenuItem>
                          ) : null}
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            variant="destructive"
                            onClick={() => void handleDelete(item)}
                          >
                            <Trash2 className="size-3.5" />
                            Delete
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </td>
                  </tr>
                ))}
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-muted-foreground px-4 py-16 text-center">
                      {products.length === 0
                        ? "No products yet. Add your first product to start renting."
                        : "No products match your filters."}
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
          <div className="border-border/70 text-muted-foreground flex items-center justify-between border-t px-4 py-3 text-xs">
            <span>
              Showing {filtered.length} of {products.length} products
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
