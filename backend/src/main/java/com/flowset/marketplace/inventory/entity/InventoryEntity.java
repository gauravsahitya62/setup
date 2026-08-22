package com.flowset.marketplace.inventory.entity;

import com.flowset.marketplace.product.entity.ProductVariantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    private ProductVariantEntity variant;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    /**
     * Denormalized snapshot. Date-based reserved quantity is computed from
     * {@code inventory_reservations}; this column is not the source of truth.
     */
    @Column(nullable = false)
    private int reserved;

    /**
     * Denormalized standing availability: {@code total - maintenance - damaged}.
     * Date-window availability is calculated by {@code InventoryAvailabilityService}.
     */
    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int maintenance;

    @Column(nullable = false)
    private int damaged;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Standing pool that is never rentable, regardless of date: maintenance + damaged.
     */
    public int standingUnavailable() {
        return maintenance + damaged;
    }

    /**
     * Units that can still be reserved on a given date before applying reservations/blocks.
     */
    public int standingPool() {
        return Math.max(0, totalQuantity - standingUnavailable());
    }

    public UUID getId() {
        return id;
    }

    public ProductVariantEntity getVariant() {
        return variant;
    }

    public void setVariant(ProductVariantEntity variant) {
        this.variant = variant;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(int maintenance) {
        this.maintenance = maintenance;
    }

    public int getDamaged() {
        return damaged;
    }

    public void setDamaged(int damaged) {
        this.damaged = damaged;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }
}
