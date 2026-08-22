import type { Metadata } from "next";

import { AdminOrdersPage } from "@/components/admin/admin-orders-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Orders",
  description: "Support view of marketplace orders.",
  path: "/admin/orders",
});

export default function AdminOrdersRoute() {
  return <AdminOrdersPage />;
}
