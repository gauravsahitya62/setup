import type { Metadata } from "next";

import { VendorStockOverviewPage } from "@/components/vendor-dashboard/inventory/vendor-stock-overview-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Inventory",
  description: "Manage rental stock, reservations, and maintenance for your variants.",
  path: "/vendor/inventory",
});

export default function VendorStockPage() {
  return <VendorStockOverviewPage />;
}
