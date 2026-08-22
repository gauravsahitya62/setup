import type { Metadata } from "next";

import { AdminReportsPage } from "@/components/admin/admin-reports-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Reports",
  description: "Operational and marketplace reports.",
  path: "/admin/reports",
});

export default function AdminReportsRoute() {
  return <AdminReportsPage />;
}
