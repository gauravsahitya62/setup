"use client";

import * as React from "react";
import Link from "next/link";
import { Search } from "lucide-react";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge, type AdminStatusTone } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { VERIFICATION_LABEL, adminErrorMessage } from "@/lib/admin/labels";
import { ROUTES } from "@/constants";
import { ApiClientError, adminApi, type VendorDto } from "@/services";

const TONE: Record<string, AdminStatusTone> = {
  PENDING: "warning",
  UNDER_REVIEW: "info",
  VERIFIED: "success",
  REJECTED: "danger",
  SUSPENDED: "neutral",
};

export function AdminVendorsPage() {
  const [query, setQuery] = React.useState("");
  const [verification, setVerification] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<VendorDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.vendors({
        q: query || undefined,
        verificationStatus: verification || undefined,
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
  }, [query, verification, page]);

  React.useEffect(() => {
    const handle = window.setTimeout(() => void load(), 250);
    return () => window.clearTimeout(handle);
  }, [load]);

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Vendors"
        description="Search, filter, and open a vendor for verification, documents, products, and bookings."
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
            placeholder="Search company, email, phone"
            className="rounded-xl pl-9"
          />
        </div>
        <select
          className="bg-background rounded-xl border px-3 py-2 text-sm"
          value={verification}
          onChange={(e) => {
            setVerification(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All verification</option>
          <option value="PENDING">Pending</option>
          <option value="UNDER_REVIEW">Under review</option>
          <option value="VERIFIED">Verified</option>
          <option value="REJECTED">Rejected</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>
      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load vendors" description={error} /> : null}
      {!loading && !error && rows.length === 0 ? (
        <EmptyState title="No vendors match these filters" />
      ) : null}
      <div className="overflow-x-auto rounded-2xl border">
        <table className="w-full text-sm">
          <thead className="bg-muted/40 text-muted-foreground text-left text-xs tracking-wide uppercase">
            <tr>
              <th className="px-4 py-3">Company</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">GST</th>
              <th className="px-4 py-3">Storefront</th>
              <th className="px-4 py-3">Verification</th>
              <th className="px-4 py-3">Registered</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {rows.map((vendor) => (
              <tr key={vendor.id} className="border-t">
                <td className="px-4 py-3 font-medium">{vendor.companyName}</td>
                <td className="px-4 py-3">{vendor.email ?? "—"}</td>
                <td className="px-4 py-3">{vendor.gstNumber ?? "—"}</td>
                <td className="px-4 py-3">{vendor.status}</td>
                <td className="px-4 py-3">
                  <AdminStatusBadge
                    label={VERIFICATION_LABEL[vendor.verificationStatus ?? "PENDING"] ?? "Pending"}
                    tone={TONE[vendor.verificationStatus ?? "PENDING"]}
                  />
                </td>
                <td className="px-4 py-3">{new Date(vendor.createdAt).toLocaleDateString()}</td>
                <td className="px-4 py-3 text-right">
                  <Button size="sm" className="rounded-lg" asChild>
                    <Link href={ROUTES.admin.vendor(vendor.id)}>Open</Link>
                  </Button>
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
    </div>
  );
}

export { AdminVendorsPage as AdminVendorVerificationPage };
