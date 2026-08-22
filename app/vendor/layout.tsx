import type { Metadata } from "next";

import { VendorOpsShell } from "@/components/vendor-dashboard/vendor-ops-shell";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Vendor",
  description: "Manage inventory, requests, bookings, and analytics in FlowSet.",
  path: "/vendor",
});

export default function VendorLayout({ children }: { children: React.ReactNode }) {
  return <VendorOpsShell>{children}</VendorOpsShell>;
}
