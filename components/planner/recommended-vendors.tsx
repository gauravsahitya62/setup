"use client";

import Link from "next/link";

import { Button } from "@/components/ui/button";
import { VendorCard } from "@/components/vendor/vendor-card";
import type { Vendor } from "@/types";
import { ROUTES } from "@/constants";

interface RecommendedVendorsProps {
  vendors: Vendor[];
}

export function RecommendedVendors({ vendors }: RecommendedVendorsProps) {
  return (
    <section>
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">Recommended vendors</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Verified partners that match your upcoming event profiles.
          </p>
        </div>
        <Button variant="outline" size="sm" className="rounded-xl" asChild>
          <Link href={ROUTES.vendors}>Explore vendors</Link>
        </Button>
      </div>

      {vendors.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          No verified vendors are listed yet. Browse the vendor directory as catalogs are published.
        </p>
      ) : (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {vendors.map((vendor, index) => (
            <div
              key={vendor.id}
              className="animate-in fade-in slide-in-from-bottom-3 fill-mode-both duration-500"
              style={{ animationDelay: `${index * 60}ms` }}
            >
              <VendorCard vendor={vendor} href={`${ROUTES.vendors}/${vendor.slug}`} />
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
