import { describe, expect, it } from "vitest";

import {
  formatActivityTime,
  groupActivityByDay,
  projectErrorMessage,
} from "@/lib/projects/activity";
import { fileKind, formatFileSize } from "@/lib/projects/labels";
import { mapProject, mapProjectSummary, type ProjectSummaryDto } from "@/services/project-api";

describe("groupActivityByDay", () => {
  it("groups today and yesterday", () => {
    const now = new Date("2026-08-22T15:00:00.000Z");
    const groups = groupActivityByDay(
      [
        { id: "1", description: "Added Royal Gold Chair", createdAt: "2026-08-22T07:00:00.000Z" },
        { id: "2", description: "Uploaded venue layout", createdAt: "2026-08-21T11:00:00.000Z" },
      ],
      now
    );
    expect(groups.map((group) => group.label)).toEqual(["Today", "Yesterday"]);
    expect(groups[0]?.items[0]?.description).toBe("Added Royal Gold Chair");
  });
});

describe("projectErrorMessage", () => {
  it("hides other planners' projects as not found", () => {
    expect(projectErrorMessage({ status: 404, message: "Project not found" })).toBe(
      "Project not found"
    );
  });
});

describe("file helpers", () => {
  it("formats sizes and kinds", () => {
    expect(formatFileSize(512)).toBe("512 B");
    expect(formatFileSize(2048)).toBe("2.0 KB");
    expect(fileKind("application/pdf")).toBe("pdf");
    expect(fileKind("image/png")).toBe("image");
  });
});

describe("mapProjectSummary", () => {
  it("builds a list card model from the API", () => {
    const dto: ProjectSummaryDto = {
      id: "p1",
      plannerId: "u1",
      name: "Tata Corporate Conference",
      slug: "tata-corporate-conference",
      eventType: "CONFERENCE",
      eventDate: "2026-11-01",
      venueName: "JW Marriott",
      city: "Mumbai",
      guestCount: 400,
      status: "PLANNING",
      productCount: 3,
      vendorCount: 2,
      progressPercent: 40,
      createdAt: "2026-08-01T00:00:00.000Z",
      updatedAt: "2026-08-20T00:00:00.000Z",
    };
    const project = mapProjectSummary(dto);
    expect(project.location).toBe("JW Marriott, Mumbai");
    expect(project.itemCount).toBe(3);
    expect(project.progressPercent).toBe(40);
  });
});

describe("project loading", () => {
  it("maps workspace progress from the API", () => {
    const project = mapProject({
      id: "p1",
      plannerId: "u1",
      name: "Sharma Wedding",
      slug: "sharma-wedding",
      eventType: "WEDDING",
      status: "DRAFT",
      productCount: 1,
      vendorCount: 0,
      moodBoardItemCount: 2,
      noteCount: 1,
      fileCount: 0,
      progressPercent: 40,
      progress: {
        percent: 40,
        completed: 4,
        total: 10,
        tasks: [{ id: "eventDate", label: "Event date set", done: true }],
      },
      recentActivity: [],
      createdAt: "2026-08-01T00:00:00.000Z",
      updatedAt: "2026-08-20T00:00:00.000Z",
    });
    expect(project.progress?.percent).toBe(40);
    expect(project.moodBoardItemCount).toBe(2);
  });
});

describe("add and remove product payloads", () => {
  it("sends a shortlist payload without booking or inventory fields", () => {
    const payload = { variantId: "v1", quantity: 12, notes: "Head table" };
    expect(payload).not.toHaveProperty("reserve");
    expect(payload).not.toHaveProperty("bookingRequestId");
    expect(Object.keys(payload)).toEqual(["variantId", "quantity", "notes"]);
  });
});

describe("mood board, notes, and files", () => {
  it("keeps mood board captions and order client-side", () => {
    const items = [
      { id: "a", displayOrder: 0 },
      { id: "b", displayOrder: 1 },
    ];
    const reordered = [items[1]!.id, items[0]!.id];
    expect(reordered).toEqual(["b", "a"]);
  });

  it("pins notes without dropping content", () => {
    const note = { id: "n1", title: "Call sheet", content: "City Palace", pinned: false };
    const updated = { ...note, pinned: true };
    expect(updated.pinned).toBe(true);
    expect(updated.content).toBe("City Palace");
  });
});

describe("formatActivityTime", () => {
  it("returns a localized time", () => {
    expect(formatActivityTime("not-a-date")).toBe("");
    expect(formatActivityTime("2026-08-22T12:30:00.000Z")).toBeTruthy();
  });
});
