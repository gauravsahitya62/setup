"use client";

import * as React from "react";
import { Search } from "lucide-react";
import { toast } from "sonner";

import { AdminConfirmDialog } from "@/components/admin/admin-confirm-dialog";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge, type AdminStatusTone } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { PRODUCT_STATUS_LABEL, adminErrorMessage, formatEstimatedValue } from "@/lib/admin/labels";
import { ApiClientError, adminApi, type ProductStatus, type ProductSummaryDto } from "@/services";

const TONE: Record<string, AdminStatusTone> = {
  DRAFT: "neutral",
  PUBLISHED: "success",
  PENDING_REVIEW: "warning",
  REJECTED: "danger",
  SUSPENDED: "danger",
  ARCHIVED: "neutral",
};

type ProductAction = "approve" | "reject" | "suspend" | "restore" | "feature" | "unfeature";

export function AdminProductsPage() {
  const [query, setQuery] = React.useState("");
  const [status, setStatus] = React.useState<ProductStatus | "">("");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<ProductSummaryDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [pending, setPending] = React.useState<ProductSummaryDto | null>(null);
  const [action, setAction] = React.useState<ProductAction | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.products({
        q: query || undefined,
        status: status || undefined,
        page,
        size: 20,
      });
      setRows(result.content);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [query, status, page]);

  React.useEffect(() => {
    const handle = window.setTimeout(() => void load(), 250);
    return () => window.clearTimeout(handle);
  }, [load]);

  async function confirm(reason: string) {
    if (!pending || !action) return;
    try {
      if (action === "approve") await adminApi.approveProduct(pending.id);
      if (action === "reject") await adminApi.rejectProduct(pending.id, reason);
      if (action === "suspend") await adminApi.suspendProduct(pending.id, reason);
      if (action === "restore") await adminApi.restoreProduct(pending.id);
      if (action === "feature") await adminApi.featureProduct(pending.id);
      if (action === "unfeature") await adminApi.unfeatureProduct(pending.id);
      toast.success("Saved");
      setPending(null);
      setAction(null);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Product moderation"
        description="Approve, reject, suspend, restore, or feature listings. Public visibility is enforced on the server."
      />

      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative min-w-0 flex-1 sm:max-w-md">
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
          <Input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            placeholder="Search products or vendors"
            className="rounded-xl pl-9"
          />
        </div>
        <select
          className="bg-background rounded-xl border px-3 py-2 text-sm"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as ProductStatus | "");
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {Object.entries(PRODUCT_STATUS_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load products" description={error} /> : null}
      {!loading && !error && rows.length === 0 ? (
        <EmptyState title="No products match these filters" />
      ) : null}

      <div className="overflow-x-auto rounded-2xl border">
        <table className="w-full min-w-[960px] text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-left text-xs tracking-wide uppercase">
            <tr>
              <th className="px-4 py-3">Product</th>
              <th className="px-4 py-3">Vendor</th>
              <th className="px-4 py-3">Category</th>
              <th className="px-4 py-3">Price/day</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Featured</th>
              <th className="px-4 py-3">Created</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((product) => (
              <tr key={product.id} className="border-t">
                <td className="px-4 py-3">
                  <p className="font-medium">{product.name}</p>
                  {product.moderationReason ? (
                    <p className="text-muted-foreground text-xs">{product.moderationReason}</p>
                  ) : null}
                </td>
                <td className="text-muted-foreground px-4 py-3">{product.vendorName}</td>
                <td className="text-muted-foreground px-4 py-3">{product.categoryName}</td>
                <td className="px-4 py-3 tabular-nums">
                  {formatEstimatedValue(product.pricePerDay)}
                </td>
                <td className="px-4 py-3">
                  <AdminStatusBadge
                    label={PRODUCT_STATUS_LABEL[product.status] ?? product.status}
                    tone={TONE[product.status]}
                  />
                </td>
                <td className="px-4 py-3">{product.featured ? "Yes" : "No"}</td>
                <td className="px-4 py-3">
                  {product.createdAt ? new Date(product.createdAt).toLocaleDateString() : "—"}
                </td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {product.status !== "PUBLISHED" ? (
                      <Button
                        size="sm"
                        className="rounded-lg"
                        onClick={() => {
                          setPending(product);
                          setAction("approve");
                        }}
                      >
                        Approve
                      </Button>
                    ) : null}
                    <Button
                      size="sm"
                      variant="outline"
                      className="rounded-lg"
                      onClick={() => {
                        setPending(product);
                        setAction("reject");
                      }}
                    >
                      Reject
                    </Button>
                    {product.status !== "SUSPENDED" ? (
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-lg"
                        onClick={() => {
                          setPending(product);
                          setAction("suspend");
                        }}
                      >
                        Suspend
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-lg"
                        onClick={() => {
                          setPending(product);
                          setAction("restore");
                        }}
                      >
                        Restore
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="ghost"
                      className="rounded-lg"
                      onClick={() => {
                        setPending(product);
                        setAction(product.featured ? "unfeature" : "feature");
                      }}
                    >
                      {product.featured ? "Unfeature" : "Feature"}
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex justify-end gap-2">
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          className="rounded-xl"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </Button>
      </div>

      <AdminConfirmDialog
        open={Boolean(pending && action)}
        title={
          action === "reject"
            ? "Reject product"
            : action === "suspend"
              ? "Suspend product"
              : action === "restore"
                ? "Restore product"
                : action === "feature"
                  ? "Feature product"
                  : action === "unfeature"
                    ? "Unfeature product"
                    : "Approve product"
        }
        description="This action is recorded in the audit log. Rejecting a product requires a reason."
        confirmLabel="Confirm"
        requireReason={action === "reject" || action === "suspend"}
        onOpenChange={(open) => {
          if (!open) {
            setPending(null);
            setAction(null);
          }
        }}
        onConfirm={confirm}
      />
    </div>
  );
}
