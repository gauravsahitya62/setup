import type { Metadata } from "next";

import { AdminUsersPage } from "@/components/admin/admin-users-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Users",
  description: "Manage FlowSet user accounts and roles.",
  path: "/admin/users",
});

export default function AdminUsersRoute() {
  return <AdminUsersPage />;
}
