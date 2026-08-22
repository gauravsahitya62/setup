package com.flowset.marketplace.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowset.config.InventoryProperties;
import com.flowset.exceptions.BadRequestException;
import com.flowset.marketplace.inventory.entity.InventoryEntity;
import com.flowset.marketplace.inventory.mapper.InventoryMapper;
import com.flowset.marketplace.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;

/**
 * Standing-stock rule: available = total − maintenance − damaged.
 * Date-window reservations are calculated by {@code InventoryAvailabilityService}.
 */
class InventoryServiceTest {

    private final InventoryService inventoryService = new InventoryService(
            null,
            null,
            null,
            null,
            null,
            new InventoryMapper(),
            null,
            null,
            new InventoryProperties(20, 366),
            null);

    @Test
    void recalculateComputesAvailableFromStandingTotals() {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setTotalQuantity(50);
        inventory.setMaintenance(2);
        inventory.setDamaged(1);

        inventoryService.recalculate(inventory);

        assertThat(inventory.getAvailable()).isEqualTo(47);
    }

    @Test
    void recalculateAllowsFullyAvailableStock() {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setTotalQuantity(20);

        inventoryService.recalculate(inventory);

        assertThat(inventory.getAvailable()).isEqualTo(20);
    }

    @Test
    void recalculateRejectsInconsistentOverAllocation() {
        InventoryEntity inventory = new InventoryEntity();
        inventory.setTotalQuantity(10);
        inventory.setMaintenance(8);
        inventory.setDamaged(4);

        assertThatThrownBy(() -> inventoryService.recalculate(inventory))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inconsistent");
    }
}
