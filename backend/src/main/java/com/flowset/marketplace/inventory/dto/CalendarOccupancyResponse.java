package com.flowset.marketplace.inventory.dto;

import com.flowset.common.InventoryBlockReason;
import com.flowset.common.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Orders and inventory blocks occupying a single calendar date")
public record CalendarOccupancyResponse(
        LocalDate date,
        int totalQuantity,
        int reservedQuantity,
        int maintenanceQuantity,
        int damagedQuantity,
        int blockedQuantity,
        int availableQuantity,
        List<Booking> bookings,
        List<Block> blocks
) {

    public record Booking(
            UUID orderId,
            String orderNumber,
            OrderStatus orderStatus,
            UUID bookingRequestId,
            String plannerName,
            String projectName,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate,
            BigDecimal totalAmount,
            String currency,
            int quantity,
            List<Line> items
    ) {}

    public record Line(
            UUID variantId,
            String productName,
            String variantName,
            String sku,
            int quantity
    ) {}

    public record Block(
            UUID id,
            UUID variantId,
            String productName,
            String variantName,
            int quantity,
            LocalDate startDate,
            LocalDate endDate,
            InventoryBlockReason reason,
            String notes
    ) {}
}
