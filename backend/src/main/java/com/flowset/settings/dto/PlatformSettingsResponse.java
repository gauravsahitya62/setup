package com.flowset.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public record PlatformSettingsResponse(
        String platformName,
        String supportEmail,
        int defaultInventoryThreshold,
        int reviewEditWindowDays,
        boolean vendorVerificationRequired,
        int maxImageSizeBytes,
        String allowedFileTypes,
        Instant updatedAt,
        UUID updatedBy
) {
}
