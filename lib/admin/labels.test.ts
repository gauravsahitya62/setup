import { describe, expect, it } from "vitest";

import { formatEstimatedValue, requiresReason, USER_STATUS_LABEL } from "@/lib/admin/labels";

describe("admin labels", () => {
  it("maps pending verification to Pending", () => {
    expect(USER_STATUS_LABEL.PENDING_VERIFICATION).toBe("Pending");
  });

  it("formats estimated rental value without calling it revenue", () => {
    expect(formatEstimatedValue(125000)).toContain("1,25,000");
    expect(formatEstimatedValue(0)).not.toMatch(/revenue/i);
  });

  it("requires confirmation reasons for destructive moderation", () => {
    expect(requiresReason("reject")).toBe(true);
    expect(requiresReason("suspend")).toBe(true);
    expect(requiresReason("remove")).toBe(true);
    expect(requiresReason("archive")).toBe(true);
    expect(requiresReason("approve")).toBe(false);
  });
});
