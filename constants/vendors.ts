import type { MarketplaceListing, VendorProfile, VendorReview } from "@/types";
import { MARKETPLACE_LISTINGS } from "@/constants/marketplace";

/** @deprecated Mock vendor profiles removed — public/ops storefronts use the API. */
export const VENDOR_PROFILES: VendorProfile[] = [];

export function getVendorProfile(_slug: string): VendorProfile | undefined {
  return undefined;
}

export function getVendorReviews(_slug: string): VendorReview[] {
  return [];
}

export function getVendorListings(slug: string): MarketplaceListing[] {
  return MARKETPLACE_LISTINGS.filter((listing) => listing.vendorSlug === slug);
}

export function getAllVendorSlugs(): string[] {
  return [];
}
