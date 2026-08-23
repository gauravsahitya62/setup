import { env } from "@/lib/env";

export const ANALYTICS_EVENTS = [
  "planner_registered",
  "vendor_registered",
  "project_created",
  "product_viewed",
  "product_added_to_project",
  "vendor_viewed",
  "availability_checked",
  "booking_request_created",
  "booking_request_accepted",
  "booking_request_rejected",
  "order_created",
  "delivery_completed",
  "order_completed",
  "review_submitted",
] as const;

export type AnalyticsEvent = (typeof ANALYTICS_EVENTS)[number];

export type AnalyticsProperties = Record<string, string | number | boolean | undefined>;

const PII_KEYS = new Set(["email", "phone", "password", "token", "name", "firstName", "lastName"]);

function sanitize(properties?: AnalyticsProperties): Record<string, string | number | boolean> {
  if (!properties) return {};
  const next: Record<string, string | number | boolean> = {};
  for (const [key, value] of Object.entries(properties)) {
    if (value == null) continue;
    if (PII_KEYS.has(key)) continue;
    next[key] = value;
  }
  return next;
}

export function analyticsPayload(
  event: AnalyticsEvent,
  properties?: AnalyticsProperties
): { event: AnalyticsEvent; properties: Record<string, string | number | boolean>; env: string } {
  return {
    event,
    properties: sanitize(properties),
    env: env.NEXT_PUBLIC_APP_ENV,
  };
}

/**
 * Product analytics only. No PII. No third-party sink until a real provider is configured.
 * Enable with NEXT_PUBLIC_ENABLE_ANALYTICS=true.
 */
export function track(event: AnalyticsEvent, properties?: AnalyticsProperties): void {
  if (!env.NEXT_PUBLIC_ENABLE_ANALYTICS) return;
  const payload = { ...analyticsPayload(event, properties), ts: Date.now() };
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent("flowset:analytics", { detail: payload }));
}
