import { env } from "@/lib/env";

export function publicApiBaseUrl(): string {
  return env.NEXT_PUBLIC_API_BASE_URL.replace(/\/$/, "");
}

/** Browser-facing API origin. Server-side Docker/SSR may use API_INTERNAL_BASE_URL. */
export function serverApiBaseUrl(): string {
  const internal = process.env.API_INTERNAL_BASE_URL;
  if (internal && internal.trim().length > 0) {
    return internal.replace(/\/$/, "");
  }
  return publicApiBaseUrl();
}
