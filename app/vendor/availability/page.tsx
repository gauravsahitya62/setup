import { Suspense } from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { VendorAvailabilityExperience } from "@/components/vendor-dashboard/inventory/vendor-availability-experience";
import { ROUTES } from "@/constants";

export default function VendorAvailabilityPage() {
  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Availability</h1>
          <p className="text-muted-foreground text-sm">
            Month and week views of reserved, maintenance, and available units. Click a date for the
            breakdown.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.inventory}>Inventory</Link>
        </Button>
      </div>
      <Suspense>
        <VendorAvailabilityExperience />
      </Suspense>
    </div>
  );
}
