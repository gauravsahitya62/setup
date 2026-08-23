import { serverApiBaseUrl } from "@/lib/api-base";
import {
  mapProductSummaryToListing,
  type PageResponse,
  type ProductSummaryDto,
} from "@/services/marketplace-api";
import { mapVendorDtoToProfile, type VendorDto, type VendorPageDto } from "@/services/vendor-api";
import type { MarketplaceListing, VendorProfile } from "@/types";

const apiBase = serverApiBaseUrl();

async function fetchJson<T>(path: string): Promise<T | null> {
  try {
    const res = await fetch(`${apiBase}${path}`, {
      next: { revalidate: 30 },
      headers: { Accept: "application/json" },
    });
    if (!res.ok) return null;
    return (await res.json()) as T;
  } catch {
    return null;
  }
}

export async function fetchPublishedVendors(params?: {
  companyName?: string;
  city?: string;
  state?: string;
  verified?: boolean;
  page?: number;
  size?: number;
}): Promise<VendorProfile[]> {
  const search = new URLSearchParams();
  if (params?.companyName) search.set("companyName", params.companyName);
  if (params?.city) search.set("city", params.city);
  if (params?.state) search.set("state", params.state);
  if (params?.verified != null) search.set("verified", String(params.verified));
  search.set("page", String(params?.page ?? 0));
  search.set("size", String(params?.size ?? 24));

  const page = await fetchJson<VendorPageDto>(`/vendors?${search.toString()}`);
  if (!page) return [];
  return page.content.map(mapVendorDtoToProfile);
}

export async function fetchVendorProfileBySlug(slug: string): Promise<VendorProfile | null> {
  const dto = await fetchJson<VendorDto>(`/vendors/slug/${encodeURIComponent(slug)}`);
  if (!dto) return null;
  return mapVendorDtoToProfile(dto);
}

export async function fetchVendorPublishedListings(
  vendorId: string
): Promise<MarketplaceListing[]> {
  const page = await fetchJson<PageResponse<ProductSummaryDto>>(
    `/vendors/${encodeURIComponent(vendorId)}/products?page=0&size=50`
  );
  if (!page) return [];
  return page.content.map(mapProductSummaryToListing);
}
