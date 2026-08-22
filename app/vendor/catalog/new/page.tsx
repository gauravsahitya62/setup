import type { Metadata } from "next";

import { ProductWizard } from "@/components/vendor-dashboard/catalog/product-wizard";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Add product",
  description: "Add a new rental product to your catalog.",
  path: "/vendor/catalog/new",
});

export default function VendorNewProductPage() {
  return <ProductWizard />;
}
