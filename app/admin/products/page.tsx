import type { Metadata } from "next";

import { AdminProductsPage } from "@/components/admin/admin-products-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Product moderation",
  description: "Moderate marketplace product listings.",
  path: "/admin/products",
});

export default function AdminProductsRoute() {
  return <AdminProductsPage />;
}
