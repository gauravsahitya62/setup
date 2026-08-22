package com.flowset.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalPlanners,
        long totalVendors,
        long verifiedVendors,
        long pendingVendors,
        long totalProducts,
        long activeProducts,
        long pendingModeration,
        long activeProjects,
        long bookingRequests,
        long confirmedOrders,
        long completedOrders,
        long reportedReviews,
        long openSupportTickets,
        List<AuditLogResponse> recentActivity,
        List<TrendPoint> newUsers,
        List<TrendPoint> newVendors,
        List<TrendPoint> newProducts,
        List<TrendPoint> bookingRequestTrend,
        List<TrendPoint> confirmedOrdersTrend,
        List<TrendPoint> completedOrdersTrend
) {
}
