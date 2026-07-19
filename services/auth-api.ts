import { apiClient } from "@/services/api-client";
import type { AuthUser, UserRole } from "@/types";

export interface AuthTokensResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: BackendUser;
}

export interface BackendUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  role: "PLANNER" | "VENDOR" | "ADMIN";
  status: "ACTIVE" | "PENDING_VERIFICATION" | "SUSPENDED";
  emailVerified: boolean;
  companyName?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RegisterPlannerPayload {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  password: string;
  companyName?: string;
}

export interface RegisterVendorPayload {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  password: string;
  businessName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

const ROLE_MAP: Record<BackendUser["role"], UserRole> = {
  PLANNER: "planner",
  VENDOR: "vendor",
  ADMIN: "admin",
};

export function mapBackendUser(user: BackendUser): AuthUser {
  return {
    id: user.id,
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    role: ROLE_MAP[user.role],
    phone: user.phone ?? undefined,
    companyName: user.companyName ?? undefined,
    emailVerified: user.emailVerified,
    status: user.status,
    createdAt: user.createdAt,
    updatedAt: user.updatedAt,
  };
}

export function dashboardPathForRole(role: UserRole): string {
  switch (role) {
    case "vendor":
      return "/vendor";
    case "admin":
      return "/admin";
    case "planner":
      return "/planner";
    default:
      return "/";
  }
}

export const authApi = {
  registerPlanner: (payload: RegisterPlannerPayload) =>
    apiClient.post<AuthTokensResponse>("/auth/register/planner", payload),
  registerVendor: (payload: RegisterVendorPayload) =>
    apiClient.post<AuthTokensResponse>("/auth/register/vendor", payload),
  login: (payload: LoginPayload) => apiClient.post<AuthTokensResponse>("/auth/login", payload),
  logout: () => apiClient.post<{ message: string }>("/auth/logout"),
  refresh: () => apiClient.post<AuthTokensResponse>("/auth/refresh"),
  me: () => apiClient.get<BackendUser>("/auth/me"),
  forgotPassword: (email: string) =>
    apiClient.post<{ message: string }>("/auth/forgot-password", { email }),
  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<{ message: string }>("/auth/reset-password", { token, newPassword }),
  resendVerification: (email: string) =>
    apiClient.post<{ message: string }>("/auth/resend-verification", { email }),
  verifyEmail: (token: string) =>
    apiClient.post<{ message: string }>("/auth/verify-email", { token }),
};
