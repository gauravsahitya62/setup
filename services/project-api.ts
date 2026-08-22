import { apiClient, axiosClient } from "@/services/api-client";
import { formatProjectLocation } from "@/lib/projects/labels";
import type { EventType, Project, ProjectProgress, ProjectStatus } from "@/types";
import type { PageResponse } from "@/services/marketplace-api";

export type EventTypeDto = EventType;
export type ProjectStatusDto = ProjectStatus;

export interface ProjectProgressDto {
  percent: number;
  completed: number;
  total: number;
  tasks: { id: string; label: string; done: boolean }[];
}

export interface ProjectActivityDto {
  id: string;
  projectId: string;
  userId?: string | null;
  activityType: string;
  description: string;
  metadata?: string | null;
  createdAt: string;
}

export interface ProjectSummaryDto {
  id: string;
  plannerId: string;
  name: string;
  slug: string;
  eventType: EventType;
  description?: string | null;
  eventDate?: string | null;
  venueName?: string | null;
  city?: string | null;
  guestCount?: number | null;
  status: ProjectStatus;
  coverImageUrl?: string | null;
  productCount: number;
  vendorCount: number;
  progressPercent: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectDto extends ProjectSummaryDto {
  startTime?: string | null;
  endTime?: string | null;
  venueAddress?: string | null;
  state?: string | null;
  country?: string | null;
  moodBoardItemCount: number;
  noteCount: number;
  fileCount: number;
  progress: ProjectProgressDto;
  recentActivity: ProjectActivityDto[];
}

export interface ProjectProductDto {
  id: string;
  projectId: string;
  productVariantId?: string | null;
  productId?: string | null;
  productSlug?: string | null;
  productName: string;
  variantName: string;
  sku?: string | null;
  vendorId: string;
  vendorName: string;
  vendorSlug: string;
  vendorVerified: boolean;
  quantity: number;
  notes?: string | null;
  snapshotPricePerDay: number | string;
  currentPricePerDay?: number | string | null;
  currency: string;
  imageUrl?: string | null;
  listed: boolean;
  deletedFromCatalog: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectVendorDto {
  id: string;
  projectId: string;
  vendorId: string;
  companyName: string;
  slug: string;
  logoUrl?: string | null;
  rating: number | string;
  reviewCount: number;
  verified: boolean;
  city?: string | null;
  notes?: string | null;
  productsInProject: number;
  unavailable: boolean;
  createdAt: string;
}

export interface MoodBoardItemDto {
  id: string;
  moodBoardId: string;
  imageUrl: string;
  sourceUrl?: string | null;
  caption?: string | null;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface MoodBoardDto {
  id: string;
  projectId: string;
  items: MoodBoardItemDto[];
  createdAt: string;
  updatedAt: string;
}

export interface ProjectNoteDto {
  id: string;
  projectId: string;
  title: string;
  content: string;
  pinned: boolean;
  createdBy: string;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectFileDto {
  id: string;
  projectId: string;
  fileUrl: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  uploadedBy: string;
  uploadedByName: string;
  createdAt: string;
}

export interface CreateProjectPayload {
  name: string;
  eventType: EventType;
  description?: string;
  eventDate?: string;
  startTime?: string;
  endTime?: string;
  venueName?: string;
  venueAddress?: string;
  city?: string;
  state?: string;
  country?: string;
  guestCount?: number;
  status?: ProjectStatus;
}

export type UpdateProjectPayload = Partial<CreateProjectPayload>;

export interface ProjectListParams {
  q?: string;
  status?: ProjectStatus | "";
  eventType?: EventType | "";
  sort?: "eventDate" | "updatedAt" | "createdAt" | "name";
  direction?: "asc" | "desc";
  page?: number;
  size?: number;
}

function toQuery(params: Record<string, string | number | boolean | undefined>) {
  return params;
}

export function mapProjectSummary(dto: ProjectSummaryDto): Project {
  return {
    id: dto.id,
    plannerId: dto.plannerId,
    name: dto.name,
    slug: dto.slug,
    eventType: dto.eventType,
    description: dto.description ?? undefined,
    eventDate: dto.eventDate ?? undefined,
    venueName: dto.venueName ?? undefined,
    city: dto.city ?? undefined,
    guestCount: dto.guestCount ?? undefined,
    status: dto.status,
    coverImageUrl: dto.coverImageUrl ?? undefined,
    productCount: dto.productCount,
    vendorCount: dto.vendorCount,
    progressPercent: dto.progressPercent,
    itemCount: dto.productCount,
    location: formatProjectLocation({ venueName: dto.venueName, city: dto.city }),
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

export function mapProject(dto: ProjectDto): Project {
  return {
    ...mapProjectSummary(dto),
    startTime: dto.startTime ?? undefined,
    endTime: dto.endTime ?? undefined,
    venueAddress: dto.venueAddress ?? undefined,
    state: dto.state ?? undefined,
    country: dto.country ?? undefined,
    moodBoardItemCount: dto.moodBoardItemCount,
    noteCount: dto.noteCount,
    fileCount: dto.fileCount,
    progress: dto.progress as ProjectProgress,
  };
}

function upload(path: string, file: File, fields?: Record<string, string>) {
  const form = new FormData();
  form.append("file", file);
  if (fields) {
    Object.entries(fields).forEach(([key, value]) => {
      if (value) form.append(key, value);
    });
  }
  return axiosClient.post(path, form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export const projectApi = {
  list: (params: ProjectListParams = {}) =>
    apiClient.get<PageResponse<ProjectSummaryDto>>("/projects", {
      params: toQuery({
        q: params.q || undefined,
        status: params.status || undefined,
        eventType: params.eventType || undefined,
        sort: params.sort,
        direction: params.direction,
        page: params.page ?? 0,
        size: params.size ?? 12,
      }),
    }),
  get: (id: string) => apiClient.get<ProjectDto>(`/projects/${id}`),
  getBySlug: (slug: string) => apiClient.get<ProjectDto>(`/projects/slug/${slug}`),
  create: (payload: CreateProjectPayload) => apiClient.post<ProjectDto>("/projects", payload),
  update: (id: string, payload: UpdateProjectPayload) =>
    apiClient.put<ProjectDto>(`/projects/${id}`, payload),
  remove: (id: string) => apiClient.delete<void>(`/projects/${id}`),
  uploadCover: async (id: string, file: File) => {
    const response = await upload(`/projects/${id}/cover`, file);
    return response.data as ProjectDto;
  },

  products: {
    list: (projectId: string) =>
      apiClient.get<ProjectProductDto[]>(`/projects/${projectId}/products`),
    add: (projectId: string, payload: { variantId: string; quantity?: number; notes?: string }) =>
      apiClient.post<ProjectProductDto>(`/projects/${projectId}/products`, payload),
    update: (projectId: string, id: string, payload: { quantity?: number; notes?: string }) =>
      apiClient.patch<ProjectProductDto>(`/projects/${projectId}/products/${id}`, payload),
    remove: (projectId: string, id: string) =>
      apiClient.delete<void>(`/projects/${projectId}/products/${id}`),
  },

  vendors: {
    list: (projectId: string) =>
      apiClient.get<ProjectVendorDto[]>(`/projects/${projectId}/vendors`),
    save: (projectId: string, payload: { vendorId: string; notes?: string }) =>
      apiClient.post<ProjectVendorDto>(`/projects/${projectId}/vendors`, payload),
    remove: (projectId: string, vendorId: string) =>
      apiClient.delete<void>(`/projects/${projectId}/vendors/${vendorId}`),
  },

  moodBoard: {
    get: (projectId: string) => apiClient.get<MoodBoardDto>(`/projects/${projectId}/mood-board`),
    addUrl: (
      projectId: string,
      payload: { imageUrl: string; sourceUrl?: string; caption?: string }
    ) => apiClient.post<MoodBoardItemDto>(`/projects/${projectId}/mood-board/items`, payload),
    upload: async (projectId: string, file: File, caption?: string, sourceUrl?: string) => {
      const response = await upload(`/projects/${projectId}/mood-board/items`, file, {
        caption: caption ?? "",
        sourceUrl: sourceUrl ?? "",
      });
      return response.data as MoodBoardItemDto;
    },
    update: (
      projectId: string,
      id: string,
      payload: { caption?: string; sourceUrl?: string; displayOrder?: number }
    ) =>
      apiClient.patch<MoodBoardItemDto>(`/projects/${projectId}/mood-board/items/${id}`, payload),
    reorder: (projectId: string, itemIds: string[]) =>
      apiClient.put<void>(`/projects/${projectId}/mood-board/reorder`, { itemIds }),
    remove: (projectId: string, id: string) =>
      apiClient.delete<void>(`/projects/${projectId}/mood-board/items/${id}`),
  },

  notes: {
    list: (projectId: string) => apiClient.get<ProjectNoteDto[]>(`/projects/${projectId}/notes`),
    create: (projectId: string, payload: { title: string; content: string; pinned?: boolean }) =>
      apiClient.post<ProjectNoteDto>(`/projects/${projectId}/notes`, payload),
    update: (
      projectId: string,
      id: string,
      payload: { title?: string; content?: string; pinned?: boolean }
    ) => apiClient.patch<ProjectNoteDto>(`/projects/${projectId}/notes/${id}`, payload),
    remove: (projectId: string, id: string) =>
      apiClient.delete<void>(`/projects/${projectId}/notes/${id}`),
  },

  files: {
    list: (projectId: string) => apiClient.get<ProjectFileDto[]>(`/projects/${projectId}/files`),
    upload: async (projectId: string, file: File) => {
      const response = await upload(`/projects/${projectId}/files`, file);
      return response.data as ProjectFileDto;
    },
    remove: (projectId: string, id: string) =>
      apiClient.delete<void>(`/projects/${projectId}/files/${id}`),
  },

  activity: {
    list: (projectId: string, page = 0, size = 30) =>
      apiClient.get<PageResponse<ProjectActivityDto>>(`/projects/${projectId}/activity`, {
        params: { page, size },
      }),
  },
};
