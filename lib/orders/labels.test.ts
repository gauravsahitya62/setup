import { describe, expect, it } from "vitest";

import { ORDER_STATUS_LABELS, orderErrorMessage } from "@/lib/orders/labels";
import { schedulePickupSchema } from "@/lib/orders/schema";

describe("order labels", () => {
  it("labels operational statuses", () => {
    expect(ORDER_STATUS_LABELS.CONFIRMED).toBe("Confirmed");
    expect(ORDER_STATUS_LABELS.OUT_FOR_DELIVERY).toBe("Out for delivery");
    expect(ORDER_STATUS_LABELS.COMPLETED).toBe("Completed");
  });

  it("surfaces API errors without leaking hidden 404s", () => {
    expect(orderErrorMessage({ status: 404, message: "hidden" })).toBe("Order not found");
    expect(
      orderErrorMessage({
        status: 409,
        message: "This order can no longer be cancelled because delivery is underway",
      })
    ).toContain("delivery is underway");
    expect(orderErrorMessage({ status: 401, message: "x" })).toBe("Please sign in to continue");
  });
});

describe("pickup schema", () => {
  it("rejects pickup before delivery", () => {
    const result = schedulePickupSchema.safeParse({
      pickupDate: "2026-12-10",
      deliveryDate: "2026-12-12",
    });
    expect(result.success).toBe(false);
  });
});
