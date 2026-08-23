import { describe, expect, it } from "vitest";

import {
  BOOKING_STATUS_LABELS,
  bookingErrorMessage,
  formatAvailabilityHint,
} from "@/lib/booking/labels";

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
        message:
          "Royal Gold Chair (Gold): only 70 of 100 requested units are available for the selected dates.",
      })
    ).toContain("Royal Gold Chair");
    expect(bookingErrorMessage({ status: 404, message: "hidden" })).toBe(
      "Booking request not found"
    );
  });

  it("names the product when stock is short", () => {
    expect(
      formatAvailabilityHint({
        productName: "Clear Marquee Tent",
        variantName: "12x12",
        available: 4,
        requested: 5,
        datesKnown: true,
      })
    ).toBe(
      "Clear Marquee Tent (12x12): only 4 of 5 requested units are available for the selected dates."
    );
  });
});
