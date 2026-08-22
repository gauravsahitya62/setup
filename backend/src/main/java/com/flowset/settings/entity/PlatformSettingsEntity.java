package com.flowset.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_settings")
public class PlatformSettingsEntity {

    public static final UUID SINGLETON_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");

    @Id
    private UUID id = SINGLETON_ID;

    @Column(name = "platform_name", nullable = false, length = 120)
    private String platformName;

    @Column(name = "support_email", nullable = false)
    private String supportEmail;

    @Column(name = "default_inventory_threshold", nullable = false)
    private int defaultInventoryThreshold;

    @Column(name = "review_edit_window_days", nullable = false)
    private int reviewEditWindowDays;

    @Column(name = "vendor_verification_required", nullable = false)
    private boolean vendorVerificationRequired;

    @Column(name = "max_image_size_bytes", nullable = false)
    private int maxImageSizeBytes;

    @Column(name = "allowed_file_types", nullable = false, length = 500)
    private String allowedFileTypes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public int getDefaultInventoryThreshold() {
        return defaultInventoryThreshold;
    }

    public void setDefaultInventoryThreshold(int defaultInventoryThreshold) {
        this.defaultInventoryThreshold = defaultInventoryThreshold;
    }

    public int getReviewEditWindowDays() {
        return reviewEditWindowDays;
    }

    public void setReviewEditWindowDays(int reviewEditWindowDays) {
        this.reviewEditWindowDays = reviewEditWindowDays;
    }

    public boolean isVendorVerificationRequired() {
        return vendorVerificationRequired;
    }

    public void setVendorVerificationRequired(boolean vendorVerificationRequired) {
        this.vendorVerificationRequired = vendorVerificationRequired;
    }

    public int getMaxImageSizeBytes() {
        return maxImageSizeBytes;
    }

    public void setMaxImageSizeBytes(int maxImageSizeBytes) {
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public String getAllowedFileTypes() {
        return allowedFileTypes;
    }

    public void setAllowedFileTypes(String allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
