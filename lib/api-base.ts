import { env } from "@/lib/env";

export function publicApiBaseUrl(): string {
  const configured = env.NEXT_PUBLIC_API_BASE_URL.replace(/\/$/, "");
  if (typeof window !== "undefined" && configured.startsWith("/")) {
    return `${window.location.origin}${configured}`;
  }
  return configured;
}

/** Browser-facing API origin. Server-side Docker/SSR may use API_INTERNAL_BASE_URL. */
export function serverApiBaseUrl(): string {
  const internal = process.env.API_INTERNAL_BASE_URL?.trim();
  if (internal) {
    return internal.replace(/\/$/, "");
  }
  const host = process.env.API_HOST?.trim();
  if (host) {
    const origin = host.includes("://") ? host : `https://${host}`;
    return `${origin.replace(/\/$/, "")}/api/v1`;
  }
  const configured = env.NEXT_PUBLIC_API_BASE_URL.replace(/\/$/, "");
  if (configured.startsWith("/")) {
    const app = (process.env.NEXT_PUBLIC_APP_URL || process.env.RENDER_EXTERNAL_URL || "").replace(
      /\/$/,
      ""
    );
    if (app) {
      return `${app}${configured}`;
    }
  }
  return configured;
}
