"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { toast } from "sonner";

import { AdminConfirmDialog } from "@/components/admin/admin-confirm-dialog";
import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge, type AdminStatusTone } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import {
  PRODUCT_STATUS_LABEL,
  VERIFICATION_LABEL,
  adminErrorMessage,
  formatEstimatedValue,
} from "@/lib/admin/labels";
import { ApiClientError, adminApi, type AdminVendorDetailDto } from "@/services";

const TONE: Record<string, AdminStatusTone> = {
  PENDING: "warning",
  UNDER_REVIEW: "info",
  VERIFIED: "success",
  REJECTED: "danger",
  SUSPENDED: "neutral",
};

type VendorAction = "start" | "verify" | "reject" | "suspend" | "reactivate";
type DocumentAction = "approve" | "reject" | "resubmit";

export function AdminVendorDetailPage() {
  const params = useParams<{ id: string }>();
  const vendorId = params.id;
  const [data, setData] = React.useState<AdminVendorDetailDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [action, setAction] = React.useState<VendorAction | null>(null);
  const [documentId, setDocumentId] = React.useState<string | null>(null);
  const [documentAction, setDocumentAction] = React.useState<DocumentAction | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await adminApi.vendor(vendorId));
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [vendorId]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const vendor = data?.vendor;
  const verification = vendor?.verificationStatus ?? "PENDING";

  async function confirmVendor(reason: string) {
    if (!vendor || !action) return;
    try {
      if (action === "start") await adminApi.startVendorReview(vendor.id);
      if (action === "verify") await adminApi.verifyVendor(vendor.id);
      if (action === "reject") await adminApi.rejectVendor(vendor.id, reason);
      if (action === "suspend") await adminApi.suspendVendor(vendor.id, reason);
      if (action === "reactivate") await adminApi.reactivateVendor(vendor.id);
      toast.success("Vendor updated");
      setAction(null);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function confirmDocument(reason: string) {
    if (!vendor || !documentId || !documentAction) return;
    try {
      if (documentAction === "approve") await adminApi.approveDocument(vendor.id, documentId);
      if (documentAction === "reject") await adminApi.rejectDocument(vendor.id, documentId, reason);
      if (documentAction === "resubmit")
        await adminApi.requestDocumentResubmission(vendor.id, documentId, reason);
      toast.success("Document updated");
      setDocumentId(null);
      setDocumentAction(null);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function toggleFeatured() {
    if (!vendor) return;
    try {
      await adminApi.featureVendor(vendor.id, !vendor.featured);
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  if (loading) return <SkeletonLoader className="h-64 rounded-2xl" />;
  if (error || !vendor || !data)
    return <EmptyState title="Vendor not found" description={error ?? undefined} />;

  return (
    <div className="space-y-8">
      <AdminPageHeader
        title={vendor.companyName}
        description="Verification, documents, products, reviews, and booking history."
        actions={
          <div className="flex flex-wrap gap-2">
            {verification === "PENDING" || verification === "REJECTED" ? (
              <Button className="rounded-xl" onClick={() => setAction("start")}>
                Start review
              </Button>
            ) : null}
            {verification !== "VERIFIED" ? (
              <Button className="rounded-xl" onClick={() => setAction("verify")}>
                Verify
              </Button>
            ) : null}
            <Button variant="outline" className="rounded-xl" onClick={() => setAction("reject")}>
              Reject
            </Button>
            {verification !== "SUSPENDED" ? (
              <Button variant="outline" className="rounded-xl" onClick={() => setAction("suspend")}>
                Suspend
              </Button>
            ) : (
              <Button
                variant="outline"
                className="rounded-xl"
                onClick={() => setAction("reactivate")}
              >
                Reactivate
              </Button>
            )}
          </div>
        }
      />

      <section className="grid gap-4 rounded-2xl border p-5 sm:grid-cols-2 lg:grid-cols-3">
        <Fact label="Owner" value={data.ownerName ?? "—"} />
        <Fact label="Email" value={data.ownerEmail ?? vendor.email ?? "—"} />
        <Fact label="Phone" value={data.ownerPhone ?? vendor.phone ?? "—"} />
        <Fact label="GST number" value={vendor.gstNumber ?? "—"} />
        <Fact
          label="Established year"
          value={vendor.establishedYear ? String(vendor.establishedYear) : "—"}
        />
        <Fact label="Registration date" value={new Date(vendor.createdAt).toLocaleDateString()} />
        <div className="sm:col-span-2">
          <p className="text-muted-foreground text-xs">Service areas</p>
          <p className="text-sm">
            {vendor.serviceAreas.length
              ? vendor.serviceAreas.map((area) => `${area.city}, ${area.state}`).join(" · ")
              : "—"}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <AdminStatusBadge
            label={VERIFICATION_LABEL[verification] ?? verification}
            tone={TONE[verification]}
          />
          <div className="flex items-center gap-2 text-sm">
            <Switch
              checked={Boolean(vendor.featured)}
              onCheckedChange={() => void toggleFeatured()}
            />
            Featured vendor
          </div>
        </div>
        {vendor.verificationReason ? (
          <Fact label="Last reason" value={vendor.verificationReason} />
        ) : null}
      </section>

      <Section title="Documents">
        {data.documents.length === 0 ? (
          <p className="text-muted-foreground text-sm">No documents submitted.</p>
        ) : null}
        <div className="space-y-3">
          {data.documents.map((doc) => (
            <div
              key={doc.id}
              className="flex flex-col gap-2 rounded-xl border p-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="font-medium">{doc.fileName}</p>
                <p className="text-muted-foreground text-xs">
                  {doc.documentType.replaceAll("_", " ")} · {doc.verificationStatus}
                  {doc.reviewReason ? ` · ${doc.reviewReason}` : ""}
                </p>
                {doc.fileUrl ? (
                  <a
                    href={doc.fileUrl}
                    className="text-xs underline"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Open privately
                  </a>
                ) : null}
              </div>
              <div className="flex flex-wrap gap-1">
                <Button
                  size="sm"
                  className="rounded-lg"
                  onClick={() => {
                    setDocumentId(doc.id);
                    setDocumentAction("approve");
                  }}
                >
                  Approve
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="rounded-lg"
                  onClick={() => {
                    setDocumentId(doc.id);
                    setDocumentAction("reject");
                  }}
                >
                  Reject
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="rounded-lg"
                  onClick={() => {
                    setDocumentId(doc.id);
                    setDocumentAction("resubmit");
                  }}
                >
                  Request resubmission
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Section>

      <Section title="Portfolio">
        {vendor.portfolio.length === 0 ? (
          <p className="text-muted-foreground text-sm">No portfolio items.</p>
        ) : null}
        <div className="grid gap-3 sm:grid-cols-3">
          {vendor.portfolio.map((item) => (
            <div key={item.id} className="overflow-hidden rounded-xl border">
              {item.imageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element -- remote vendor URLs vary by storage host
                <img src={item.imageUrl} alt="" className="h-32 w-full object-cover" />
              ) : null}
              <p className="p-2 text-sm">{item.title ?? "Untitled"}</p>
            </div>
          ))}
        </div>
      </Section>

      <Section title="Products">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="text-muted-foreground text-left text-xs uppercase">
              <tr>
                <th className="py-2 pr-3">Product</th>
                <th className="py-2 pr-3">Status</th>
                <th className="py-2 pr-3">Price/day</th>
              </tr>
            </thead>
            <tbody>
              {data.products.map((product) => (
                <tr key={product.id} className="border-t">
                  <td className="py-2 pr-3">{product.name}</td>
                  <td className="py-2 pr-3">
                    {PRODUCT_STATUS_LABEL[product.status] ?? product.status}
                  </td>
                  <td className="py-2 pr-3">{formatEstimatedValue(product.pricePerDay)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Section>

      <Section title="Reviews">
        {data.reviews.length === 0 ? (
          <p className="text-muted-foreground text-sm">No reviews yet.</p>
        ) : null}
        <div className="space-y-2">
          {data.reviews.map((review) => (
            <p key={review.id} className="rounded-xl border p-3 text-sm">
              {review.overallRating}/5 · {review.reviewText ?? "No comment"}
            </p>
          ))}
        </div>
      </Section>

      <Section title="Booking history">
        {data.bookings.length === 0 ? (
          <p className="text-muted-foreground text-sm">No booking requests.</p>
        ) : null}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="text-muted-foreground text-left text-xs uppercase">
              <tr>
                <th className="py-2 pr-3">Planner</th>
                <th className="py-2 pr-3">Status</th>
                <th className="py-2 pr-3">Dates</th>
                <th className="py-2 pr-3">Est. value</th>
              </tr>
            </thead>
            <tbody>
              {data.bookings.map((booking) => (
                <tr key={booking.id} className="border-t">
                  <td className="py-2 pr-3">{booking.plannerName ?? "—"}</td>
                  <td className="py-2 pr-3">{booking.status}</td>
                  <td className="py-2 pr-3">
                    {booking.rentalStartDate} → {booking.rentalEndDate}
                  </td>
                  <td className="py-2 pr-3">
                    {formatEstimatedValue(booking.estimatedRentalValue)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Section>

      <AdminConfirmDialog
        open={Boolean(action)}
        title={
          action === "verify"
            ? "Verify vendor"
            : action === "reject"
              ? "Reject vendor"
              : action === "suspend"
                ? "Suspend vendor"
                : action === "reactivate"
                  ? "Reactivate vendor"
                  : "Start review"
        }
        description="This is recorded in the audit log with your admin identity and timestamp."
        confirmLabel="Confirm"
        requireReason={action === "reject" || action === "suspend"}
        onOpenChange={(open) => {
          if (!open) setAction(null);
        }}
        onConfirm={confirmVendor}
      />

      <AdminConfirmDialog
        open={Boolean(documentId && documentAction)}
        title={
          documentAction === "approve"
            ? "Approve document"
            : documentAction === "reject"
              ? "Reject document"
              : "Request resubmission"
        }
        description="Document URLs stay private. The reason is stored on the audit log."
        confirmLabel="Confirm"
        requireReason={documentAction === "reject" || documentAction === "resubmit"}
        onOpenChange={(open) => {
          if (!open) {
            setDocumentId(null);
            setDocumentAction(null);
          }
        }}
        onConfirm={confirmDocument}
      />
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-muted-foreground text-xs">{label}</p>
      <p className="text-sm font-medium">{value}</p>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-3">
      <h2 className="text-base font-semibold">{title}</h2>
      {children}
    </section>
  );
}
