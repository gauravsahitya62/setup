import Link from "next/link";
import { BarChart3 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { VendorAnalyticsCharts } from "@/components/vendor-dashboard/vendor-analytics-charts";
import { ROUTES } from "@/constants";

export default function VendorAnalyticsPage() {
  return (
    <div className="space-y-8">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Analytics</h1>
        <p className="text-muted-foreground text-sm">
          Estimated rental value from confirmed orders. This is not collected revenue.
        </p>
      </div>
      <VendorAnalyticsCharts />
      <EmptyState
        title="Deeper reports coming soon"
        description="Cohort, utilization, and planner conversion reports will expand this view."
        icon={<BarChart3 className="size-5" />}
      />
      <div className="flex justify-center">
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.root}>Back to dashboard</Link>
        </Button>
      </div>
    </div>
  );
}
