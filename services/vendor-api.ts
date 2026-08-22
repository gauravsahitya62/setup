import { apiClient, axiosClient } from "@/services/api-client";
import type { VendorProfile, VendorSocialLinks } from "@/types";

export type VendorStatus = "DRAFT" | "PUBLISHED" | "SUSPENDED";

export interface VendorServiceAreaDto {
  id: string;
  country: string;
  state: string;
  city: string;
}

export interface VendorPortfolioItemDto {
  id: string;
  imageUrl: string;
  title?: string | null;
  description?: string | null;
  displayOrder: number;
}

export interface VendorDocumentDto {
  id: string;
  fileUrl: string;
  fileName: string;
  documentType: string;
  verificationStatus: string;
  reviewReason?: string | null;
  reviewedAt?: string | null;
}

export interface VendorSocialLinksDto {
  instagram?: string | null;
  facebook?: string | null;
  linkedin?: string | null;
  youtube?: string | null;
  pinterest?: string | null;
  website?: string | null;
}

export interface VendorDto {
  id: string;
  ownerUserId: string;
  companyName: string;
  slug: string;
  description?: string | null;
  shortDescription?: string | null;
  gstNumber?: string | null;
  businessType?: string | null;
  establishedYear?: number | null;
  website?: string | null;
  email?: string | null;
  phone?: string | null;
  logoUrl?: string | null;
  bannerUrl?: string | null;
  rating: number;
  reviewCount: number;
  completedEvents: number;
  responseTimeMinutes?: number | null;
  verified: boolean;
  status: VendorStatus;
  verificationStatus?: "PENDING" | "UNDER_REVIEW" | "VERIFIED" | "REJECTED" | "SUSPENDED";
  featured?: boolean;
  verificationReason?: string | null;
  serviceAreas: VendorServiceAreaDto[];
  portfolio: VendorPortfolioItemDto[];
  socialLinks?: VendorSocialLinksDto | null;
  documents: VendorDocumentDto[];
  createdAt: string;
  updatedAt: string;
}

export interface VendorPageDto {
  content: VendorDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateVendorPayload {
  companyName: string;
  slug?: string;
  description?: string;
  shortDescription?: string;
  gstNumber?: string;
  businessType?: string;
  establishedYear?: number;
  website?: string;
  email?: string;
  phone?: string;
  status?: VendorStatus;
}

export type UpdateVendorPayload = CreateVendorPayload & {
  responseTimeMinutes?: number;
  completedEvents?: number;
};

export function mapVendorDtoToProfile(dto: VendorDto): VendorProfile {
  const social: VendorSocialLinks = {
    website: dto.socialLinks?.website ?? dto.website ?? undefined,
    instagram: dto.socialLinks?.instagram ?? undefined,
    linkedin: dto.socialLinks?.linkedin ?? undefined,
    facebook: dto.socialLinks?.facebook ?? undefined,
    youtube: dto.socialLinks?.youtube ?? undefined,
    pinterest: dto.socialLinks?.pinterest ?? undefined,
  };

  const year = new Date().getFullYear();
  const yearsInBusiness = dto.establishedYear ? Math.max(0, year - dto.establishedYear) : 0;

  return {
    id: dto.id,
    name: dto.companyName,
    slug: dto.slug,
    tagline: dto.shortDescription ?? undefined,
    description: dto.shortDescription ?? dto.description ?? "",
    about: dto.description ?? dto.shortDescription ?? "",
    logoUrl: dto.logoUrl ?? undefined,
    coverImageUrl: dto.bannerUrl ?? undefined,
    location: dto.serviceAreas[0]
      ? `${dto.serviceAreas[0].city}, ${dto.serviceAreas[0].state}`
      : "",
    rating: Number(dto.rating ?? 0),
    reviewCount: dto.reviewCount,
    verificationStatus: dto.verified ? "verified" : "pending",
    categories: dto.businessType ? [dto.businessType] : [],
    responseTimeHours: dto.responseTimeMinutes
      ? Math.max(1, Math.round(dto.responseTimeMinutes / 60))
      : undefined,
    completedEvents: dto.completedEvents,
    yearsInBusiness,
    serviceCities: dto.serviceAreas.map((a) => `${a.city}, ${a.state}`),
    portfolio: dto.portfolio.map((p) => ({
      id: p.id,
      imageUrl: p.imageUrl,
      caption: p.title ?? p.description ?? undefined,
    })),
    social,
    highlights: undefined,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

export const vendorApi = {
  list: (params?: Record<string, string | number | boolean | undefined>) =>
    apiClient.get<VendorPageDto>("/vendors", { params }),
  getById: (id: string) => apiClient.get<VendorDto>(`/vendors/${id}`),
  getBySlug: (slug: string) => apiClient.get<VendorDto>(`/vendors/slug/${slug}`),
  getMine: () => apiClient.get<VendorDto>("/vendors/me"),
  create: (payload: CreateVendorPayload) => apiClient.post<VendorDto>("/vendors", payload),
  update: (id: string, payload: UpdateVendorPayload) =>
    apiClient.put<VendorDto>(`/vendors/${id}`, payload),
  remove: (id: string) => apiClient.delete<void>(`/vendors/${id}`),
  uploadLogo: async (id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    const { data } = await axiosClient.post<VendorDto>(`/vendors/${id}/logo`, form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data;
  },
  uploadBanner: async (id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    const { data } = await axiosClient.post<VendorDto>(`/vendors/${id}/banner`, form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data;
  },
  listServiceAreas: (id: string) =>
    apiClient.get<VendorServiceAreaDto[]>(`/vendors/${id}/service-areas`),
  addServiceArea: (id: string, body: { country: string; state: string; city: string }) =>
    apiClient.post<VendorServiceAreaDto>(`/vendors/${id}/service-areas`, body),
  updateServiceArea: (
    id: string,
    areaId: string,
    body: { country: string; state: string; city: string }
  ) => apiClient.put<VendorServiceAreaDto>(`/vendors/${id}/service-areas/${areaId}`, body),
  deleteServiceArea: (id: string, areaId: string) =>
    apiClient.delete<void>(`/vendors/${id}/service-areas/${areaId}`),
  getSocialLinks: (id: string) =>
    apiClient.get<VendorSocialLinksDto>(`/vendors/${id}/social-links`),
  upsertSocialLinks: (id: string, body: VendorSocialLinksDto) =>
    apiClient.put<VendorSocialLinksDto>(`/vendors/${id}/social-links`, body),
  addPortfolio: async (id: string, file: File, title?: string, description?: string) => {
    const form = new FormData();
    form.append("file", file);
    if (title) form.append("title", title);
    if (description) form.append("description", description);
    const { data } = await axiosClient.post<VendorPortfolioItemDto>(
      `/vendors/${id}/portfolio`,
      form,
      { headers: { "Content-Type": "multipart/form-data" } }
    );
    return data;
  },
  updatePortfolio: (
    id: string,
    itemId: string,
    body: { title?: string; description?: string; displayOrder?: number }
  ) => apiClient.put<VendorPortfolioItemDto>(`/vendors/${id}/portfolio/${itemId}`, body),
  deletePortfolio: (id: string, itemId: string) =>
    apiClient.delete<void>(`/vendors/${id}/portfolio/${itemId}`),
  listDocuments: (id: string) => apiClient.get<VendorDocumentDto[]>(`/vendors/${id}/documents`),
  uploadDocument: async (id: string, file: File, documentType: string) => {
    const form = new FormData();
    form.append("file", file);
    const { data } = await axiosClient.post<VendorDocumentDto>(`/vendors/${id}/documents`, form, {
      headers: { "Content-Type": "multipart/form-data" },
      params: { documentType },
    });
    return data;
  },
  deleteDocument: (id: string, documentId: string) =>
    apiClient.delete<void>(`/vendors/${id}/documents/${documentId}`),
};
