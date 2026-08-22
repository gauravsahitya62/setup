import type { Metadata } from "next";

import { PlannerShell } from "@/components/planner/planner-shell";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Planner",
  description: "Manage projects, booking requests, and vendor sourcing in FlowSet.",
  path: "/planner",
});

export default function PlannerLayout({ children }: { children: React.ReactNode }) {
  return <PlannerShell>{children}</PlannerShell>;
}
