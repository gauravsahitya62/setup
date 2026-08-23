"use client";

import * as React from "react";
import { useRouter } from "next/navigation";

import { startPageTransition } from "@/components/shared/navigation-progress";
import { authApi, bindAuthTokenHandlers, dashboardPathForRole, mapBackendUser } from "@/services";
import { track } from "@/lib/analytics";
import { clearAuthHintCookies, setAuthHintCookies } from "@/lib/auth/session-cookies";
import { useAuthStore } from "@/store";
import type { AuthUser, UserRole } from "@/types";
import type { LoginPayload, RegisterPlannerPayload, RegisterVendorPayload } from "@/services";

interface AuthContextValue {
  user: AuthUser | null;
  role: UserRole;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<AuthUser>;
  registerPlanner: (payload: RegisterPlannerPayload) => Promise<AuthUser>;
  registerVendor: (payload: RegisterVendorPayload) => Promise<AuthUser>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<AuthUser | null>;
}

const AuthContext = React.createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const {
    user,
    role,
    isAuthenticated,
    accessToken,
    isHydrated,
    setSession,
    setAccessToken,
    clearAuth,
    setHydrated,
  } = useAuthStore();
  const [isLoading, setIsLoading] = React.useState(true);

  const applySession = React.useCallback(
    (nextUser: AuthUser, token: string) => {
      setSession(nextUser, token);
      setAuthHintCookies(nextUser.role);
    },
    [setSession]
  );

  const refreshSession = React.useCallback(async () => {
    try {
      const tokens = await authApi.refresh();
      const mapped = mapBackendUser(tokens.user);
      applySession(mapped, tokens.accessToken);
      return mapped;
    } catch {
      clearAuth();
      clearAuthHintCookies();
      return null;
    }
  }, [applySession, clearAuth]);

  React.useEffect(() => {
    bindAuthTokenHandlers({
      getAccessToken: () => useAuthStore.getState().accessToken,
      setAccessToken: (token) => {
        setAccessToken(token);
        if (!token) {
          clearAuth();
          clearAuthHintCookies();
        }
      },
      refreshAccessToken: async () => {
        const mapped = await refreshSession();
        return mapped ? useAuthStore.getState().accessToken : null;
      },
    });
  }, [clearAuth, refreshSession, setAccessToken]);

  React.useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      setIsLoading(true);
      try {
        if (accessToken) {
          const me = await authApi.me();
          if (!cancelled) {
            applySession(mapBackendUser(me), accessToken);
          }
        } else {
          await refreshSession();
        }
      } catch {
        if (!cancelled) {
          clearAuth();
          clearAuthHintCookies();
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
          setHydrated(true);
        }
      }
    }

    void bootstrap();
    return () => {
      cancelled = true;
    };
    // Bootstrap once on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = React.useCallback(
    async (payload: LoginPayload) => {
      const tokens = await authApi.login(payload);
      const mapped = mapBackendUser(tokens.user);
      applySession(mapped, tokens.accessToken);
      startPageTransition();
      router.replace(dashboardPathForRole(mapped.role));
      return mapped;
    },
    [applySession, router]
  );

  const registerPlanner = React.useCallback(
    async (payload: RegisterPlannerPayload) => {
      const tokens = await authApi.registerPlanner(payload);
      const mapped = mapBackendUser(tokens.user);
      applySession(mapped, tokens.accessToken);
      track("planner_registered", { role: "planner" });
      startPageTransition();
      router.replace(dashboardPathForRole(mapped.role));
      return mapped;
    },
    [applySession, router]
  );

  const registerVendor = React.useCallback(
    async (payload: RegisterVendorPayload) => {
      const tokens = await authApi.registerVendor(payload);
      const mapped = mapBackendUser(tokens.user);
      applySession(mapped, tokens.accessToken);
      track("vendor_registered", { role: "vendor" });
      startPageTransition();
      router.replace(dashboardPathForRole(mapped.role));
      return mapped;
    },
    [applySession, router]
  );

  const logout = React.useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // still clear local session
    } finally {
      clearAuth();
      clearAuthHintCookies();
      startPageTransition();
      router.replace("/sign-in");
    }
  }, [clearAuth, router]);

  const value: AuthContextValue = {
    user,
    role,
    isAuthenticated: isAuthenticated && Boolean(user),
    isLoading: isLoading || !isHydrated,
    login,
    registerPlanner,
    registerVendor,
    logout,
    refreshSession,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = React.useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
