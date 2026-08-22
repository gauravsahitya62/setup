import { apiClient, axiosClient } from "@/services/api-client";
import type { PageResponse } from "@/services/marketplace-api";
import type {
  ProductStatus,
  ProductSummaryDto,
  ProductDto,
  CategoryDto,
} from "@/services/marketplace-api";
import type { VendorDto, VendorStatus } from "@/services/vendor-api";
import type { ReviewDto } from "@/services/review-api";
import type { BookingRequestSummaryDto } from "@/services/booking-api";
import type { OrderSummaryDto } from "@/services/order-api";

export type AdminDateRange = "TODAY" | "DAYS_7" | "DAYS_30" | "DAYS_90" | "CUSTOM";

export type VendorVerificationStatus =
  "PENDING" | "UNDER_REVIEW" | "VERIFIED" | "REJECTED" | "SUSPENDED";
export type UserStatus = "ACTIVE" | "PENDING_VERIFICATION" | "SUSPENDED";
export type SupportTicketStatus =
  "OPEN" | "IN_PROGRESS" | "WAITING_FOR_USER" | "RESOLVED" | "CLOSED";
export type SupportTicketPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface TrendPoint {
  date: string;
  count: number;
}

export interface AuditLogDto {
  id: string;
  actorUserId?: string | null;
  actorName?: string | null;
  action: string;
  resourceType?: string | null;
  resourceId?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  metadata?: string | null;
  createdAt: string;
}

export interface AdminDashboardDto {
  totalPlanners: number;
  totalVendors: number;
  verifiedVendors: number;
  pendingVendors: number;
  totalProducts: number;
  activeProducts: number;
  pendingModeration: number;
  activeProjects: number;
  bookingRequests: number;
  confirmedOrders: number;
  completedOrders: number;
  reportedReviews: number;
  openSupportTickets: number;
  recentActivity: AuditLogDto[];
  newUsers: TrendPoint[];
  newVendors: TrendPoint[];
  newProducts: TrendPoint[];
  bookingRequestTrend: TrendPoint[];
  confirmedOrdersTrend: TrendPoint[];
  completedOrdersTrend: TrendPoint[];
}

export interface AdminUserDto {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  role: "PLANNER" | "VENDOR" | "ADMIN";
  status: UserStatus;
  emailVerified: boolean;
  createdAt: string;
  lastLoginAt?: string | null;
  deletedAt?: string | null;
}

export interface AdminVendorDetailDto {
  vendor: VendorDto;
  ownerName?: string | null;
  ownerEmail?: string | null;
  ownerPhone?: string | null;
  documents: VendorDto["documents"];
  products: ProductSummaryDto[];
  reviews: ReviewDto[];
  bookings: BookingRequestSummaryDto[];
}

export interface VendorPerformanceRow {
  vendorId: string;
  companyName: string;
  bookingRequests: number;
  acceptanceRate: number;
  rejectionRate: number;
  completedOrders: number;
  cancellationRate: number;
  averageRating: number | string;
  averageResponseHours?: number | null;
  estimatedBookingValue: number | string;
}

export interface ProductPerformanceRow {
  productId: string;
  productName: string;
  views: number;
  projectAdds: number;
  bookingRequests: number;
  confirmedBookings: number;
  completedOrders: number;
  estimatedBookingValue: number | string;
  favorites: number;
}

export interface AdminReportsDto {
  vendorPerformance: VendorPerformanceRow[];
  productPerformance: ProductPerformanceRow[];
  bookingRequests: number;
  confirmedOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  estimatedRentalValue: number | string;
  publishedReviews: number;
  reportedReviews: number;
  newUsers: number;
  financialLabel: string;
}

export interface PlatformSettingsDto {
  platformName: string;
  supportEmail: string;
  defaultInventoryThreshold: number;
  reviewEditWindowDays: number;
  vendorVerificationRequired: boolean;
  maxImageSizeBytes: number;
  allowedFileTypes: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
}

export interface SupportCategoryDto {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  displayOrder: number;
}

export interface SupportTicketSummaryDto {
  id: string;
  createdBy: string;
  createdByName: string;
  subject: string;
  categoryName: string;
  priority: SupportTicketPriority;
  status: SupportTicketStatus;
  assignedAdminId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SupportMessageDto {
  id: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
}

export interface SupportTicketDto extends SupportTicketSummaryDto {
  description: string;
  categoryId: string;
  resolvedAt?: string | null;
  messages: SupportMessageDto[];
}

function qs(params: Record<string, string | number | boolean | undefined | null>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const encoded = search.toString();
  return encoded ? `?${encoded}` : "";
}

export const adminApi = {
  dashboard: (range: AdminDateRange = "DAYS_30", from?: string, to?: string) =>
    apiClient.get<AdminDashboardDto>(`/admin/dashboard${qs({ range, from, to })}`),

  users: (params: {
    q?: string;
    role?: string;
    status?: string;
    pendingVerification?: boolean;
    page?: number;
    size?: number;
  }) => apiClient.get<PageResponse<AdminUserDto>>(`/admin/users${qs(params)}`),

  user: (id: string) => apiClient.get<AdminUserDto>(`/admin/users/${id}`),

  suspendUser: (id: string, reason: string) =>
    apiClient.post<AdminUserDto>(`/admin/users/${id}/suspend`, { reason }),

  reactivateUser: (id: string) => apiClient.post<AdminUserDto>(`/admin/users/${id}/reactivate`),

  resetUserPassword: (id: string) =>
    apiClient.post<{ message: string }>(`/admin/users/${id}/reset-password`),

  userActivity: (id: string, page = 0) =>
    apiClient.get<PageResponse<AuditLogDto>>(
      `/admin/users/${id}/activity${qs({ page, size: 20 })}`
    ),

  vendors: (params: {
    q?: string;
    verificationStatus?: string;
    status?: string;
    featured?: boolean;
    page?: number;
    size?: number;
  }) => apiClient.get<PageResponse<VendorDto>>(`/admin/vendors${qs(params)}`),

  vendor: (id: string) => apiClient.get<AdminVendorDetailDto>(`/admin/vendors/${id}`),

  startVendorReview: (id: string) => apiClient.post<VendorDto>(`/admin/vendors/${id}/start-review`),

  verifyVendor: (id: string) => apiClient.post<VendorDto>(`/admin/vendors/${id}/verify`),

  rejectVendor: (id: string, reason: string) =>
    apiClient.post<VendorDto>(`/admin/vendors/${id}/reject`, { reason }),

  suspendVendor: (id: string, reason: string) =>
    apiClient.post<VendorDto>(`/admin/vendors/${id}/suspend`, { reason }),

  reactivateVendor: (id: string) => apiClient.post<VendorDto>(`/admin/vendors/${id}/reactivate`),

  featureVendor: (id: string, featured: boolean) =>
    apiClient.post<VendorDto>(`/admin/vendors/${id}/feature`, { featured }),

  approveDocument: (vendorId: string, documentId: string) =>
    apiClient.post(`/admin/vendors/${vendorId}/documents/${documentId}/approve`),

  rejectDocument: (vendorId: string, documentId: string, reason: string) =>
    apiClient.post(`/admin/vendors/${vendorId}/documents/${documentId}/reject`, { reason }),

  requestDocumentResubmission: (vendorId: string, documentId: string, reason: string) =>
    apiClient.post(`/admin/vendors/${vendorId}/documents/${documentId}/request-resubmission`, {
      reason,
    }),

  products: (params: {
    q?: string;
    status?: ProductStatus;
    vendorId?: string;
    categoryId?: string;
    featured?: boolean;
    page?: number;
    size?: number;
  }) => apiClient.get<PageResponse<ProductSummaryDto>>(`/admin/products${qs(params)}`),

  approveProduct: (id: string) => apiClient.post<ProductDto>(`/admin/products/${id}/approve`),

  rejectProduct: (id: string, reason: string) =>
    apiClient.post<ProductDto>(`/admin/products/${id}/reject`, { reason }),

  suspendProduct: (id: string, reason?: string) =>
    apiClient.post<ProductDto>(`/admin/products/${id}/suspend`, reason ? { reason } : {}),

  restoreProduct: (id: string) => apiClient.post<ProductDto>(`/admin/products/${id}/restore`),

  featureProduct: (id: string) => apiClient.post<ProductDto>(`/admin/products/${id}/feature`),

  unfeatureProduct: (id: string) => apiClient.post<ProductDto>(`/admin/products/${id}/unfeature`),

  categories: () => apiClient.get<CategoryDto[]>("/admin/categories"),

  createCategory: (payload: Record<string, unknown>) =>
    apiClient.post<CategoryDto>("/admin/categories", payload),

  updateCategory: (id: string, payload: Record<string, unknown>) =>
    apiClient.put<CategoryDto>(`/admin/categories/${id}`, payload),

  archiveCategory: (id: string) => apiClient.post<CategoryDto>(`/admin/categories/${id}/archive`),

  reorderCategories: (orderedIds: string[]) =>
    apiClient.post("/admin/categories/reorder", { orderedIds }),

  featureCategory: (id: string, featured: boolean) =>
    apiClient.post<CategoryDto>(`/admin/categories/${id}/feature`, { featured }),

  uploadCategoryImage: async (id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    const { data } = await axiosClient.post<CategoryDto>(`/admin/categories/${id}/image`, form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return data;
  },

  reports: (range: AdminDateRange = "DAYS_30", from?: string, to?: string) =>
    apiClient.get<AdminReportsDto>(`/admin/reports${qs({ range, from, to })}`),

  auditLogs: (params: {
    action?: string;
    resourceType?: string;
    actorUserId?: string;
    resourceId?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }) => apiClient.get<PageResponse<AuditLogDto>>(`/admin/audit-logs${qs(params)}`),

  settings: () => apiClient.get<PlatformSettingsDto>("/admin/settings"),

  updateSettings: (payload: PlatformSettingsDto) =>
    apiClient.put<PlatformSettingsDto>("/admin/settings", payload),

  supportTickets: (params: { status?: string; priority?: string; page?: number; size?: number }) =>
    apiClient.get<PageResponse<SupportTicketSummaryDto>>(`/admin/support/tickets${qs(params)}`),

  supportTicket: (id: string) => apiClient.get<SupportTicketDto>(`/admin/support/tickets/${id}`),

  updateSupportTicket: (
    id: string,
    payload: {
      status?: SupportTicketStatus;
      priority?: SupportTicketPriority;
      assignedAdminId?: string;
    }
  ) => apiClient.patch<SupportTicketDto>(`/admin/support/tickets/${id}`, payload),

  replySupportTicket: (id: string, body: string) =>
    apiClient.post<SupportTicketDto>(`/admin/support/tickets/${id}/messages`, { body }),

  orders: (params: { status?: string; page?: number; size?: number }) =>
    apiClient.get<PageResponse<OrderSummaryDto>>(`/admin/orders${qs(params)}`),
};

export const supportApi = {
  categories: () => apiClient.get<SupportCategoryDto[]>("/support/categories"),
  tickets: (page = 0) =>
    apiClient.get<PageResponse<SupportTicketSummaryDto>>(
      `/support/tickets${qs({ page, size: 20 })}`
    ),
  ticket: (id: string) => apiClient.get<SupportTicketDto>(`/support/tickets/${id}`),
  create: (payload: {
    subject: string;
    description: string;
    categoryId: string;
    priority?: SupportTicketPriority;
  }) => apiClient.post<SupportTicketDto>("/support/tickets", payload),
  message: (id: string, body: string) =>
    apiClient.post<SupportTicketDto>(`/support/tickets/${id}/messages`, { body }),
  close: (id: string) => apiClient.patch<SupportTicketDto>(`/support/tickets/${id}/close`),
};

export type { VendorStatus, ProductStatus };
