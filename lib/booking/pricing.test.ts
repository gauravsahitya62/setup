import { describe, expect, it } from "vitest";

import { formatEstimatedRental, lineEstimatedValue, rentalDays } from "@/lib/booking/pricing";

describe("booking pricing", () => {
  it("counts inclusive rental days", () => {
    expect(rentalDays("2026-12-12", "2026-12-14")).toBe(3);
    expect(rentalDays("2026-12-12", "2026-12-12")).toBe(1);
  });

  it("computes estimated rental value as price × qty × days", () => {
    expect(lineEstimatedValue(18, 100, 3)).toBe(5400);
    expect(formatEstimatedRental(5400, "INR")).toContain("5,400");
  });
});
