import { describe, expect, it } from "vitest";

import { createProjectSchema, noteSchema } from "@/lib/projects/schema";

describe("createProjectSchema", () => {
  it("requires a name and event type", () => {
    const result = createProjectSchema.safeParse({ name: "", eventType: "WEDDING" });
    expect(result.success).toBe(false);
  });

  it("accepts a draft with only name and type", () => {
    const result = createProjectSchema.parse({
      name: "  Sharma Wedding  ",
      eventType: "WEDDING",
    });
    expect(result.name).toBe("Sharma Wedding");
    expect(result.eventType).toBe("WEDDING");
    expect(result.eventDate).toBeUndefined();
  });

  it("coerces guest count and blanks optional fields", () => {
    const result = createProjectSchema.parse({
      name: "Ananya Reception",
      eventType: "RECEPTION",
      eventDate: "2026-12-12",
      guestCount: "180",
      city: "  Jaipur ",
      description: "",
    });
    expect(result.guestCount).toBe(180);
    expect(result.city).toBe("Jaipur");
    expect(result.description).toBeUndefined();
  });

  it("rejects negative guest counts", () => {
    const result = createProjectSchema.safeParse({
      name: "Test",
      eventType: "OTHER",
      guestCount: -2,
    });
    expect(result.success).toBe(false);
  });

  it("does not accept a client-supplied planner id", () => {
    const result = createProjectSchema.parse({
      name: "Sharma Wedding",
      eventType: "WEDDING",
      plannerId: "should-be-ignored",
    });
    expect(result).not.toHaveProperty("plannerId");
  });
});

describe("noteSchema", () => {
  it("requires a title and body", () => {
    expect(noteSchema.safeParse({ title: "", content: "x" }).success).toBe(false);
    expect(noteSchema.parse({ title: " Call sheet ", content: " Unit base " })).toEqual({
      title: "Call sheet",
      content: "Unit base",
    });
  });
});
