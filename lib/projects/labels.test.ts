import { describe, expect, it } from "vitest";

import { EVENT_TYPE_LABELS, EVENT_TYPES, PROJECT_STATUSES } from "@/lib/projects/labels";

describe("project catalogs", () => {
  it("covers every event type and status", () => {
    expect(EVENT_TYPES).toHaveLength(12);
    expect(Object.keys(EVENT_TYPE_LABELS)).toEqual(expect.arrayContaining([...EVENT_TYPES]));
    expect(PROJECT_STATUSES).toEqual(["DRAFT", "PLANNING", "ACTIVE", "COMPLETED", "CANCELLED"]);
  });
});
