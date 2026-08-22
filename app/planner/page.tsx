import type { Metadata } from "next";

import { PlannerDashboard } from "@/components/planner/planner-dashboard";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Dashboard",
  description: "Your FlowSet planner dashboard — projects, requests, and upcoming events.",
  path: "/planner",
});

export default function PlannerDashboardPage() {
  return <PlannerDashboard />;
}
