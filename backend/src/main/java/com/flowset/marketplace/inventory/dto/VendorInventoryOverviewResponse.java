package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryStockStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Vendor inventory overview: KPIs plus one row per variant")
public record VendorInventoryOverviewResponse(
        int totalProducts,
        int totalVariants,
        int totalUnits,
        int reservedUnits,
        int availableUnits,
        int maintenanceUnits,
        int damagedUnits,
        int lowAvailabilityCount,
        List<VendorInventoryItemResponse> items
) {
}
