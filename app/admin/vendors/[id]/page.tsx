import type { Metadata } from "next";

import { AdminVendorDetailPage } from "@/components/admin/admin-vendor-detail-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Vendor review",
  description: "Verify documents, products, and booking history for a vendor.",
  path: "/admin/vendors",
});

export default function AdminVendorDetailRoute() {
  return <AdminVendorDetailPage />;
}
