import { describe, expect, it, vi } from "vitest";

vi.mock("@/lib/env", () => ({
  env: {
    NEXT_PUBLIC_ENABLE_ANALYTICS: true,
    NEXT_PUBLIC_APP_ENV: "development",
  },
}));

describe("analytics payload", () => {
  it("drops email and other PII keys from the payload", async () => {
    const { analyticsPayload } = await import("@/lib/analytics");
    const detail = analyticsPayload("planner_registered", {
      email: "hidden@example.com",
      role: "planner",
    });
    expect(detail.event).toBe("planner_registered");
    expect(detail.properties.role).toBe("planner");
    expect(detail.properties.email).toBeUndefined();
  });
});
