"use client";

import { useSearchParams } from "next/navigation";

import { VendorAvailabilityCalendar } from "@/components/vendor-dashboard/vendor-availability-calendar";

export function VendorAvailabilityExperience() {
  const searchParams = useSearchParams();
  const variant = searchParams.get("variant") ?? "all";
  return <VendorAvailabilityCalendar initialVariant={variant} />;
}
