import { env } from "@/lib/env";
import type {
  CategoryDto,
  PageResponse,
  ProductDto,
  ProductSummaryDto,
  SearchParams,
} from "@/services/marketplace-api";

const apiBase = env.NEXT_PUBLIC_API_BASE_URL.replace(/\/$/, "");

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

export async function fetchCategoryTree(includeHidden = false): Promise<CategoryDto[]> {
  const data = await fetchJson<CategoryDto[]>(
    `/categories?tree=true&includeHidden=${includeHidden}`
  );
  return data ?? [];
}

export async function fetchCategoryBySlug(slug: string): Promise<CategoryDto | null> {
  return fetchJson<CategoryDto>(`/categories/slug/${encodeURIComponent(slug)}`);
}

export async function fetchProductBySlug(slug: string): Promise<ProductDto | null> {
  return fetchJson<ProductDto>(`/products/slug/${encodeURIComponent(slug)}`);
}

export async function fetchProductById(id: string): Promise<ProductDto | null> {
  return fetchJson<ProductDto>(`/products/${encodeURIComponent(id)}`);
}

export async function fetchSearch(params: SearchParams): Promise<PageResponse<ProductSummaryDto>> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const page = await fetchJson<PageResponse<ProductSummaryDto>>(`/search?${search.toString()}`);
  return page ?? { content: [], page: 0, size: params.size ?? 12, totalElements: 0, totalPages: 0 };
}
