"use client";

import * as React from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { VendorCard } from "@/components/vendor/vendor-card";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import { mapVendorDtoToProfile, vendorApi } from "@/services";
import type { VendorProfile } from "@/types";

export function LandingVendors() {
  const [vendors, setVendors] = React.useState<VendorProfile[]>([]);
  const [loaded, setLoaded] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const page = await vendorApi.list({ size: 6, verified: true });
        if (!cancelled) {
          const mapped = page.content.map(mapVendorDtoToProfile);
          if (mapped.length === 0) {
            const fallback = await vendorApi.list({ size: 6 });
            if (!cancelled) setVendors(fallback.content.map(mapVendorDtoToProfile));
          } else {
            setVendors(mapped);
          }
        }
      } catch {
        if (!cancelled) setVendors([]);
      } finally {
        if (!cancelled) setLoaded(true);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="bg-muted/35 py-20 sm:py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <FadeIn className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <SectionHeading
            eyebrow="Featured vendors"
            title="Trusted partners for serious events"
            description="Work with verified rental companies known for reliable inventory and responsive booking."
          />
          <Button asChild variant="outline" className="self-start rounded-xl sm:self-auto">
            <Link href={ROUTES.vendors}>View all vendors</Link>
          </Button>
        </FadeIn>

        <div className="mt-12">
          {!loaded ? (
            <p className="text-muted-foreground text-sm">Loading vendors…</p>
          ) : vendors.length === 0 ? (
            <EmptyState
              title="Vendors coming soon"
              description="Published storefronts will appear here as partners join FlowSet."
            />
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {vendors.map((vendor, index) => (
                <FadeIn key={vendor.id} delay={index * 0.08}>
                  <VendorCard vendor={vendor} href={`${ROUTES.vendors}/${vendor.slug}`} />
                </FadeIn>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
