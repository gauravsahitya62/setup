import type { Metadata } from "next";

import { AdminDashboard } from "@/components/admin/admin-dashboard";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Admin overview",
  description: "Platform health, queues, and operational analytics.",
  path: "/admin",
});

export default function AdminHomePage() {
  return <AdminDashboard />;
}
