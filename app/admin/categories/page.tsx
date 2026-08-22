import type { Metadata } from "next";

import { AdminCategoriesPage } from "@/components/admin/admin-categories-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Categories",
  description: "Manage marketplace category taxonomy.",
  path: "/admin/categories",
});

export default function AdminCategoriesRoute() {
  return <AdminCategoriesPage />;
}
