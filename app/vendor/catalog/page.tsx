import type { Metadata } from "next";

import { VendorInventoryPage } from "@/components/vendor-dashboard/vendor-inventory-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Inventory",
  description: "Manage your rental catalog, stock levels, and product statuses.",
  path: "/vendor/catalog",
});

export default function VendorCatalogPage() {
  return <VendorInventoryPage />;
}
