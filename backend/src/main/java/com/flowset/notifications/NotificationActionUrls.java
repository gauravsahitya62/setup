package com.flowset.notifications;

/**
 * Frontend deep-link paths. Keep in sync with {@code ROUTES} in constants/index.ts.
 */
public final class NotificationActionUrls {

    private NotificationActionUrls() {
    }

    public static String plannerBookingRequest(java.util.UUID id) {
        return "/planner/booking-requests/" + id;
    }

    public static String vendorBookingRequest(java.util.UUID id) {
        return "/vendor/requests/" + id;
    }

    public static String plannerOrder(java.util.UUID id) {
        return "/planner/orders/" + id;
    }

    public static String vendorOrder(java.util.UUID id) {
        return "/vendor/bookings/" + id;
    }

    public static String plannerProject(java.util.UUID id) {
        return "/planner/projects/" + id;
    }

    public static String vendorInventory(java.util.UUID variantId) {
        return "/vendor/inventory/" + variantId;
    }

    public static String adminVendors() {
        return "/admin/vendors";
    }

    public static String vendorProfile() {
        return "/vendor/profile";
    }

    public static String plannerReviews() {
        return "/planner/reviews";
    }

    public static String plannerOrderReview(java.util.UUID orderId) {
        return "/planner/orders/" + orderId;
    }

    public static String vendorReviews() {
        return "/vendor/reviews";
    }

    public static String adminReviews() {
        return "/admin/reviews";
    }
}
