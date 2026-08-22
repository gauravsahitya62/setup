import type { Metadata } from "next";

import { RootShell } from "@/components/layout";
import {
  LandingCategories,
  LandingCta,
  LandingFaq,
  LandingHero,
  LandingHowItWorks,
  LandingRentals,
  LandingSearch,
  LandingTestimonials,
  LandingVendors,
  LandingWhy,
} from "@/components/marketing";
import { createPageMetadata } from "@/lib/metadata";

export const metadata: Metadata = createPageMetadata({
  title: "Powering Every Event",
  description:
    "FlowSet helps event planners discover, compare, and request bookings from verified rental vendors.",
  path: "/",
});

export default function HomePage() {
  return (
    <RootShell>
      <LandingHero />
      <LandingSearch />
      <LandingCategories />
      <LandingVendors />
      <LandingRentals />
      <LandingHowItWorks />
      <LandingWhy />
      <LandingTestimonials />
      <LandingFaq />
      <LandingCta />
    </RootShell>
  );
}
