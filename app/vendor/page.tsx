import type { Metadata } from "next";

import { VendorDashboard } from "@/components/vendor-dashboard/vendor-dashboard";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Vendor dashboard",
  description: "Revenue, requests, inventory, and bookings for your rental catalog.",
  path: "/vendor",
});

export default function VendorDashboardPage() {
  return <VendorDashboard />;
}
