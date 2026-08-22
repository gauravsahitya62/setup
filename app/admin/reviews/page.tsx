import type { Metadata } from "next";

import { AdminReviewsPage } from "@/components/reviews/admin-reviews-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Reviews",
  description: "Moderate marketplace reviews and reports.",
  path: "/admin/reviews",
});

export default function AdminReviewsRoute() {
  return <AdminReviewsPage />;
}
