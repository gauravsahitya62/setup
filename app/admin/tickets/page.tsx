import type { Metadata } from "next";

import { AdminTicketsPage } from "@/components/admin/admin-tickets-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Support tickets",
  description: "Customer support triage queue.",
  path: "/admin/tickets",
});

export default function AdminTicketsRoute() {
  return <AdminTicketsPage />;
}
