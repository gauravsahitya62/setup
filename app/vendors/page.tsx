import type { Metadata } from "next";
import Link from "next/link";

import { RootShell } from "@/components/layout";
import { VendorCard } from "@/components/vendor/vendor-card";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import { createPageMetadata } from "@/lib/metadata";
import { fetchPublishedVendors } from "@/lib/vendor-server";

export const metadata: Metadata = createPageMetadata({
  title: "Vendors",
  description: "Browse verified rental vendors on FlowSet.",
  path: "/vendors",
});

export const dynamic = "force-dynamic";

export default async function VendorsIndexPage() {
  const vendors = await fetchPublishedVendors({ size: 48 });

  return (
    <RootShell>
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <p className="text-muted-foreground text-sm font-medium">Vendors</p>
          <h1 className="font-heading mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
            Verified rental partners
          </h1>
          <p className="text-muted-foreground mt-3 text-base leading-relaxed">
            Explore company profiles, portfolios, and catalogs before you request a booking.
          </p>
        </div>

        {vendors.length === 0 ? (
          <div className="mt-10">
            <EmptyState
              title="No published vendors yet"
              description="Published storefronts will appear here once vendors complete their profiles."
            />
          </div>
        ) : (
          <div className="mt-10 grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {vendors.map((vendor) => (
              <VendorCard
                key={vendor.id}
                vendor={vendor}
                href={`${ROUTES.vendors}/${vendor.slug}`}
              />
            ))}
          </div>
        )}

        <p className="text-muted-foreground mt-10 text-center text-sm">
          Looking for inventory?{" "}
          <Link
            href={ROUTES.marketplace}
            className="text-foreground font-medium underline-offset-4 hover:underline"
          >
            Browse the marketplace
          </Link>
        </p>
      </div>
    </RootShell>
  );
}
