import type { Metadata } from "next";

import { AdminAuditPage } from "@/components/admin/admin-audit-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Audit logs",
  description: "Immutable privileged action trail.",
  path: "/admin/audit-logs",
});

export default function AdminAuditLogsRoute() {
  return <AdminAuditPage />;
}
