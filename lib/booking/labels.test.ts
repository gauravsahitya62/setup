import { describe, expect, it } from "vitest";

import { BOOKING_STATUS_LABELS, bookingErrorMessage } from "@/lib/booking/labels";

describe("booking labels", () => {
  it("labels API statuses", () => {
    expect(BOOKING_STATUS_LABELS.PENDING).toBe("Pending");
    expect(BOOKING_STATUS_LABELS.CONFIRMED).toBe("Confirmed");
  });

  it("surfaces duplicate and inventory API messages", () => {
    expect(
      bookingErrorMessage({
        status: 409,
        message: "A pending booking request already exists for this vendor, products, and dates.",
      })
    ).toContain("pending booking request already exists");
    expect(
      bookingErrorMessage({
        status: 409,
        message: "Only 70 units are currently available for the selected dates.",
      })
    ).toContain("Only 70 units");
    expect(bookingErrorMessage({ status: 404, message: "hidden" })).toBe(
      "Booking request not found"
    );
  });
});
