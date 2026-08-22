package com.flowset.marketplace.inventory.mapper;

import com.flowset.common.InventoryStockStatus;
import com.flowset.marketplace.inventory.dto.AvailabilityResponse;
import com.flowset.marketplace.inventory.dto.InventoryBlockResponse;
import com.flowset.marketplace.inventory.dto.InventoryMovementResponse;
import com.flowset.marketplace.inventory.dto.InventoryResponse;
import com.flowset.marketplace.inventory.dto.MaintenanceRecordResponse;
import com.flowset.marketplace.inventory.dto.ReservationResponse;
import com.flowset.marketplace.inventory.dto.VendorInventoryItemResponse;
import com.flowset.marketplace.inventory.entity.AvailabilityCalendarEntity;
import com.flowset.marketplace.inventory.entity.InventoryBlockEntity;
import com.flowset.marketplace.inventory.entity.InventoryEntity;
import com.flowset.marketplace.inventory.entity.InventoryMovementEntity;
import com.flowset.marketplace.inventory.entity.InventoryReservationEntity;
import com.flowset.marketplace.inventory.entity.MaintenanceRecordEntity;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(InventoryEntity inventory) {
        return toResponse(inventory, inventory.getReserved(), inventory.getAvailable());
    }

    public InventoryResponse toResponse(InventoryEntity inventory, int reserved, int available) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getVariant().getId(),
                inventory.getTotalQuantity(),
                reserved,
                available,
                inventory.getMaintenance(),
                inventory.getDamaged(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt(),
                inventory.getVersion());
    }

    public AvailabilityResponse toResponse(AvailabilityCalendarEntity entry) {
        return new AvailabilityResponse(
                entry.getId(),
                entry.getVariant().getId(),
                entry.getDate(),
                entry.getStatus(),
                entry.getQuantityReserved(),
                entry.getNotes());
    }

    public ReservationResponse toReservation(InventoryReservationEntity reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getVariant().getId(),
                reservation.getBookingRequestId(),
                reservation.getQuantity(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

    public InventoryBlockResponse toBlock(InventoryBlockEntity block) {
        return new InventoryBlockResponse(
                block.getId(),
                block.getVariant().getId(),
                block.getQuantity(),
                block.getStartDate(),
                block.getEndDate(),
                block.getReason(),
                block.getNotes(),
                block.getStatus(),
                block.getCreatedAt(),
                block.getUpdatedAt());
    }

    public InventoryMovementResponse toMovement(InventoryMovementEntity movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getVariant().getId(),
                movement.getQuantity(),
                movement.getMovementType(),
                movement.getReason(),
                movement.getReferenceId(),
                movement.getCreatedBy(),
                movement.getCreatedAt());
    }

    public MaintenanceRecordResponse toMaintenance(MaintenanceRecordEntity record) {
        return new MaintenanceRecordResponse(
                record.getId(),
                record.getVariant().getId(),
                record.getBlock().getId(),
                record.getQuantity(),
                record.getStartDate(),
                record.getEndDate(),
                record.getReason(),
                record.getStatus(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    public VendorInventoryItemResponse toItem(
            InventoryEntity inventory,
            int reservedQuantity,
            int availableQuantity,
            int lowStockThresholdPercent
    ) {
        ProductVariantEntity variant = inventory.getVariant();
        ProductEntity product = variant.getProduct();
        InventoryStockStatus status = resolveStatus(
                inventory, availableQuantity, lowStockThresholdPercent);
        return new VendorInventoryItemResponse(
                inventory.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getFeaturedImage(),
                variant.getVariantName(),
                variant.getSku(),
                variant.getColor(),
                inventory.getTotalQuantity(),
                reservedQuantity,
                availableQuantity,
                inventory.getMaintenance(),
                inventory.getDamaged(),
                status,
                status == InventoryStockStatus.LOW_STOCK,
                inventory.getUpdatedAt(),
                inventory.getVersion());
    }

    public InventoryStockStatus resolveStatus(
            InventoryEntity inventory,
            int availableQuantity,
            int lowStockThresholdPercent
    ) {
        int total = inventory.getTotalQuantity();
        if (total <= 0 || availableQuantity <= 0) {
            boolean maintenanceHoldsTheRest =
                    inventory.getMaintenance() > 0
                            && inventory.getMaintenance() >= Math.max(0, total - inventory.getDamaged());
            return maintenanceHoldsTheRest
                    ? InventoryStockStatus.MAINTENANCE
                    : InventoryStockStatus.UNAVAILABLE;
        }
        if (availableQuantity * 100 <= total * lowStockThresholdPercent) {
            return InventoryStockStatus.LOW_STOCK;
        }
        return InventoryStockStatus.AVAILABLE;
    }
}
