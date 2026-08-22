import type { Metadata } from "next";

import { AdminVendorVerificationPage } from "@/components/admin/admin-vendors-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Vendor verification",
  description: "Review and approve vendor applications.",
  path: "/admin/vendors",
});

export default function AdminVendorsRoute() {
  return <AdminVendorVerificationPage />;
}
