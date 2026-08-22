import { describe, expect, it } from "vitest";

import { bookingRequestSchema, rejectBookingSchema } from "@/lib/booking/schema";

const base = {
  projectId: "11111111-1111-4111-8111-111111111111",
  vendorId: "22222222-2222-4222-8222-222222222222",
  variantIds: ["33333333-3333-4333-8333-333333333333"],
  quantities: { "33333333-3333-4333-8333-333333333333": 100 },
  eventDate: "2026-12-12",
  rentalStartDate: "2026-12-12",
  rentalEndDate: "2026-12-14",
  venueName: "City Palace",
  venueAddress: "Old City",
  city: "Jaipur",
  state: "Rajasthan",
  deliveryDate: "2026-12-12",
  pickupDate: "2026-12-14",
  contactPerson: "Maya Chen",
  contactPhone: "9999999999",
  termsAccepted: true,
};

describe("bookingRequestSchema", () => {
  it("accepts a complete request", () => {
    const parsed = bookingRequestSchema.parse(base);
    expect(parsed.vendorId).toBe(base.vendorId);
    expect(parsed.variantIds).toHaveLength(1);
  });

  it("rejects inverted rental dates", () => {
    const result = bookingRequestSchema.safeParse({
      ...base,
      rentalEndDate: "2026-12-10",
    });
    expect(result.success).toBe(false);
  });

  it("rejects zero quantity", () => {
    const result = bookingRequestSchema.safeParse({
      ...base,
      quantities: { "33333333-3333-4333-8333-333333333333": 0 },
    });
    expect(result.success).toBe(false);
  });

  it("requires terms acknowledgment", () => {
    const result = bookingRequestSchema.safeParse({ ...base, termsAccepted: false });
    expect(result.success).toBe(false);
  });
});

describe("rejectBookingSchema", () => {
  it("requires a reason", () => {
    expect(rejectBookingSchema.safeParse({}).success).toBe(false);
    expect(
      rejectBookingSchema.parse({ reason: "DATES_UNAVAILABLE", note: "Weekend booked" }).reason
    ).toBe("DATES_UNAVAILABLE");
  });
});
