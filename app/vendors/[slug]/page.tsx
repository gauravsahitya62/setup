import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { RootShell } from "@/components/layout";
import { VendorProfileView } from "@/components/vendor/vendor-profile-view";
import { createPageMetadata } from "@/lib/metadata";
import { fetchVendorProfileBySlug, fetchVendorPublishedListings } from "@/lib/vendor-server";

interface VendorPageProps {
  params: Promise<{ slug: string }>;
}

export const dynamic = "force-dynamic";

export async function generateMetadata({ params }: VendorPageProps): Promise<Metadata> {
  const { slug } = await params;
  const vendor = await fetchVendorProfileBySlug(slug);

  if (!vendor) {
    return createPageMetadata({
      title: "Vendor not found",
      path: `/vendors/${slug}`,
    });
  }

  return createPageMetadata({
    title: vendor.name,
    description: vendor.tagline ?? vendor.description,
    path: `/vendors/${slug}`,
  });
}

export default async function VendorProfilePublicPage({ params }: VendorPageProps) {
  const { slug } = await params;
  const vendor = await fetchVendorProfileBySlug(slug);

  if (!vendor) {
    notFound();
  }

  const listings = await fetchVendorPublishedListings(vendor.id);

  return (
    <RootShell>
      <VendorProfileView vendor={vendor} listings={listings} />
    </RootShell>
  );
}
