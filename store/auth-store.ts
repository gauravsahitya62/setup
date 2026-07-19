import { create } from "zustand";

import type { AuthUser, UserRole } from "@/types";

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  role: UserRole;
  isHydrated: boolean;
  setSession: (user: AuthUser, accessToken: string) => void;
  setAccessToken: (accessToken: string | null) => void;
  clearAuth: () => void;
  setHydrated: (value: boolean) => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  role: "guest",
  isHydrated: false,
  setSession: (user, accessToken) =>
    set({
      user,
      accessToken,
      isAuthenticated: true,
      role: user.role,
    }),
  setAccessToken: (accessToken) => set({ accessToken }),
  clearAuth: () =>
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      role: "guest",
    }),
  setHydrated: (value) => set({ isHydrated: value }),
}));
