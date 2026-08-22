import { apiClient, axiosClient } from "@/services/api-client";
import type { MarketplaceListing } from "@/types";

export type ProductStatus =
  "DRAFT" | "PUBLISHED" | "PENDING_REVIEW" | "REJECTED" | "SUSPENDED" | "ARCHIVED";
export type VariantStatus = "ACTIVE" | "INACTIVE";
export type AvailabilityStatus = "AVAILABLE" | "RESERVED" | "BLOCKED" | "MAINTENANCE";
export type InventoryStockStatus = "AVAILABLE" | "LOW_STOCK" | "UNAVAILABLE" | "MAINTENANCE";
export type AvailabilityLevel = "AVAILABLE" | "PARTIAL" | "UNAVAILABLE";
export type ReservationStatus = "PENDING" | "CONFIRMED" | "RELEASED" | "CANCELLED" | "EXPIRED";
export type InventoryMovementType =
  | "STOCK_ADDED"
  | "STOCK_REMOVED"
  | "DAMAGED"
  | "REPAIRED"
  | "MAINTENANCE"
  | "ADJUSTMENT"
  | "RETURNED";
export type MaintenanceRecordStatus = "ACTIVE" | "REMOVED";
export type InventoryBlockReason =
  "MAINTENANCE" | "PRIVATE_BOOKING" | "DAMAGED" | "VENDOR_UNAVAILABLE" | "OTHER";
export type CategoryStatus = "ACTIVE" | "HIDDEN" | "ARCHIVED";
export type CollectionStatus = "ACTIVE" | "HIDDEN";
export type SearchSort = "NEWEST" | "POPULARITY" | "PRICE_ASC" | "PRICE_DESC" | "RATING";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CategoryDto {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  icon?: string | null;
  imageUrl?: string | null;
  parentCategoryId?: string | null;
  displayOrder: number;
  featured?: boolean;
  status: CategoryStatus;
  children: CategoryDto[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductSummaryDto {
  id: string;
  vendorId: string;
  vendorName: string;
  vendorSlug: string;
  categoryId: string;
  categoryName: string;
  categorySlug: string;
  name: string;
  slug: string;
  shortDescription?: string | null;
  featuredImage?: string | null;
  pricePerDay: number | string;
  status: ProductStatus;
  featured: boolean;
  verifiedVendor: boolean;
  vendorRating: number | string;
  reviewCount: number;
  city?: string | null;
  available: boolean;
  moderationReason?: string | null;
  createdAt?: string;
}

export interface VariantDto {
  id: string;
  productId: string;
  variantName: string;
  sku: string;
  color?: string | null;
  material?: string | null;
  quantity: number;
  availableQuantity: number;
  pricePerDay: number | string;
  status: VariantStatus;
}

export interface ImageDto {
  id: string;
  imageUrl: string;
  altText?: string | null;
  displayOrder: number;
  featured: boolean;
}

export interface AttributeDto {
  id: string;
  attributeName: string;
  attributeValue: string;
  displayOrder: number;
}

export interface ProductDto {
  id: string;
  vendorId: string;
  vendorName: string;
  vendorSlug: string;
  verifiedVendor: boolean;
  vendorRating: number | string;
  reviewCount: number;
  categoryId: string;
  categoryName: string;
  categorySlug: string;
  subcategoryId?: string | null;
  subcategoryName?: string | null;
  name: string;
  slug: string;
  shortDescription?: string | null;
  description?: string | null;
  featuredImage?: string | null;
  pricePerDay: number | string;
  securityDeposit?: number | string | null;
  minimumRentalDays: number;
  deliveryAvailable: boolean;
  setupAvailable: boolean;
  pickupAvailable: boolean;
  status: ProductStatus;
  featured: boolean;
  viewCount?: number;
  moderationReason?: string | null;
  variants: VariantDto[];
  images: ImageDto[];
  attributes: AttributeDto[];
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequestPayload {
  vendorId?: string;
  name: string;
  slug?: string;
  categoryId: string;
  subcategoryId?: string;
  shortDescription?: string;
  description?: string;
  pricePerDay: number;
  securityDeposit?: number;
  minimumRentalDays?: number;
  deliveryAvailable?: boolean;
  setupAvailable?: boolean;
  pickupAvailable?: boolean;
  featured?: boolean;
  status?: ProductStatus;
  tagIds?: string[];
}

export interface VariantRequestPayload {
  variantName: string;
  sku: string;
  color?: string;
  material?: string;
  quantity: number;
  pricePerDay?: number;
  status?: VariantStatus;
}

export interface AttributeRequestPayload {
  attributeName: string;
  attributeValue: string;
  displayOrder?: number;
}

export interface InventoryDto {
  id: string;
  variantId: string;
  totalQuantity: number;
  reserved: number;
  available: number;
  maintenance: number;
  damaged: number;
  createdAt?: string;
  updatedAt: string;
  version?: number;
}

export interface InventoryUpdatePayload {
  totalQuantity: number;
  reserved?: number;
  maintenance?: number;
  damaged?: number;
}

export interface VendorInventoryItemDto {
  id: string;
  variantId: string;
  productId: string;
  productName: string;
  productSlug: string;
  featuredImage?: string | null;
  variantName: string;
  sku: string;
  color?: string | null;
  totalQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  maintenanceQuantity: number;
  damagedQuantity: number;
  status: InventoryStockStatus;
  lowStock: boolean;
  updatedAt: string;
  version: number;
}

export interface VendorInventoryOverviewDto {
  totalProducts: number;
  totalVariants: number;
  totalUnits: number;
  reservedUnits: number;
  availableUnits: number;
  maintenanceUnits: number;
  damagedUnits: number;
  lowAvailabilityCount: number;
  items: VendorInventoryItemDto[];
}

export interface ReservationDto {
  id: string;
  variantId: string;
  bookingRequestId?: string | null;
  quantity: number;
  startDate: string;
  endDate: string;
  status: ReservationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MaintenanceRecordDto {
  id: string;
  variantId: string;
  blockId: string;
  quantity: number;
  startDate: string;
  endDate: string;
  reason: string;
  status: MaintenanceRecordStatus;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryMovementDto {
  id: string;
  variantId: string;
  quantity: number;
  movementType: InventoryMovementType;
  reason: string;
  referenceId?: string | null;
  createdBy?: string | null;
  createdAt: string;
}

export interface VendorInventoryDetailDto {
  id: string;
  variantId: string;
  productId: string;
  productName: string;
  productSlug: string;
  featuredImage?: string | null;
  imageUrls: string[];
  variantName: string;
  sku: string;
  color?: string | null;
  material?: string | null;
  totalQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  maintenanceQuantity: number;
  damagedQuantity: number;
  status: InventoryStockStatus;
  lowStock: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
  upcomingReservations: ReservationDto[];
  maintenanceRecords: MaintenanceRecordDto[];
  recentMovements: InventoryMovementDto[];
}

export interface CalendarDayDto {
  date: string;
  variantId?: string | null;
  productName?: string | null;
  variantName?: string | null;
  totalQuantity: number;
  reservedQuantity: number;
  maintenanceQuantity: number;
  damagedQuantity: number;
  blockedQuantity: number;
  availableQuantity: number;
}

export interface AvailabilityConflictDto {
  date: string;
  reservedQuantity: number;
  blockedQuantity: number;
  availableQuantity: number;
  type: string;
}

export interface AvailabilityCheckDto {
  variantId: string;
  available: boolean;
  status: AvailabilityLevel;
  requestedQuantity: number;
  availableQuantity: number;
  message: string;
  conflicts: AvailabilityConflictDto[];
  calendar: CalendarDayDto[];
}

export interface InventoryAdjustPayload {
  delta: number;
  reason: string;
}

export interface MaintenanceCreatePayload {
  quantity: number;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface AvailabilityDto {
  id: string;
  variantId: string;
  date: string;
  status: AvailabilityStatus;
  quantityReserved: number;
  notes?: string | null;
}

export interface AvailabilityUpdatePayload {
  date: string;
  status: AvailabilityStatus;
  quantityReserved?: number;
  notes?: string;
}

export interface CollectionDto {
  id: string;
  vendorId: string;
  name: string;
  slug: string;
  description?: string | null;
  featuredImage?: string | null;
  status: CollectionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CollectionRequestPayload {
  vendorId?: string;
  name: string;
  slug?: string;
  description?: string;
  featuredImage?: string;
  status?: CollectionStatus;
}

export interface CollectionItemDto {
  id: string;
  productId: string;
  productName: string;
  productSlug: string;
  displayOrder: number;
}

export interface SearchParams {
  keyword?: string;
  categoryId?: string;
  subcategoryId?: string;
  vendorId?: string;
  city?: string;
  minPrice?: number;
  maxPrice?: number;
  availableFrom?: string;
  availableTo?: string;
  verifiedVendor?: boolean;
  minRating?: number;
  sort?: SearchSort;
  page?: number;
  size?: number;
}

function toQueryParams(
  params: Record<string, string | number | boolean | undefined>
): Record<string, string | number | boolean | undefined> {
  const result: Record<string, string | number | boolean | undefined> = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      result[key] = value;
    }
  });
  return result;
}

export const marketplaceApi = {
  search: (params: SearchParams, signal?: AbortSignal) =>
    apiClient.get<PageResponse<ProductSummaryDto>>("/search", {
      params: toQueryParams({ ...params }),
      signal,
    }),

  categories: {
    tree: (includeHidden = false) =>
      apiClient.get<CategoryDto[]>("/categories", { params: { tree: true, includeHidden } }),
    flat: (includeHidden = false) =>
      apiClient.get<CategoryDto[]>("/categories", { params: { tree: false, includeHidden } }),
    getById: (id: string) => apiClient.get<CategoryDto>(`/categories/${id}`),
    getBySlug: (slug: string) => apiClient.get<CategoryDto>(`/categories/slug/${slug}`),
  },

  products: {
    getById: (id: string) => apiClient.get<ProductDto>(`/products/${id}`),
    getBySlug: (slug: string) => apiClient.get<ProductDto>(`/products/slug/${slug}`),
    create: (payload: ProductRequestPayload) => apiClient.post<ProductDto>("/products", payload),
    update: (id: string, payload: ProductRequestPayload) =>
      apiClient.put<ProductDto>(`/products/${id}`, payload),
    remove: (id: string) => apiClient.delete<void>(`/products/${id}`),
    addImage: async (id: string, file: File, altText?: string) => {
      const form = new FormData();
      form.append("file", file);
      const { data } = await axiosClient.post<ImageDto>(`/products/${id}/images`, form, {
        headers: { "Content-Type": "multipart/form-data" },
        params: altText ? { altText } : undefined,
      });
      return data;
    },
    deleteImage: (id: string, imageId: string) =>
      apiClient.delete<void>(`/products/${id}/images/${imageId}`),
    reorderImages: (id: string, imageIds: string[]) =>
      apiClient.put<ImageDto[]>(`/products/${id}/images/reorder`, { imageIds }),
    listAttributes: (id: string) => apiClient.get<AttributeDto[]>(`/products/${id}/attributes`),
    addAttribute: (id: string, payload: AttributeRequestPayload) =>
      apiClient.post<AttributeDto>(`/products/${id}/attributes`, payload),
    updateAttribute: (id: string, attributeId: string, payload: AttributeRequestPayload) =>
      apiClient.put<AttributeDto>(`/products/${id}/attributes/${attributeId}`, payload),
    deleteAttribute: (id: string, attributeId: string) =>
      apiClient.delete<void>(`/products/${id}/attributes/${attributeId}`),
  },

  vendorProducts: {
    list: (vendorId: string, page = 0, size = 12) =>
      apiClient.get<PageResponse<ProductSummaryDto>>(`/vendors/${vendorId}/products`, {
        params: { page, size },
      }),
  },

  variants: {
    list: (productId: string) => apiClient.get<VariantDto[]>(`/products/${productId}/variants`),
    getById: (productId: string, variantId: string) =>
      apiClient.get<VariantDto>(`/products/${productId}/variants/${variantId}`),
    create: (productId: string, payload: VariantRequestPayload) =>
      apiClient.post<VariantDto>(`/products/${productId}/variants`, payload),
    update: (productId: string, variantId: string, payload: VariantRequestPayload) =>
      apiClient.put<VariantDto>(`/products/${productId}/variants/${variantId}`, payload),
    remove: (productId: string, variantId: string) =>
      apiClient.delete<void>(`/products/${productId}/variants/${variantId}`),
  },

  inventory: {
    get: (variantId: string) => apiClient.get<InventoryDto>(`/variants/${variantId}/inventory`),
    update: (variantId: string, payload: InventoryUpdatePayload) =>
      apiClient.put<InventoryDto>(`/variants/${variantId}/inventory`, payload),
    check: (variantId: string, startDate: string, endDate: string, quantity?: number) =>
      apiClient.get<AvailabilityCheckDto>(`/inventory/${variantId}/availability`, {
        params: toQueryParams({ startDate, endDate, quantity }),
      }),
  },

  vendorInventory: {
    overview: () => apiClient.get<VendorInventoryOverviewDto>("/vendor/inventory"),
    get: (variantId: string) =>
      apiClient.get<VendorInventoryDetailDto>(`/vendor/inventory/${variantId}`),
    patch: (variantId: string, payload: InventoryUpdatePayload) =>
      apiClient.patch<InventoryDto>(`/vendor/inventory/${variantId}`, payload),
    adjust: (variantId: string, payload: InventoryAdjustPayload) =>
      apiClient.post<InventoryDto>(`/vendor/inventory/${variantId}/adjust`, payload),
    createMaintenance: (variantId: string, payload: MaintenanceCreatePayload) =>
      apiClient.post<MaintenanceRecordDto>(`/vendor/inventory/${variantId}/maintenance`, payload),
    deleteMaintenance: (variantId: string, id: string) =>
      apiClient.delete<void>(`/vendor/inventory/${variantId}/maintenance/${id}`),
    movements: (variantId: string, limit = 50) =>
      apiClient.get<InventoryMovementDto[]>(`/vendor/inventory/${variantId}/movements`, {
        params: { limit },
      }),
    calendar: (params: {
      variant?: string;
      product?: string;
      category?: string;
      startDate: string;
      endDate: string;
    }) =>
      apiClient.get<CalendarDayDto[]>("/vendor/calendar", {
        params: toQueryParams({ ...params }),
      }),
  },

  availability: {
    list: (variantId: string, from?: string, to?: string) =>
      apiClient.get<AvailabilityDto[]>(`/variants/${variantId}/availability`, {
        params: toQueryParams({ from, to }),
      }),
    upsert: (variantId: string, payload: AvailabilityUpdatePayload) =>
      apiClient.put<AvailabilityDto>(`/variants/${variantId}/availability`, payload),
  },

  favorites: {
    list: (page = 0, size = 24) =>
      apiClient.get<PageResponse<ProductSummaryDto>>("/favorites", { params: { page, size } }),
    add: (productId: string) => apiClient.post<void>(`/favorites/${productId}`),
    remove: (productId: string) => apiClient.delete<void>(`/favorites/${productId}`),
  },

  collections: {
    search: (vendorId?: string, page = 0, size = 12) =>
      apiClient.get<PageResponse<CollectionDto>>("/collections", {
        params: toQueryParams({ vendorId, page, size }),
      }),
    getById: (id: string) => apiClient.get<CollectionDto>(`/collections/${id}`),
    getBySlug: (slug: string) => apiClient.get<CollectionDto>(`/collections/slug/${slug}`),
    create: (payload: CollectionRequestPayload) =>
      apiClient.post<CollectionDto>("/collections", payload),
    update: (id: string, payload: CollectionRequestPayload) =>
      apiClient.put<CollectionDto>(`/collections/${id}`, payload),
    remove: (id: string) => apiClient.delete<void>(`/collections/${id}`),
    listItems: (id: string) => apiClient.get<CollectionItemDto[]>(`/collections/${id}/items`),
    addItem: (id: string, productId: string, displayOrder?: number) =>
      apiClient.post<CollectionItemDto>(`/collections/${id}/items`, { productId, displayOrder }),
    removeItem: (id: string, productId: string) =>
      apiClient.delete<void>(`/collections/${id}/items/${productId}`),
    reorderItems: (id: string, productIds: string[]) =>
      apiClient.put<CollectionItemDto[]>(`/collections/${id}/items/reorder`, { productIds }),
  },
};

/** Flatten a category tree into a slug -> id lookup (includes nested children). */
export function flattenCategorySlugMap(categories: CategoryDto[]): Map<string, string> {
  const map = new Map<string, string>();
  function walk(nodes: CategoryDto[]) {
    nodes.forEach((node) => {
      map.set(node.slug, node.id);
      if (node.children?.length) walk(node.children);
    });
  }
  walk(categories);
  return map;
}

/** Flatten a category tree into a flat list (parents + all descendants). */
export function flattenCategories(categories: CategoryDto[]): CategoryDto[] {
  const result: CategoryDto[] = [];
  function walk(nodes: CategoryDto[]) {
    nodes.forEach((node) => {
      result.push(node);
      if (node.children?.length) walk(node.children);
    });
  }
  walk(categories);
  return result;
}

export function mapProductSummaryToListing(summary: ProductSummaryDto): MarketplaceListing {
  const now = new Date().toISOString();
  return {
    id: summary.id,
    vendorId: summary.vendorId,
    name: summary.name,
    slug: summary.slug,
    description: summary.shortDescription ?? "",
    imageUrl: summary.featuredImage ?? undefined,
    categoryId: summary.categoryId,
    dailyRate: Number(summary.pricePerDay ?? 0),
    currency: "USD",
    available: summary.available,
    vendorName: summary.vendorName,
    vendorSlug: summary.vendorSlug,
    vendorVerified: summary.verifiedVendor,
    city: summary.city ?? "",
    rating: Number(summary.vendorRating ?? 0),
    reviewCount: summary.reviewCount,
    categorySlug: summary.categorySlug,
    categoryName: summary.categoryName,
    createdAt: now,
    updatedAt: now,
  };
}

export function mapProductResponseToListing(product: ProductDto): MarketplaceListing {
  const now = new Date().toISOString();
  return {
    id: product.id,
    vendorId: product.vendorId,
    name: product.name,
    slug: product.slug,
    description: product.shortDescription ?? product.description ?? "",
    imageUrl: product.featuredImage ?? product.images[0]?.imageUrl,
    categoryId: product.categoryId,
    dailyRate: Number(product.pricePerDay ?? 0),
    currency: "USD",
    available: product.status === "PUBLISHED",
    vendorName: product.vendorName,
    vendorSlug: product.vendorSlug,
    vendorVerified: product.verifiedVendor,
    city: "",
    rating: Number(product.vendorRating ?? 0),
    reviewCount: product.reviewCount,
    categorySlug: product.categorySlug,
    categoryName: product.categoryName,
    createdAt: product.createdAt ?? now,
    updatedAt: product.updatedAt ?? now,
  };
}
