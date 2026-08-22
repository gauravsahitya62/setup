package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryStockStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorInventoryDetailResponse(
        UUID id,
        UUID variantId,
        UUID productId,
        String productName,
        String productSlug,
        String featuredImage,
        List<String> imageUrls,
        String variantName,
        String sku,
        String color,
        String material,
        int totalQuantity,
        int reservedQuantity,
        int availableQuantity,
        int maintenanceQuantity,
        int damagedQuantity,
        InventoryStockStatus status,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt,
        int version,
        List<ReservationResponse> upcomingReservations,
        List<MaintenanceRecordResponse> maintenanceRecords,
        List<InventoryMovementResponse> recentMovements
) {
}
