import { describe, expect, it } from "vitest";

import { ROUTES } from "@/constants";
import {
  formatUnreadBadge,
  notificationActionHref,
  notificationErrorMessage,
  notificationsInboxHref,
} from "@/lib/notifications/labels";

describe("notification badge", () => {
  it("hides at zero, caps at 99+", () => {
    expect(formatUnreadBadge(0)).toBeNull();
    expect(formatUnreadBadge(-1)).toBeNull();
    expect(formatUnreadBadge(1)).toBe("1");
    expect(formatUnreadBadge(99)).toBe("99");
    expect(formatUnreadBadge(100)).toBe("99+");
  });
});

describe("notification navigation", () => {
  it("uses role inbox routes from ROUTES", () => {
    expect(notificationsInboxHref("planner")).toBe(ROUTES.planner.notifications);
    expect(notificationsInboxHref("vendor")).toBe(ROUTES.vendor.notifications);
    expect(notificationsInboxHref("admin")).toBe(ROUTES.admin.notifications);
  });

  it("keeps backend action urls as deep links", () => {
    expect(notificationActionHref({ actionUrl: ROUTES.planner.bookingRequest("abc") })).toBe(
      "/planner/booking-requests/abc"
    );
    expect(notificationActionHref({ actionUrl: ROUTES.vendor.request("abc") })).toBe(
      "/vendor/requests/abc"
    );
    expect(notificationActionHref({ actionUrl: ROUTES.planner.order("abc") })).toBe(
      "/planner/orders/abc"
    );
    expect(notificationActionHref({ actionUrl: null })).toBeNull();
  });
});

describe("notification errors", () => {
  it("maps API failures for empty and error states", () => {
    expect(notificationErrorMessage({ status: 401, message: "x" })).toBe(
      "Please sign in to continue"
    );
    expect(notificationErrorMessage({ status: 404, message: "hidden" })).toBe(
      "Notification not found"
    );
    expect(notificationErrorMessage({ status: 500, message: "Database failure" })).toBe(
      "Database failure"
    );
  });
});
