import type { Metadata } from "next";

import { VendorStockDetailPage } from "@/components/vendor-dashboard/inventory/vendor-stock-detail-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Inventory detail",
  description: "Variant stock, availability calendar, adjustments, and maintenance.",
  path: "/vendor/inventory",
});

export default async function VendorStockDetailRoute({
  params,
}: {
  params: Promise<{ variantId: string }>;
}) {
  const { variantId } = await params;
  return <VendorStockDetailPage variantId={variantId} />;
}
