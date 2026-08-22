package com.flowset.notifications.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "booking_updates", nullable = false)
    private boolean bookingUpdates = true;

    @Column(name = "order_updates", nullable = false)
    private boolean orderUpdates = true;

    @Column(name = "delivery_updates", nullable = false)
    private boolean deliveryUpdates = true;

    @Column(name = "inventory_alerts", nullable = false)
    private boolean inventoryAlerts = true;

    @Column(name = "system_announcements", nullable = false)
    private boolean systemAnnouncements = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isBookingUpdates() {
        return bookingUpdates;
    }

    public void setBookingUpdates(boolean bookingUpdates) {
        this.bookingUpdates = bookingUpdates;
    }

    public boolean isOrderUpdates() {
        return orderUpdates;
    }

    public void setOrderUpdates(boolean orderUpdates) {
        this.orderUpdates = orderUpdates;
    }

    public boolean isDeliveryUpdates() {
        return deliveryUpdates;
    }

    public void setDeliveryUpdates(boolean deliveryUpdates) {
        this.deliveryUpdates = deliveryUpdates;
    }

    public boolean isInventoryAlerts() {
        return inventoryAlerts;
    }

    public void setInventoryAlerts(boolean inventoryAlerts) {
        this.inventoryAlerts = inventoryAlerts;
    }

    public boolean isSystemAnnouncements() {
        return systemAnnouncements;
    }

    public void setSystemAnnouncements(boolean systemAnnouncements) {
        this.systemAnnouncements = systemAnnouncements;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
