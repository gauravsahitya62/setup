import type { Metadata } from "next";

import { ProductWizard } from "@/components/vendor-dashboard/catalog/product-wizard";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Edit product",
  description: "Update a rental product in your catalog.",
  path: "/vendor/catalog",
});

export default async function VendorEditProductPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <ProductWizard productId={id} />;
}
