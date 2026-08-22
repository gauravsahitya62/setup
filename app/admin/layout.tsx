import type { Metadata } from "next";

import { AdminShell } from "@/components/admin/admin-shell";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Admin",
  description: "FlowSet enterprise admin — verification, moderation, users, and support.",
  path: "/admin",
});

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <AdminShell>{children}</AdminShell>;
}
